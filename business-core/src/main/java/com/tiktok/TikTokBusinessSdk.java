/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_CRASH;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.appevents.*;
import com.tiktok.appevents.base.EventName;
import com.tiktok.appevents.base.TTBaseEvent;
import com.tiktok.iap.TTInAppPurchaseWrapper;
import com.tiktok.util.RegexUtil;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;

import com.tiktok.util.TTUtil;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TikTokBusinessSdk {
    static final String TAG = TikTokBusinessSdk.class.getName();

    /**
     * Singleton instance for {@link TikTokBusinessSdk}
     */
    static volatile TikTokBusinessSdk ttSdk = null;

    public static volatile boolean isActivatedLogicRun = false;

    /**
     * {@link TTAppEventLogger} package singleton
     */
    static TTAppEventLogger appEventLogger;

    private static final AtomicBoolean globalConfigFetched = new AtomicBoolean(false);

    /**
     * We provide a global switch in order that you can turn off our sdk remotely
     * This is a final rescue in case our sdk is causing constant crashes in you app.
     * If the switch is off, the events will neither be saved to the memory nor on the disk,
     * our sdk simply ignores all the track or flush requests.
     */
    private static Boolean sdkGlobalSwitch = true;
    /**
     * api available version
     */
    private static String apiAvailableVersion = "v1.2";
    /**
     * api treckEvent sent Domain
     */
    private static String apiTrackDomain = "business-api.tiktok.com";
    /**
     * {@link LogLevel} of initialized sdk
     */
    private static LogLevel logLevel = LogLevel.INFO;
    /**
     * if set to false, only save to memory and disk, no api request will be sent
     */
    private static AtomicBoolean networkSwitch;

    /**
     *  debug mode switch
     */
    private static AtomicBoolean sdkDebugModeSwitch = new AtomicBoolean(false);

    /**
     *  debug mode switch
     */
    private static AtomicBoolean sdkLDUModeSwitch = new AtomicBoolean(false);

    /**

     * save the test Event code
     */
    private static String testEventCode = "";

    private static TTConfig config;
    /**
     * logger util
     */
    private static TTLogger logger;

    /**
     * unique session ID, not persisted
     */
    private static final String sessionID = UUID.randomUUID().toString();

    private static CrashListener onCrashListener;

    private TikTokBusinessSdk(@NonNull TTConfig ttConfig) {
        /* sdk logger & loglevel */
        logLevel = ttConfig.logLevel;
        logger = new TTLogger(TAG, logLevel);

        /* no app id exception */
        if (TextUtils.isEmpty(ttConfig.appId) || !RegexUtil.validateAppId(ttConfig.appId)) {
            ttConfig.appId = "";
            logger.warn("Invalid App Id!");
        }

        if (ttConfig.ttAppId == null || !RegexUtil.validateTTAppId(ttConfig.ttAppId)) {
            ttConfig.ttAppId = "";
            ttConfig.ttAppIds = new String[]{""};
            ttConfig.ttFirstAppId = new BigInteger("0");
            logger.warn("Invalid TikTok App Id!");
        }
        logger.info("appId: %s, TTAppId: %s, autoIapTrack: %s", ttConfig.appId, ttConfig.ttAppId, ttConfig.autoIapTrack);
        config = ttConfig;
        networkSwitch = new AtomicBoolean(ttConfig.autoStart);
        sdkDebugModeSwitch.set(ttConfig.debugModeSwitch);
        if (sdkDebugModeSwitch.get()) {
            testEventCode = createTestEventCode(ttConfig);
        }
        sdkLDUModeSwitch.set(ttConfig.lduModeSwitch);
    }

    private String createTestEventCode(@NonNull TTConfig ttConfig) {
        return ttConfig.ttAppId.toString();
    }

    public static synchronized boolean isInitialized() {
        return ttSdk != null;
    }

    /**
     * Only one TikTokBusinessSdk instance exist within a single App process
     */
    public static synchronized void initializeSdk(TTConfig ttConfig) {
        if (ttSdk != null) return;
        long initTimeMS = System.currentTimeMillis();
        try {
            Thread.UncaughtExceptionHandler existingExHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                    if (TTCrashHandler.isTTSDKRelatedException(throwable)) {
                        TTCrashHandler.handleCrash(TAG, throwable, TTSDK_EXCEPTION_CRASH);
                    }
                    if (getCrashListener() != null) {
                        getCrashListener().onCrash(thread, throwable);
                    }
                    if (existingExHandler != null) {
                        existingExHandler.uncaughtException(thread, throwable);
                    }
                }
            });
        } catch (Exception ignored) {
            // SecurityException
        }

        ttSdk = new TikTokBusinessSdk(ttConfig);
        TTUserInfo.reset(TikTokBusinessSdk.getApplicationContext(), false);
        // the appEventLogger instance will be the main interface to track events
        appEventLogger = new TTAppEventLogger(ttConfig.autoEvent,ttConfig.disabledEvents,
                ttConfig.flushTime, ttConfig.disableMetrics, initTimeMS);
        if (ttConfig.autoIapTrack) {
            TTInAppPurchaseWrapper.registerIapTrack();
        }

        try {
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(null)
                    .put("latency", endTimeMS-initTimeMS);
            appEventLogger.monitorMetric("init_end", meta, null);
        } catch (Exception ignored) {}
    }

    /**
     * Normally, the sdk will enable network after it is initialized,
     * all the events stored in the memory or on the disk will be flushed to network once some conditions are reached,
     * for example, every {@link TTAppEventLogger#TIME_BUFFER} seconds or there are more than {@link TTAppEventLogger#THRESHOLD} events
     * in the memory.
     * But if the app developer calls {@link TTConfig#disableAutoStart()} ()}, then the "flush to network" operation will be simply suppressed
     * by the sdk, then the developer has to call startTrack to bring network back.
     * <p>
     * When to use?
     * This method can be invoked after the user agrees to some terms or conditions, so that the events are not pushed to the network
     * before users' consent.
     */
    public static void startTrack() {
        if (!networkSwitch.get()) {
            networkSwitch.set(true);
            appEventLogger.forceFlush();
        }
    }

    public static void destroy() {
        ttSdk = null;
        memoryListener = null;
        diskListener = null;
        networkListener = null;
        nextTimeFlushListener = null;
        if (appEventLogger != null) {
            appEventLogger.destroy();
        }
    }

    /**
     * For internal development usage, a monitoring tool which oversees how many events are in the memory,
     * on the disk and have been flushed to network.
     *
     * @param ml
     * @param dl
     * @param nl
     * @param nfl
     */
    public static synchronized void setUpSdkListeners(
            MemoryListener ml,
            DiskStatusListener dl,
            NetworkListener nl,
            NextTimeFlushListener nfl
    ) {
        if (ml != null) {
            memoryListener = ml;
        }
        if (dl != null) {
            diskListener = dl;
        }
        if (nl != null) {
            networkListener = nl;
        }
        if (nfl != null) {
            nextTimeFlushListener = nfl;
        }
        flush();
    }

    // inner status listeners, for debugging purpose
    public interface DiskStatusListener {
        void onDiskChange(int diskSize, boolean read);

        void onDumped(int dumped);
    }

    public interface NextTimeFlushListener {
        // how many seconds until next auto flush
        void timeLeft(int timeLeft);

        // how many until threshold
        // i.e. threshold is 100, current in memory is 80, then left will be 100 - 80 = 20
        void thresholdLeft(int threshold, int left);
    }

    public interface MemoryListener {
        void onMemoryChange(int size);
    }

    public interface NetworkListener {
        void onNetworkChange(int toBeSentRequests, int successfulRequest, int failedRequests,
                             int totalRequests, int totalSuccessRequests);
    }

    public static DiskStatusListener diskListener;
    public static MemoryListener memoryListener;
    public static NetworkListener networkListener;
    public static NextTimeFlushListener nextTimeFlushListener;

    /**
     * A shortcut method for the situations where the events do not require a property body.
     * see more {@link TikTokBusinessSdk#trackEvent(String, JSONObject)}
     */
    @Deprecated
    public static void trackEvent(String event) {
        appEventLogger.track(event, null);
    }

    @Deprecated
    public static void trackEvent(String event, String eventId) {
        appEventLogger.track(event, null, eventId);
    }

    public static void trackTTEvent(TTBaseEvent event) {
        appEventLogger.track(event.eventName, event.properties, event.eventId);
    }

    public static void trackTTEvent(EventName event) {
        appEventLogger.track(event.toString(), null);
    }

    public static void trackTTEvent(EventName event, String eventId) {
        appEventLogger.track(event.toString(), null, eventId);
    }

    /**
     * <pre>
     * public interface for tracking Event with custom properties.
     * You can pass in any eventName and relevant properties as per
     * <a href="https://ads.tiktok.com/marketing_api/docs?rid=a5vmu2dmwy&id=1679472066464769">here</a>
     *
     * As everything is schemaless in this version, so we highly encourage you to construct the properties
     * conforming to what is described in the above doc.
     *
     * Purchase events could be special, since we are providing a more user-friendly way here:
     * For google play purchase event, see {@link #trackGooglePlayPurchase(TTPurchaseInfo)}
     *
     * For a more common purchase scenario, here is an example
     * {@code
     *      TTPurchaseItem item1 = new TTPurchaseItem(23.5f, 2, "a", "a");
     *      TTPurchaseItem item2 = new TTPurchaseItem(10.5f, 1, "b", "b");
     *
     *      TikTokBusinessSdk.trackEvent("Purchase", TTPurchaseItem.getPurchaseProperty("dollar", item1, item2));
     * }
     * </pre>
     *
     * @param event event name
     */
    @Deprecated
    public static void trackEvent(String event, @Nullable JSONObject props) {
        appEventLogger.track(event, props, "");
    }
    @Deprecated
    public static void trackEvent(String event, @Nullable JSONObject props, String eventId) {
        appEventLogger.track(event, props, eventId);
    }

    /**
     * Track a list of google play purchases at the same time.
     */
    public static void trackGooglePlayPurchase(List<TTPurchaseInfo> purchaseInfos) {
        appEventLogger.trackPurchase(purchaseInfos);
    }

    /**
     * Track a google play purchase, a google purchase is consisted of a Purchase Object
     * and a SkuDetails Object, which are two essential params to construct a {@link TTPurchaseInfo} object here.
     *
     * @param info
     */
    public static void trackGooglePlayPurchase(TTPurchaseInfo info) {
        trackGooglePlayPurchase(Collections.singletonList(info));
    }

    /**
     * Eagerly flush events to network.
     * Normally, all events stored in the memory or on the disk will be flushed to network once some conditions are reached,
     * for example, every {@link TTAppEventLogger#TIME_BUFFER} seconds or there are more than {@link TTAppEventLogger#THRESHOLD} events
     * in the memory.
     */
    public static void flush() {
        appEventLogger.forceFlush();
    }

    /**
     * Internal use only
     * clear all events from memory and disk
     * Calling this method is discouraged
     */
    public static void clearAll() {
        appEventLogger.clearAll();
    }

    /**
     * applicationContext getter
     */
    public static Application getApplicationContext() {
        if (ttSdk == null)
            throw new RuntimeException("TikTokBusinessSdk instance is not initialized");
        return config.application;
    }

    /**
     * sdkInit getter
     */
    public static boolean getNetworkSwitch() {
        return networkSwitch.get();
    }

    public static boolean isGaidCollectionEnabled() {
        return config.advertiserIDCollectionEnable;
    }

    /**
     * logLevel getter
     */
    public static LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * returns api_id
     */
    public static String getAppId() {
        return config.appId;
    }

    /**
     * returns api_id
     */
    public static String getTTAppId() {
        return config.ttAppId;
    }

    public static String[] getTTAppIds() {
        return config.ttAppIds;
    }

    public static BigInteger getFirstTTAppIds() {
        return config.ttFirstAppId;
    }

    /**
     * if only appId is provided, the json schema would be like
     * {
     * "app_id": "1211123727",
     * "batch": [
     * {
     * "type": "track",
     * "event": "PURCHASE",
     * "timestamp": "2020-09-18T19:49:27Z",
     * "context": {
     * "app": {
     * "name": "Angry Birds Classic",
     * "namespace": "com.rovio.baba",
     * "version": "3.0.2",
     * "build": "122"
     * },
     * ....
     * }
     * ....
     * }
     * ]
     * }
     * Only setting appId is discouraged and should be deprecated in the future by making ttAppId
     * mandatory.
     * <p>
     * If both appId and ttAppId are provided, ttAppId is set at the root level while appIds
     * are moved into context.app.id.
     * {
     * "tiktok_app_id": 6899407619589406722,
     * "batch": [
     * {
     * "type": "identify",
     * "event": "INSTALL_APP",
     * "timestamp": "2020-09-17T19:49:27Z",
     * "context": {
     * "app": {
     * "id": "com.rovio.angrybirds",
     * ....
     * },
     * ...
     * }
     * ...
     * }
     * ]
     * }
     */

    public static boolean onlyAppIdProvided() {
        if(config.appId == null){
            logger.warn("AppId should be checked before, this path should not be accessed");
        }
        return config.ttAppId == null;
    }

    /**
     * Both appId and ttAppId are provided
     * @return
     */
    public static boolean bothIdsProvided(){
        return !onlyAppIdProvided();
    }

    public static Boolean getSdkGlobalSwitch() {
        return sdkGlobalSwitch;
    }

    /**
     * if globalSwitch request is sent to network, but the network returns error, activate the app regardless,
     * because TiktokBUsinessSDK.sdkGlobalSwitch is set to true by default.
     * if globalSwitch request is sent to network and api returns false, then sdk will not be activated
     * if globalSwitch request is sent to network and api returns true, then check whether adInfoRun is set to true
     */
    public static boolean isSystemActivated() {
        Boolean sdkGlobalSwitch = TikTokBusinessSdk.getSdkGlobalSwitch();
        if (!sdkGlobalSwitch) {
            logger.info("Global switch is off, ignore all operations");
        }
        return sdkGlobalSwitch;
    }

    public static Boolean isGlobalConfigFetched() {
        return globalConfigFetched.get();
    }

    public static void setGlobalConfigFetched() {
        globalConfigFetched.set(true);
    }

    public static Boolean isInSdkDebugMode() {
        return sdkDebugModeSwitch.get();
    }

    public static Boolean isInSdkLDUMode() {
        return sdkLDUModeSwitch.get();
    }

    public static String getTestEventCode() {
        return testEventCode;
    }

    public static void setSdkGlobalSwitch(Boolean sdkGlobalSwitch) {
        TikTokBusinessSdk.sdkGlobalSwitch = sdkGlobalSwitch;
    }

    public static String getApiAvailableVersion() {
        return apiAvailableVersion;
    }

    public static void setApiAvailableVersion(String apiAvailableVersion) {
        TikTokBusinessSdk.apiAvailableVersion = apiAvailableVersion;
    }

    public static String getApiTrackDomain() {
        return apiTrackDomain;
    }

    public static void setApiTrackDomain(String apiTrackDomain) {
        TikTokBusinessSdk.apiTrackDomain = apiTrackDomain;
    }

    /**
     * Should be called whenever the user info changes <br/>
     * - when the user logins in <br/>
     * - when a new user signs up <br/>
     * - when the user updates his/her profile,logout first, then identify <br/>
     * - If the user's previous user info is remembered and the app is reopened.<br/>
     *
     * @param externalId
     * @param externalUserName
     * @param phoneNumber
     * @param email
     */
    public static synchronized void identify(String externalId,
                                             @Nullable String externalUserName,
                                             @Nullable String phoneNumber,
                                             @Nullable String email) {
        long initTimeMS = System.currentTimeMillis();
        boolean isReset = appEventLogger.identify(externalId, externalUserName, phoneNumber, email);
        if (!isReset) {
            return;
        }
        try {
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                    .put("latency", endTimeMS-initTimeMS)
                    .put("extid", externalId != null)
                    .put("username", externalUserName != null)
                    .put("phone", phoneNumber != null)
                    .put("email", email != null);
            appEventLogger.monitorMetric("identify", meta, null);
        } catch (Exception ignored) {}
    }

    /**
     * Should be called when the user logs out <br/>
     * - when the app logs out <br/>
     * - when switching to another account,
     * in that case, should call a subsequent {@link TikTokBusinessSdk#identify(String, String, String, String)}
     */
    public static synchronized void logout() {
        long initTimeMS = System.currentTimeMillis();
        appEventLogger.logout();
        try {
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                    .put("latency", endTimeMS-initTimeMS);
            appEventLogger.monitorMetric("logout", meta, null);
        } catch (Exception ignored) {}
    }

    public static String getSessionID() {
        return sessionID;
    }

    /**
     * Get app event logger
     *
     * @return app event logger
     */
    public static TTAppEventLogger getAppEventLogger() {
        StackTraceElement[] stackTraceElts = Thread.currentThread().getStackTrace();
        if (TTCrashHandler.isTTSDKRelatedException(Arrays.copyOfRange(stackTraceElts, 3, stackTraceElts.length))) {
            return appEventLogger;
        }
        return null;
    }

    public static void crashSDK() {
        // only used for test purposes
        throw new RuntimeException("force crash from sdk");
    }

    /**
     * To get config and permissions from the app
     * All config items can be set by declaring <meta-data> in AndroidManifest.xml,
     * but they can also be set explicitly by calling the relevant setters methods defined in this class
     * see more {@link TTConfig#disableAutoStart()}, {@link TTConfig#setLogLevel(LogLevel)}
     */
    public static class TTConfig {
        /* application context */
        private final Application application;
        /* api_id for api calls, App ID in EM */
        private String appId = "";
        /* tt_app_id for api calls, TikTok App ID from EM */
        private String ttAppId = "";
        private String[] ttAppIds = {""};
        private BigInteger ttFirstAppId=new BigInteger("0");
        /* flush time interval in seconds, default 15, 0 -> disabled */
        private int flushTime = 15;
//        /* Access-Token for api calls */
//        private String accessToken;
        /* to enable logs */
        private LogLevel logLevel = LogLevel.NONE;
        /* to enable auto event tracking */
        private boolean autoEvent = true;
        /* confirmation to read gaid */
        private boolean advertiserIDCollectionEnable = true;
        /* auto init flag check in manifest */
        private boolean autoStart = true;
        /* disable custom auto events */
        private final List<TTConst.AutoEvents> disabledEvents;
        /* disable monitor metrics */
        private boolean disableMetrics = false;
        /* open debug mode*/
        private boolean debugModeSwitch = false;
        /* open LDU mode*/
        private boolean lduModeSwitch = false;

        private boolean autoIapTrack = false;


        /**
         * Read configs from <meta-data>
         *
         * @param context
         */
        public TTConfig(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            application = (Application) context.getApplicationContext();
            disabledEvents = new ArrayList<>();
        }

        /**
         * Enables debug logs
         */
        public TTConfig setLogLevel(LogLevel ll) {
            this.logLevel = ll;
            return this;
        }

        /**
         * set app id
         */
        public TTConfig setTTAppId(String ttAppId) {
            this.ttAppId = ttAppId;
            try {
                ttAppIds = (ttAppId.replace(" ", "")).split(",");
                ttFirstAppId = new BigInteger(ttAppIds[0]);
            } catch (Throwable throwable) {
            }
            return this;
        }

        /**
         * set app id
         */
        public TTConfig setAppId(String apiId) {
            if (!TextUtils.isEmpty(apiId)) {
                this.appId = apiId;
            }
            return this;
        }

        /**
         * to disable auto event tracking & lifecycle listeners
         */
        public TTConfig disableAutoStart() {
            this.autoStart = false;
            return this;
        }

        /**
         * to disable all auto event tracking captured by lifecycle listeners
         */
        public TTConfig disableAutoEvents() {
            this.autoEvent = false;
            return this;
        }

        /**
         * to disable auto event tracking for InstallApp event
         */
        public TTConfig disableInstallLogging() {
            this.disabledEvents.add(TTConst.AutoEvents.InstallApp);
            return this;
        }

        /**
         * to disable auto event tracking for LaunchAPP event
         */
        public TTConfig disableLaunchLogging() {
            this.disabledEvents.add(TTConst.AutoEvents.LaunchAPP);
            return this;
        }

        /**
         * to disable auto event tracking for 2Dretention event
         */
        public TTConfig disableRetentionLogging() {
            this.disabledEvents.add(TTConst.AutoEvents.SecondDayRetention);
            return this;
        }

        /**
         * to disable gaid in tracking
         */
        public TTConfig disableAdvertiserIDCollection() {
            this.advertiserIDCollectionEnable = false;
            return this;
        }

        /**
         * to set a custom flush time interval in seconds, defaults to 15
         */
        public TTConfig setFlushTimeInterval(int seconds) {
            if (seconds < 0) throw new RuntimeException("Invalid Flush interval");
            this.flushTime = seconds;
            return this;
        }

        /**
         * to disable sdk monitor - metrics tracking
         */
        public TTConfig disableMonitor() {
            disableMetrics = true;
            return this;
        }

        /**
         * to open the debug mode
         */
        public  TTConfig openDebugMode() {
            debugModeSwitch = true;
            return this;
        }
      
        /**
         * to open the LDU(limited data use)mode
         */
        public  TTConfig enableLimitedDataUse() {
            lduModeSwitch = true;
            return this;
        }

        /**
         * to open the Auto In App Purchase Track
         */
        public  TTConfig enableAutoIapTrack() {
            autoIapTrack = true;
            return this;
        }

        public boolean isAutoIapTrack() {
            return autoIapTrack;
        }
    }

    /**
     * Controls the level of logging.
     */
    public enum LogLevel {
        /* No logging. */
        NONE,
        /* Log exceptions only. */
        INFO,
        WARN,
        /* Log exceptions and print debug output. */
        DEBUG;

        public boolean log() {
            return this != NONE;
        }
    }

    public static boolean enableAutoIapTrack() {
        return config != null && config.isAutoIapTrack();
    }

    public interface CrashListener {
        void onCrash(Thread thread, Throwable ex);
    }

    public static void setOnCrashListener(CrashListener crashListener) {
        onCrashListener = crashListener;
    }

    public static CrashListener getCrashListener() {
        return onCrashListener;
    }
}

