package com.tiktok.unity;

import org.json.JSONObject;

import java.lang.reflect.Method;

public class TTUnityBridge {
    public static void setConfigCallback(JSONObject config) {
        if (config == null) {
            return;
        }
        try {
            Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");
            Method method = classtype.getMethod("UnitySendMessage", String.class, String.class, String.class);
            method.invoke(classtype, "TikTokInnerManager", "UpdateConfigFromNative", config.toString());
        } catch (Throwable e) {

        }
    }
}
