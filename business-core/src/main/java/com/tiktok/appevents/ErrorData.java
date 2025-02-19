/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents;

public class ErrorData {

    public static final int TT_DDL_CODE_NOT_INIT = -1;
    public static final int TT_DDL_CODE_HTTP_ERROR = -2;
    public static final String TT_DDL_MSG_NOT_INIT = "SDK not initialized";
    public static final String TT_DDL_MSG_HTTP_ERROR = "HTTP error";

    private int code;
    private String msg;

    public ErrorData(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
