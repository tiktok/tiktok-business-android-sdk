package com.tiktok.util;

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
}
