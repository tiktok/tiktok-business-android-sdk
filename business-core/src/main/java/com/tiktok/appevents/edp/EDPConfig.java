/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents.edp;

import android.text.TextUtils;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class EDPConfig {
    public interface ConfigConst {
        String EDP_NATIVE_SDK_CONFIG = "enhanced_data_postback_native_config";
        String EDP_UNITY_SDK_CONFIG = "enhanced_data_postback_unity_config";
        String ENABLE_SDK = "enable_sdk";
        String ENABLE_APP_LAUNCH_TRACK = "enable_app_launch_track";
        String ENABLE_PAGE_SHOW_TRACK = "enable_page_show_track";
        String ENABLE_CLICK_TRACK = "enable_click_track";
        String ENABLE_WEBVIEW_REQUEST_TRACK = "enable_webview_request_track";
        String ENABLE_PAY_SHOW_TRACK = "enable_pay_show_track";
        String PAGE_DETAIL_UPLOAD_DEEP_COUNT = "page_detail_upload_deep_count";
        String TIME_DIFF_FREQUENCY_CONTROL = "time_diff_frequency_control";
        String REPORT_FREQUENCY_CONTROL = "report_frequency_control";
        String BUTTON_BLACK_LIST = "button_black_list";
        String SENSIG_FILTERING_REGEX_LIST = "sensig_filtering_regex_list";
        String SENSIG_FILTERING_REGEX_VERSION = "sensig_filtering_regex_version";
    }

    public static final String DEFAULT_SENSIG_FILTERING_REGEX_LIST = "([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9._-]+)|(\\+?0?86-?)?1[3-9]\\d{9}|(\\+\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}";
    public static boolean enable_sdk = false;
    public static boolean enable_app_launch_track = false;
    public static boolean enable_page_show_track = false;
    public static boolean enable_click_track = false;
    public static boolean enable_webview_request_track = false;
    public static boolean enable_pay_show_track = false;
    public static int page_detail_upload_deep_count = 12;
    public static double time_diff_frequency_control = 0;
    public static double report_frequency_control = 1;
    public static Set<String> button_black_list = new HashSet<>();
    public static String sensig_filtering_regex_list = DEFAULT_SENSIG_FILTERING_REGEX_LIST;
    public static int sensig_filtering_regex_version = 0;

    public static void optConfig(JSONObject config) {
        if (config == null) {
            return;
        }
        try {
            enable_sdk = config.optBoolean(ConfigConst.ENABLE_SDK, false);
            enable_app_launch_track = enable_sdk && config.optBoolean(ConfigConst.ENABLE_APP_LAUNCH_TRACK, false);
            enable_page_show_track = enable_sdk && config.optBoolean(ConfigConst.ENABLE_PAGE_SHOW_TRACK, false);
            enable_click_track = enable_sdk && config.optBoolean(ConfigConst.ENABLE_CLICK_TRACK, false);
            enable_webview_request_track = enable_sdk && config.optBoolean(ConfigConst.ENABLE_WEBVIEW_REQUEST_TRACK, false);
            enable_pay_show_track = enable_sdk && config.optBoolean(ConfigConst.ENABLE_PAY_SHOW_TRACK, false);
            page_detail_upload_deep_count = config.optInt(ConfigConst.PAGE_DETAIL_UPLOAD_DEEP_COUNT, 0);
            time_diff_frequency_control = config.optDouble(ConfigConst.TIME_DIFF_FREQUENCY_CONTROL, 0);
            report_frequency_control = config.optDouble(ConfigConst.REPORT_FREQUENCY_CONTROL, 0.0);
            JSONArray buttonBlackList = config.optJSONArray(ConfigConst.BUTTON_BLACK_LIST);
            JSONArray sensigFilteringRegexList = config.optJSONArray(ConfigConst.SENSIG_FILTERING_REGEX_LIST);
            button_black_list.clear();
            for (int i = 0; i < buttonBlackList.length(); i++) {
                if (!TextUtils.isEmpty(buttonBlackList.getString(i))) {
                    button_black_list.add(buttonBlackList.getString(i));
                }
            }
            if (!TextUtils.isEmpty(sensigFilteringRegexList.getString(0))) {
                sensig_filtering_regex_list = sensigFilteringRegexList.getString(0);
                sensig_filtering_regex_version = config.optInt(ConfigConst.SENSIG_FILTERING_REGEX_VERSION);
                TTUtil.setSensigInfo(TikTokBusinessSdk.getApplicationContext(), new Sensig(sensig_filtering_regex_version, sensig_filtering_regex_list));
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
