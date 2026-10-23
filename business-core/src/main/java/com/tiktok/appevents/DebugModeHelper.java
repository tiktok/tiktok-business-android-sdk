package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.JSON;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

class DebugModeHelper {
    private static volatile boolean sIsSuccess = false;
    private static final AtomicBoolean enableScreenshot = new AtomicBoolean(false);

    public static boolean isSuccess() {
        return sIsSuccess;
    }

    public static void tryRequestConfig() {
        if (isSuccess()) {
            return;
        }

        try {
            JSONObject result = TTRequest.getDebugModeConfig();
            JSONObject debugModeConfig = getConfigForDebugMode(result);

            sIsSuccess = debugModeConfig != null && debugModeConfig.has("enable_debug_mode");

            boolean enableDebugMode = JSON.getBoolean(debugModeConfig, "enable_debug_mode", false);

            if (enableDebugMode) {
                TikTokBusinessSdk.enableDebugMode();
            } else {
                TikTokBusinessSdk.disableDebugMode();
            }

            enableScreenshot.set(JSON.getBoolean(result, "enable_screenshot", false));
        } catch (Throwable ignore) {
        }
    }

    static JSONObject getConfigForDebugMode(JSONObject response) {
        if (response == null) {
            return null;
        }

        JSONObject businessSdkConfig = response.optJSONObject("business_sdk_config");
        if (businessSdkConfig != null && businessSdkConfig.has("enable_debug_mode")) {
            return businessSdkConfig;
        }
        return response;
    }

    public static boolean enableScreenshot() {
        return enableScreenshot.get();
    }
}
