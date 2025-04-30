package com.tiktok.util;

import android.util.Base64;
import com.tiktok.TikTokBusinessSdk;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class DecryptUtil {
    public static String encryptWithHmac(String message) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(TikTokBusinessSdk.getAccessToken().getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            String token = Base64.encodeToString(mac.doFinal(message.getBytes()), Base64.NO_WRAP);
            return token;
        } catch (Throwable e) {
            return "";
        }
    }
}
