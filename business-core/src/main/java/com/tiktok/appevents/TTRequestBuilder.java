/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import android.content.Context;
import android.os.Build;

import android.os.SystemClock;
import androidx.annotation.Nullable;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

class TTRequestBuilder {
    private static final String TAG = TTRequestBuilder.class.getCanonicalName();

    private static JSONObject basePayloadCache = null;
    private static JSONObject healthBasePayloadCache = null;

    public static JSONObject getBasePayload() {
        TTUtil.checkThread(TAG);
        if (basePayloadCache != null) {
            return basePayloadCache;
        }
        JSONObject result = new JSONObject();

        try {
            if (TikTokBusinessSdk.onlyAppIdProvided()) {// to be compatible with the old versions
                result.put("app_id", TikTokBusinessSdk.getAppId());
            } else {
                result.put("tiktok_app_id", TikTokBusinessSdk.getFirstTTAppIds());
            }
            if (TikTokBusinessSdk.isInSdkDebugMode()) {
                result.put("test_event_code", String.valueOf(TikTokBusinessSdk.getFirstTTAppIds()));
            }
            result.put("event_source", "APP_EVENTS_SDK");
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
            basePayloadCache = new JSONObject();
            return basePayloadCache;
        }
        basePayloadCache = result;
        return result;
    }

    private static JSONObject contextForApiCache = null;

    // the context part that does not change
    private static JSONObject getImmutableContextForApi(TTAppEvent event) throws JSONException {
        if (contextForApiCache != null) {
            freshOsVersion(contextForApiCache, event);
            return contextForApiCache;
        }
        TTIdentifierFactory.AdIdInfo adIdInfo = null;
        long initTimeMS = System.currentTimeMillis();
        try {
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("did_start", TTUtil.getMetaWithTS(initTimeMS), null);
            if (TikTokBusinessSdk.isGaidCollectionEnabled()) {
                // fetch gaid info through google service
                adIdInfo = TTIdentifierFactory.getGoogleAdIdInfo(TikTokBusinessSdk.getApplicationContext());
            }
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(endTimeMS)
                    .put("latency", endTimeMS-initTimeMS)
                    .put("success", adIdInfo.getAdId() != null && adIdInfo.getAdId() != "");
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("did_end", meta, null);
        } catch (Exception ignored) {}
        contextForApiCache = contextBuilder(adIdInfo);
        freshOsVersion(contextForApiCache, event);
        return contextForApiCache;
    }

    private static void freshOsVersion(JSONObject contextForApiCache, TTAppEvent event) {
        try {
            JSONObject device = contextForApiCache.getJSONObject("device");
            if (event != null && device != null) {
                device.put("os_version", SystemInfoUtil.getAndroidVersion());
                device.remove("version");
            } else {
                device.put("version", SystemInfoUtil.getAndroidVersion());
                device.remove("os_version");
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static JSONObject getContextForApi(TTAppEvent event) throws JSONException {
        JSONObject immutablePart = getImmutableContextForApi(event);
        JSONObject finalObj = new JSONObject(immutablePart.toString());
        finalObj.put("user", event.getUserInfo().toJsonObject());
        return finalObj;
    }

    private static Locale getCurrentLocale() {
        Context context = TikTokBusinessSdk.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            // noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }

    static String getBcp47Language() {
        Locale loc = getCurrentLocale();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return loc.toLanguageTag();
        }

        // we will use a dash as per BCP 47
        final char SEP = '-';
        String language = loc.getLanguage();
        String region = loc.getCountry();
        String variant = loc.getVariant();

        // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
        // this goes before the string matching since "NY" wont pass the variant checks
        if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
            language = "nn";
            region = "NO";
            variant = "";
        }

        if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}")) {
            language = "und";       // Follow the Locale#toLanguageTag() implementation
            // which says to return "und" for Undetermined
        } else if (language.equals("iw")) {
            language = "he";        // correct deprecated "Hebrew"
        } else if (language.equals("in")) {
            language = "id";        // correct deprecated "Indonesian"
        } else if (language.equals("ji")) {
            language = "yi";        // correct deprecated "Yiddish"
        }

        // ensure valid country code, if not well formed, it's omitted
        if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}")) {
            region = "";
        }

        // variant subtags that begin with a letter must be at least 5 characters long
        if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}")) {
            variant = "";
        }

        StringBuilder bcp47Tag = new StringBuilder(language);
        if (!region.isEmpty()) {
            bcp47Tag.append(SEP).append(region);
        }
        if (!variant.isEmpty()) {
            bcp47Tag.append(SEP).append(variant);
        }

        return bcp47Tag.toString();
    }

    private static JSONObject contextBuilder(@Nullable TTIdentifierFactory.AdIdInfo adIdInfo) throws JSONException {
        JSONObject app = new JSONObject();
        if (TikTokBusinessSdk.bothIdsProvided()) {
            app.put("id", TikTokBusinessSdk.getAppId());
        }
        app.put("name", SystemInfoUtil.getAppName());
        app.put("namespace", SystemInfoUtil.getPackageName());
        app.put("version", SystemInfoUtil.getAppVersionName());
        app.put("build", SystemInfoUtil.getAppVersionCode() + "");

        JSONObject device = new JSONObject();
        device.put("platform", "Android");
        if (adIdInfo != null) {
            device.put("gaid", adIdInfo.getAdId());
        }

        JSONObject library = new JSONObject();
        library.put("name", "tiktok/tiktok-business-android-sdk");
        library.put("version", SystemInfoUtil.getSDKVersion());

        JSONObject context = new JSONObject();
        app.put("tiktok_app_id", TikTokBusinessSdk.getTTAppId());
        context.put("app", app);
        context.put("library", library);
        context.put("device", device);
        context.put("locale", getBcp47Language());
        context.put("ip", SystemInfoUtil.getLocalIpAddress());

        String userAgent = SystemInfoUtil.getUserAgent();
        if (userAgent != null) {
            context.put("user_agent", userAgent);
        }
        return context;
    }

    private static JSONObject enrichDeviceBase(JSONObject d) throws JSONException {
        JSONObject device = new JSONObject(d.toString());
        device.put("id", TTUtil.getOrGenAnoId(TikTokBusinessSdk.getApplicationContext(), false));
        device.put("user_agent", SystemInfoUtil.getUserAgent());
        device.put("ip", SystemInfoUtil.getLocalIpAddress());
        device.put("network", SystemInfoUtil.getNetworkClass(TikTokBusinessSdk.getApplicationContext()));
        device.put("session", TikTokBusinessSdk.getSessionID());
        device.put("locale", getBcp47Language());
        device.put("ts", System.currentTimeMillis()-SystemClock.elapsedRealtime());
        return device;
    }

    public static JSONObject getHealthMonitorBase() throws JSONException {
        if (healthBasePayloadCache != null) {
            healthBasePayloadCache.put("device",
                    enrichDeviceBase(healthBasePayloadCache.getJSONObject("device")));
            return healthBasePayloadCache;
        }
        JSONObject finalObj = new JSONObject();
        JSONObject app = new JSONObject(getImmutableContextForApi(null).getJSONObject("app").toString());
        app.put("app_namespace", SystemInfoUtil.getPackageName());
        finalObj.put("app", app);
        finalObj.put("library", getImmutableContextForApi(null).get("library"));
        finalObj.put("device", enrichDeviceBase(getImmutableContextForApi(null).getJSONObject("device")));
        finalObj.put("log_extra", null);
        healthBasePayloadCache = finalObj;
        return healthBasePayloadCache;
    }
}