/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import android.util.Log;

import com.tiktok.TikTokBusinessSdk.LogLevel;

/**
 * Logger util class
 */
public class TTLogger {

    /**
     * loglevel
     */
    public final LogLevel logLevel;
    /**
     * log TAG
     */
    private final String tag;

    public TTLogger(String tag, LogLevel logLevel) {
        this.tag = tag;
        this.logLevel = logLevel;
    }

    private String resolvedStr(String format, Object... extra) {
        if (format == null) {
            return "null";
        }
        return extra.length == 0 ? format : String.format(format, extra);

    }

    public void info(String format, Object... extra) {
        if (shouldLog(LogLevel.INFO)) {
            String str = resolvedStr(format, extra);
            if (str.length() > 1000) {
                Log.i(tag, str.substring(0, 1000));
                info(str.substring(1000));
            } else {
                Log.i(tag, str); // continuation
            }
        }
    }

    public void warn(String format, Object... extra) {
        if (shouldLog(LogLevel.WARN)) {
            Log.w(tag, resolvedStr(format, extra));
        }
    }

    public void debug(String format, Object... extra) {
        if (shouldLog(LogLevel.DEBUG)) {
            String str = resolvedStr(format, extra);
            if (str.length() > 1000) {
                Log.d(tag, str.substring(0, 1000));
                debug(str.substring(1000));
            } else {
                Log.d(tag, str); // continuation
            }
        }
    }

    public void error(Throwable error, String format, Object... extra) {
        if (shouldLog(LogLevel.INFO)) {
            Log.e(tag, resolvedStr(format, extra), error);
        }
    }

    private boolean shouldLog(LogLevel level) {
        return logLevel.ordinal() >= level.ordinal();
    }
}
