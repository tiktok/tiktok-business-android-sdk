/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents.edp;

import static com.tiktok.appevents.edp.EDPConfig.report_frequency_control;
import static com.tiktok.appevents.edp.EDPConfig.time_diff_frequency_control;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_NAME_APP_LAUNCH;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_NAME_CLICK;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_NAME_PAGE_SHOW;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_NAME_PAY_SHOW;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_NAME_WEBVIEW_REQUEST;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLASS_NAME;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLICK_BUTTON_TEXT;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLICK_DURATION;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLICK_POSITON_X;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLICK_POSITON_Y;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLICK_SIZE_H;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CLICK_SIZE_W;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_CURRENT_PAGE_NAME;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_FROM_BACKGROUND;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_INDEX;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_PAGE_COMPONENTS;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_PAGE_DEEP_COUNT;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_PAY_CODE;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_PAY_SKU_INFO;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_REFER;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_SOURCE_URL;
import static com.tiktok.appevents.edp.TTEDPEventConstants.EDP_EVENT_PROPERTY_WEBVIEW_REQUEST_URL;

import com.tiktok.TikTokBusinessSdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

public class TTEDPEventTrack {
    private static TTAppLaunchEvent ttAppLaunchEvent;
    public static long LAST_CLICK_TS = 0l;
    private static boolean hasSendLaunch = false;
    public static volatile boolean isSending = false;
    public static volatile boolean pageShowIsSending = false;
    public static void trackAppLaunch(String refer, String sourceUrl){
        try {
            JSONObject property = new JSONObject();
            property.put(EDP_EVENT_PROPERTY_REFER, refer);
            property.put(EDP_EVENT_PROPERTY_SOURCE_URL, sourceUrl);
            JSONObject eventProp = new JSONObject();
            eventProp.put("meta", property);
            if (TikTokBusinessSdk.isInitialized()) {
                TikTokBusinessSdk.getAppEventLogger().trackEdp(EDP_EVENT_NAME_APP_LAUNCH, eventProp, null);
            } else {
                if (ttAppLaunchEvent == null && !hasSendLaunch) {
                    ttAppLaunchEvent = new TTAppLaunchEvent(property, System.currentTimeMillis());
                }
            }
        }catch (Throwable throwable){

        }
    }

    public static void trackFirstAppLaunch() {
        try {
            if (ttAppLaunchEvent != null) {
                hasSendLaunch = true;
                JSONObject eventProp = new JSONObject();
                eventProp.put("meta", ttAppLaunchEvent.getProp());
                TikTokBusinessSdk.getAppEventLogger().trackEdp(EDP_EVENT_NAME_APP_LAUNCH, eventProp, null);
                ttAppLaunchEvent = null;
            }
        } catch (Exception e) {

        }
    }

    public static void trackWebviewRequest(String url){
        try {
            JSONObject property = new JSONObject();
            property.put(EDP_EVENT_PROPERTY_WEBVIEW_REQUEST_URL, url);
            JSONObject eventProp = new JSONObject();
            eventProp.put("meta", property);
            TikTokBusinessSdk.getAppEventLogger().trackEdp(EDP_EVENT_NAME_WEBVIEW_REQUEST, eventProp, null);
        }catch (Throwable throwable){

        }
    }

    public static void trackPageShow(String pageName, int index, boolean isFromBackground, JSONObject components, int pageCount){
        try {
            JSONObject property = new JSONObject();
            property.put(EDP_EVENT_PROPERTY_CURRENT_PAGE_NAME, pageName);
            property.put(EDP_EVENT_PROPERTY_INDEX, index);
            property.put(EDP_EVENT_PROPERTY_FROM_BACKGROUND, isFromBackground);
            property.put(EDP_EVENT_PROPERTY_PAGE_COMPONENTS, components);
            property.put(EDP_EVENT_PROPERTY_PAGE_DEEP_COUNT, pageCount);
            JSONObject eventProp = new JSONObject();
            eventProp.put("meta", property);
            TikTokBusinessSdk.getAppEventLogger().trackEdp(EDP_EVENT_NAME_PAGE_SHOW, eventProp, null);
            pageShowIsSending = false;
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }

    public static void trackClick(String className, float x, float y, int width, int height, String text,
                                  String pageName, JSONObject components, int pageCount, long duration){
        try {
            LAST_CLICK_TS = System.currentTimeMillis();
            JSONObject property = new JSONObject();
            property.put(EDP_EVENT_PROPERTY_CLICK_POSITON_X, x);
            property.put(EDP_EVENT_PROPERTY_CLICK_POSITON_Y, y);
            property.put(EDP_EVENT_PROPERTY_CLICK_SIZE_W, width);
            property.put(EDP_EVENT_PROPERTY_CLICK_SIZE_H, height);
            property.put(EDP_EVENT_PROPERTY_CLICK_BUTTON_TEXT, text);
            property.put(EDP_EVENT_PROPERTY_CURRENT_PAGE_NAME, pageName);
            property.put(EDP_EVENT_PROPERTY_PAGE_COMPONENTS, components);
            property.put(EDP_EVENT_PROPERTY_PAGE_DEEP_COUNT, pageCount);
            property.put(EDP_EVENT_PROPERTY_CLICK_DURATION, duration);
            property.put(EDP_EVENT_PROPERTY_CLASS_NAME, className);
            JSONObject eventProp = new JSONObject();
            eventProp.put("meta", property);
            TikTokBusinessSdk.getAppEventLogger().trackEdp(EDP_EVENT_NAME_CLICK, eventProp, null);
            isSending = false;
        }catch (Throwable throwable){

        }
    }

    public static void trackPayShow(int code, JSONArray skuInfo){
        try {
            JSONObject property = new JSONObject();
            property.put(EDP_EVENT_PROPERTY_PAY_CODE, code);
            property.put(EDP_EVENT_PROPERTY_PAY_SKU_INFO, skuInfo);
            JSONObject eventProp = new JSONObject();
            eventProp.put("meta", property);
            TikTokBusinessSdk.getAppEventLogger().trackEdp(EDP_EVENT_NAME_PAY_SHOW, eventProp, null);
        }catch (Throwable throwable){

        }
    }

    public static void trackUnityEvent(String name, JSONObject meta){
        try {
            JSONObject eventProp = new JSONObject();
            eventProp.put("meta", meta);
            eventProp.put("api_platform", meta.remove("api_platform"));
            TikTokBusinessSdk.getAppEventLogger().trackEdp(name, eventProp, null);
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }

    public static boolean checkUpload() {
        if (new Random().nextDouble() <= report_frequency_control) {
            return true;
        }
        return false;
    }

}
