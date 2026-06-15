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

            sIsSuccess = result != null && result.has("enable_debug_mode");

            boolean enableDebugMode = JSON.getBoolean(result, "enable_debug_mode", false);

            if (enableDebugMode) {
                TikTokBusinessSdk.enableDebugMode();
            } else {
                TikTokBusinessSdk.disableDebugMode();
            }

            enableScreenshot.set(JSON.getBoolean(result, "enable_screenshot", false));
        } catch (Throwable ignore) {
        }
    }

    public static boolean enableScreenshot() {
        return enableScreenshot.get();
    }
}
