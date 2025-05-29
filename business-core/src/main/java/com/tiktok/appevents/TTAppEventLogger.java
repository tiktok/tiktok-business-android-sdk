/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.TikTokBusinessSdk.INVALID_ID;
import static com.tiktok.appevents.ErrorData.TT_DDL_CODE_HTTP_ERROR;
import static com.tiktok.appevents.ErrorData.TT_DDL_MSG_HTTP_ERROR;
import static com.tiktok.appevents.edp.EDPConfig.ConfigConst.ENABLE_SDK;
import static com.tiktok.appevents.edp.EDPConfig.ConfigConst.EDP_NATIVE_SDK_CONFIG;
import static com.tiktok.appevents.edp.EDPConfig.ConfigConst.EDP_UNITY_SDK_CONFIG;
import static com.tiktok.util.TTConst.ERROR_MESSAGE_INVALID_ID;
import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import static com.tiktok.appevents.edp.TTEDPEventTrack.trackFirstAppLaunch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.edp.EDPConfig;
import com.tiktok.appevents.edp.TTEDPEventTrack;
import com.tiktok.unity.TTUnityBridge;
import com.tiktok.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TTAppEventLogger {
    static final String SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF = "Skip flushing because global switch is turned off";
    static final String SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED = "Skip flushing because global config is not fetched";
    static final String TAG = TTAppEventLogger.class.getName();

    // every TIME_BUFFER seconds, a flush task will be pushed to the execution queue
    private static int TIME_BUFFER;
    int counter;
    // once THRESHOLD events got accumulated in the memory, a flush task will be pushed to the execution queue
    static final int THRESHOLD = 100;
    public static final String NETWORK_IS_TURNED_OFF = "SDK can't send tracking events to server, it will be cached locally, and will be sent in batches only after startTracking";

    static int totalDumped = 0;

    // whether to trigger automatic events in the lifeCycle callbacks provided by Android
    final boolean lifecycleTrackEnable;
    // custom auto event disable, events will be disabled when disabledEvents.contains(event)
    final List<TTConst.AutoEvents> disabledEvents;
    /**
     * Logger util
     */
    TTLogger logger;
    /**
     * Lifecycle
     */
    Lifecycle lifecycle;

    // for internal debug purpose
    int flushId = 0;
    public static boolean autoTrackRetentionEnable = true;
    public static boolean autoTrackPaymentEnable = true;

    // similar to what javascript has, so that all the internal tasks are executed in a waterfall fashion, avoiding race conditions
    static ScheduledExecutorService eventLoop = Executors.newSingleThreadScheduledExecutor(new TTThreadFactory());
    ScheduledFuture<?> future = null;

    // used by internal monitor
    static ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor(new TTThreadFactory());
    ScheduledFuture<?> timeFuture = null;
    private final Runnable batchFlush = () -> flush(FlushReason.TIMER);

    final TTAutoEventsManager autoEventsManager;

    static boolean metricsEnabled = true;
    private final static TTLifecycleListener mLifecycleListener = new TTLifecycleListener();
    Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    private Runnable heartRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                startHeart();
                if (TTActivityLifecycleCallbacksListener.isBackground()) {
                    return;
                }
                if (!TikTokBusinessSdk.isGlobalConfigFetched()) {
                    return;
                }
                if (!TikTokBusinessSdk.isSystemActivated()) {
                    return;
                }
                JSONObject meta = TTUtil.getMetaWithTS(System.currentTimeMillis());
                TikTokBusinessSdk.getAppEventLogger().monitorMetric("session_activity", meta, null);
            } catch (Throwable e) {

            }
        }
    };

    public static List<TTAppEvent> getSuccessfulEvents() {
        return TTRequest.getSuccessfullySentRequests();
    }

    public TTAppEventLogger(boolean lifecycleTrackEnable,
                            List<TTConst.AutoEvents> disabledEvents,
                            int flushTime,
                            boolean monitorDisable) {
        logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;
        this.disabledEvents = disabledEvents;
        TIME_BUFFER = flushTime;
        counter = flushTime;
        lifecycle = ProcessLifecycleOwner.get().getLifecycle();
        if (monitorDisable) {
            metricsEnabled = false;
        }

        /** ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacksListener activityLifecycleCallbacks = new TTActivityLifecycleCallbacksListener(this);
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                TTAppEventLogger.this.lifecycle.addObserver(activityLifecycleCallbacks);
            } else {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        TTAppEventLogger.this.lifecycle.addObserver(activityLifecycleCallbacks);
                    }
                });
            }
            TikTokBusinessSdk.getApplicationContext().registerActivityLifecycleCallbacks(mLifecycleListener);
        }catch (Throwable throwable){

        }

        autoEventsManager = new TTAutoEventsManager(this);
    }

    public void initConfig(long initTimeMS, final TikTokBusinessSdk.TTInitCallback callback, AtomicBoolean sdkInitialized){
        addToQ(() -> {
            startHeart();
        });
        addToQ(SystemInfoUtil::initAppSessionId);
        addToQ(SystemInfoUtil::initInstallReferrer);
        addToQ(SystemInfoUtil::updateSensigInfo);
        addToQ(SystemInfoUtil::initUserAgent);
        addToQ(TTAppEventsQueue::clearAll);
        addToQ(TTEdpAppEventsQueue::clearAll);
        addToQ(() -> {
            try {
                if (TextUtils.isEmpty(TikTokBusinessSdk.getTTAppId()) || TextUtils.isEmpty(TikTokBusinessSdk.getAppId())) {
                    if (callback != null) {
                        callback.fail(INVALID_ID, ERROR_MESSAGE_INVALID_ID);
                    }
                } else {
                    sdkInitialized.set(true);
                    if (callback != null) {
                        callback.success();
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        fetchGlobalConfig(0);
        monitorMetric("init_start", TTUtil.getMetaWithTS(initTimeMS), null);
    }

    /**
     * persist events to the disk
     */
    void persistEvents() {
        addToQ(() -> TTAppEventStorage.persist(null));
    }

    public void trackPurchase(List<TTPurchaseInfo> purchaseInfos) {
        if (!TikTokBusinessSdk.isSystemActivated()) {
            logger.info("Global switch is off, ignore track purchase");
            return;
        }
        addToQ(() -> {
            if (purchaseInfos.isEmpty()) {
                return;
            }

            for (TTPurchaseInfo purchaseInfo : purchaseInfos) {
                JSONObject property = TTInAppPurchaseManager.getPurchaseProps(purchaseInfo);
                if (property != null) {
                    track("Purchase", property, purchaseInfo.getEventId());
                }
            }
        });
    }

    public void startHeart() {
        TTHandlerUtil.getInstance().removeCallbacks(heartRunnable);
        TTHandlerUtil.getInstance().postDelayed(heartRunnable, 60000);
    }

    public void closeHeart() {
        TTHandlerUtil.getInstance().removeCallbacks(heartRunnable);
    }

    void startScheduler() {
        if (TIME_BUFFER != 0) {
            doStartScheduler(TIME_BUFFER, false);
        }
    }

    void restartScheduler() {
        if (TIME_BUFFER != 0) {
            doStartScheduler(TIME_BUFFER, true);
        }
    }

    /**
     * Try to flush to network every {@link TTAppEventLogger#TIME_BUFFER} seconds
     * Like setTimeInterval in js
     */
    private void doStartScheduler(int interval, boolean immediate) {
        try {
            if (future == null) {
                future = eventLoop.scheduleAtFixedRate(batchFlush, immediate ? 0 : interval, interval, TimeUnit.SECONDS);
            }
            if (timeFuture == null && TikTokBusinessSdk.nextTimeFlushListener != null) {
                counter = interval;
                timeFuture = timerService.scheduleAtFixedRate(() -> {
                    TikTokBusinessSdk.nextTimeFlushListener.timeLeft(counter);
                    if (counter == 0) {
                        counter = interval;
                    }
                    counter--;
                }, 0, 1, TimeUnit.SECONDS);
            }
        }catch (Throwable e){
            TikTokBusinessSdk.setSdkGlobalSwitch(false);
        }
    }

    /**
     * Stop the recurrent task when the user interface is no longer interactive
     */
    void stopScheduler() {
        try {
            if (future != null) {
                future.cancel(false);
                future = null;
            }
            if (timeFuture != null) {
                timeFuture.cancel(false);
                timeFuture = null;
            }
        }catch (Throwable e){
            logger.error(e,"stop scheduler exception");
        }
    }

    public boolean identify(String externalId,
                            @Nullable String externalUserName,
                            @Nullable String phoneNumber,
                            @Nullable String email) {
        TTUserInfo sharedInstance = TTUserInfo.sharedInstance;
        if (sharedInstance.isIdentified()) {
            logger.warn("SDK is already identified, if you want to switch to another" +
                    "user account, plz call TiktokBusinessSDK.logout() first and then identify");
            return false;
        }
        sharedInstance.setIdentified();
        if (!TextUtils.isEmpty(externalId)) {
            sharedInstance.setExternalId(externalId);
        }
        if (!TextUtils.isEmpty(externalUserName)) {
            sharedInstance.setExternalUserName(externalUserName);
        }
        if (!TextUtils.isEmpty(phoneNumber)) {
            sharedInstance.setPhoneNumber(phoneNumber);
        }
        if (!TextUtils.isEmpty(email)) {
            sharedInstance.setEmail(email);
        }
        trackEvent(TTAppEvent.TTAppEventType.identify, null, null, null, false);
        flushWithReason(TTAppEventLogger.FlushReason.IDENTIFY);
        return true;
    }

    public void logout() {
        TTUserInfo.reset(TikTokBusinessSdk.getApplicationContext(), true);
        flushWithReason(TTAppEventLogger.FlushReason.LOGOUT);
    }

    /**
     * interface exposed to {@link TikTokBusinessSdk}
     *
     * @param event
     * @param props
     */
    public void track(String event, @Nullable JSONObject props) {
        trackEvent(TTAppEvent.TTAppEventType.track, event, props, null, false);
    }
    public void track(String event, @Nullable JSONObject props, String eventId) {
        trackEvent(TTAppEvent.TTAppEventType.track, event, props, eventId, false);
    }

    public void trackEdp(String event, @Nullable JSONObject props, String eventId) {
        trackEvent(TTAppEvent.TTAppEventType.track, event, props, eventId, true);
    }

    private void trackEvent(TTAppEvent.TTAppEventType type, String event, @Nullable JSONObject props, String eventId, boolean edp) {
        if (!TikTokBusinessSdk.isSystemActivated() || TextUtils.isEmpty(TikTokBusinessSdk.getAppId())) {
            return;
        }
        try {
            if ("enhanced_data_postback".equals(props.optString("monitor_type", ""))) {
                TTEDPEventTrack.trackUnityEvent(event, props);
                return;
            }
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }

        JSONObject finalProps = props != null ? props : new JSONObject();
        if(TikTokBusinessSdk.isEnableDebugMode()){
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (edp) {
                            finalProps.put("track_source", "edp");
                        }
                        TTAppEvent ttAppEvent = new TTAppEvent(type, event, finalProps.toString(), eventId, TikTokBusinessSdk.getTTAppIds());
                        ttAppEvent.setScreenShot();
                        addToTask(ttAppEvent, null, null, null, null, edp);
                    }catch (Throwable e){

                    }
                }
            });
        } else {
            addToTask(null, type, event, finalProps, eventId, edp);
        }
    }

    private void addToTask(TTAppEvent appEvent,TTAppEvent.TTAppEventType type, String event, @Nullable JSONObject props, String eventId, boolean edp){
        Runnable task = () -> {
            try {
                TTAppEvent ttAppEvent = appEvent;
                if (ttAppEvent == null) {
                    if(edp){
                        props.put("track_source", "edp");
                    }
                    ttAppEvent = new TTAppEvent(type, event, props.toString(), eventId, TikTokBusinessSdk.getTTAppIds());
                }
                 if (edp) {
                    TTEdpAppEventsQueue.addEvent(ttAppEvent);
                } else {
                    TTAppEventsQueue.addEvent(ttAppEvent);
                }

                if (TTAppEventsQueue.size() + TTEdpAppEventsQueue.size() > THRESHOLD) {
                    flush(FlushReason.THRESHOLD);
                }

            }catch (Throwable e){

            }
        };
        addToQ(task);
    }


    public void forceFlush() {
        flushWithReason(FlushReason.FORCE_FLUSH);
    }

    public void flushWithReason(FlushReason reason) {
        logger.debug(reason.name() + " triggered flush");
        addToQ(() -> flush(reason));
    }

    // only when this method is called will the whole sdk be activated
    private void activateSdk() {
        autoEventsManager.trackOnAppOpenEvents();
        startScheduler();
        flush(FlushReason.START_UP);
        trackFirstAppLaunch();
    }

    void flush(FlushReason reason) {
        long initTimeMS = System.currentTimeMillis();
        TTUtil.checkThread(TAG);

        // if global config is not fetched, we can track events and put in into memory
        // but they should not be sent to the network
        if (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            logger.info(SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED);
            return;
        }
        // global switch is turned off, dump all events
        if (!TikTokBusinessSdk.isSystemActivated()) {
            logger.info(SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF);
            return;
        }

        int flushSize = 0;

        try {
            if (TikTokBusinessSdk.getNetworkSwitch()) {
                logger.debug("Start flush, version %d reason is %s", flushId, reason.name());

                TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

                appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

                flushSize = appEventPersist.getAppEvents().size() + TTEdpAppEventsQueue.size();

                List<TTAppEvent> failedEvents = TTRequest
                        .reportAppEvent(TTRequestBuilder.getBasePayloadWithTs(), appEventPersist.getAppEvents(), false);
                TTRequest
                        .reportAppEvent(TTRequestBuilder.getBasePayloadWithTs(), TTEdpAppEventsQueue.exportAllEvents(), true);

                if (!failedEvents.isEmpty()) { // flush failed, persist events
                    logger.debug("Failed to send %d events, will save to disk", failedEvents.size());
                    TTAppEventStorage.persist(failedEvents);
                }
                logger.debug("END flush, version %d reason is %s", flushId, reason.name());

                flushId++;
            } else {
                logger.info(NETWORK_IS_TURNED_OFF);
                TTAppEventStorage.persist(null);
            }
        } catch (Throwable e) {
            TTEdpAppEventsQueue.clearAll();
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
        }

        if (flushSize != 0) {
            try {
                long endTimeMS = System.currentTimeMillis();
                JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                        .put("latency", endTimeMS-initTimeMS)
                        .put("type",reason.name())
                        .put("interval", TIME_BUFFER)
                        .put("size", flushSize);
                monitorMetric("flush", meta, null);
            } catch (Exception ignored) {}
        }

        addToQ(TTCrashHandler::initCrashReporter);
    }

    public void destroy() {
        TTAppEventsQueue.clearAll();
        TTEdpAppEventsQueue.clearAll();
        stopScheduler();
    }

    /**
     * flush reasons
     */
    public enum FlushReason {
        THRESHOLD, // when reaching the threshold of the event queue
        TIMER, // triggered every 15 seconds
        START_UP, // when app is started, flush all the accumulated events
        FORCE_FLUSH, // when developer calls flush from app
        IDENTIFY, // when calling identify
        LOGOUT, //when logging out
    }

    public void addToQ(Runnable task) {
        // http://www.javabyexamples.com/handling-exceptions-from-executorservice-tasks
        try {
            eventLoop.execute(task);
        } catch (Throwable e) {
            onExecuteFailed(task, e);
        }
    }

    private void onExecuteFailed(Runnable runnable, Throwable e){
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runnable.run();
        } else {
            logger.error(e,"Runnable execute error");
        }
    }


    // Do not remove, for the ease of local test
    private void addToLater(Runnable task, int seconds) {
        // http://www.javabyexamples.com/handling-exceptions-from-executorservice-tasks
        try {
            eventLoop.schedule(task, seconds, TimeUnit.SECONDS);
        } catch (Throwable e) {
            onExecuteFailed(task, e);
        }
    }

    public void clearAll() {
        addToQ(this::clearAllImmediately);
    }

    private void clearAllImmediately() {
        TTAppEventsQueue.clearAll();
        TTEdpAppEventsQueue.clearAll();
        TTAppEventStorage.clearAll();
    }

    /**
     * set remote switch and api available version
     * if the remote config is not fetched, the events can only be saved in memory
     * if the config is fetched and config.globalSwitch is true, events can be saved in memory or on the disk.
     * if the config is fetched and config.globalSwitch is false, the events can neither be saved in memory nor on the disk
     * any events in the memory will be gone when the app is closed.
     */
    public void fetchGlobalConfig(int delaySeconds) {
        addToLater(() -> {
            try {
                logger.info("Fetching global config....");

                JSONObject requestResult = TTRequest.getBusinessSDKConfig();

                if (requestResult == null) {
                    logger.info("Opt out of initGlobalConfig because global config is null, api returns error");
                    return;
                }

                JSONObject businessSdkConfig = (JSONObject) requestResult.get("business_sdk_config");
                Boolean enableSDK = businessSdkConfig.getBoolean("enable_sdk");
                String availableVersion = businessSdkConfig.getString("available_version");
                String trackEventDomain = businessSdkConfig.getString("domain");
                boolean enableDebugMode = businessSdkConfig.optBoolean("enable_debug_mode", false);

                TikTokBusinessSdk.setSdkGlobalSwitch(enableSDK);
                logger.debug("enable_sdk=" + enableSDK);
                // if sdk is shutdown, stop all the timers
                if (!enableSDK) {
                    logger.info("Clear all events and stop timers because global switch is not turned on");
                    clearAllImmediately();
                }
                if(enableDebugMode){
                    TikTokBusinessSdk.enableDebugMode();
                }else {
                    TikTokBusinessSdk.disableDebugMode();
                }
                TikTokBusinessSdk.setApiAvailableVersion(availableVersion);
                TikTokBusinessSdk.setApiTrackDomain(trackEventDomain);
                logger.debug("available_version=" + availableVersion);
                TikTokBusinessSdk.setGlobalConfigFetched();
                autoTrackRetentionEnable = businessSdkConfig.optBoolean("auto_track_Retention_enable");
                autoTrackPaymentEnable = businessSdkConfig.optBoolean("auto_track_Payment_enable");
                TTUnityBridge.setConfigCallback(requestResult);
                EDPConfig.optConfig(businessSdkConfig.optJSONObject(EDP_NATIVE_SDK_CONFIG));
            } catch (JSONException e) {
                e.printStackTrace();
                logger.warn("Errors happened during initGlobalConfig because the structure of api result is not correct");
            } catch (Exception e) {
                logger.warn("Errors occurred during initGlobalConfig because of " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (TikTokBusinessSdk.isSystemActivated() && !TikTokBusinessSdk.isActivatedLogicRun) {
                    TikTokBusinessSdk.isActivatedLogicRun = true;
                    activateSdk();
                }
            }
        }, delaySeconds);
    }

    public void monitorMetric(@NonNull String name,
                              @Nullable JSONObject meta,
                              @Nullable JSONObject extra) {
        if (!metricsEnabled) return;
        addToQ(() -> {
            JSONObject stat = new JSONObject();
            try {
                stat = TTRequestBuilder.getHealthMonitorBase();
            } catch (Exception ignored) {}
            JSONObject monitor = new JSONObject();
            try {
                monitor.put("type", "metric");
                monitor.put("name", name);
                if (meta != null) {
                    monitor.put("meta", meta);
                }
                if (extra != null) {
                    monitor.put("extra", extra);
                }
                stat.put("monitor", monitor);
            } catch (Exception ignored) {}
            TTCrashHandler.retryLater(stat);
        });
    }

    public void fetchDeferredDeeplinkWithCompletion(TikTokBusinessSdk.FetchDeferredDeeplinkCompletion callback) {
        addToQ(() -> {
            try {
                JSONObject ddlInfo = new JSONObject(TTRequest.fetchDeferredDeeplinkWithCompletion());
                int code = ddlInfo.optInt("code");
                String data = ddlInfo.optJSONObject("data").optString("ddl");
                if (code == 0 && !TextUtils.isEmpty(data)) {
                    callback.completion(data, null);
                } else {
                    callback.completion("", new ErrorData(code, ddlInfo.optString("message", "")));
                }
            } catch (Throwable throwable) {
                callback.completion("", new ErrorData(TT_DDL_CODE_HTTP_ERROR, TT_DDL_MSG_HTTP_ERROR ));
            }
        });
    }

    void persistMonitor() {
        addToQ(TTCrashHandler::persistToFile);
    }
}