/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

public class TTConst {
    public static final String TTSDK_KEY_VALUE_STORE = "com.tiktok.sdk.keystore";
    public static final String TTSDK_APP_ANONYMOUS_ID = "com.tiktok.sdk.anonymousId";

    public static final String TTSDK_APP_FIRST_INSTALL = "com.tiktok.sdk.firstInstall";
    public static final String TTSDK_APP_LAST_LAUNCH = "com.tiktok.sdk.lastLaunch";
    public static final String TTSDK_APP_2DR_TIME = "com.tiktok.sdk.2drTime";

    public static final String TTSDK_PREFIX = "com.tiktok";

    public static enum ApiErrorCodes {
        PARTIAL_SUCCESS(20001),
        API_ERROR(40000);

        public Integer code;

        ApiErrorCodes(Integer code) {
            this.code = code;
        }
    }

    public static enum AutoEvents {
        InstallApp("InstallApp"),
        SecondDayRetention("2Dretention"),
        LaunchAPP("LaunchAPP");

        public String name;

        AutoEvents(String name) {
            this.name = name;
        }
    }
}