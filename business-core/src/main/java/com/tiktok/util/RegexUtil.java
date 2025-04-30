package com.tiktok.util;

import static com.tiktok.appevents.TTUserInfo.toSha256;

import android.text.TextUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {
    public static boolean validateAppId(String appId) {
        String appIdRegex = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$";
        Pattern pattern = Pattern.compile(appIdRegex);
        Matcher matcher = pattern.matcher(appId);
        return matcher.matches();
    }

    public static boolean validateTTAppId(String ttAppId) {
        String ttAppIdRegex = "^(\\d+,)*\\d+$";
        Pattern pattern = Pattern.compile(ttAppIdRegex);
        Matcher matcher = pattern.matcher(ttAppId);
        return matcher.matches();
    }

    public static String replaceAllToHash(String regex, String origin) {
        try {
            if (TextUtils.isEmpty(regex) || TextUtils.isEmpty(origin)) {
                return origin;
            }
            StringBuffer stringBuffer = new StringBuffer();
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(origin);
            while (matcher.find()) {
                String sha256 = toSha256(matcher.group());
                matcher.appendReplacement(stringBuffer, sha256);
            }
            matcher.appendTail(stringBuffer);
            return stringBuffer.toString();
        } catch (Throwable throwable) {
            return "";
        }
    }
}
