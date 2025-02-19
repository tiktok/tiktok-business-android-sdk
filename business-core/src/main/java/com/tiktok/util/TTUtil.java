/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTCrashHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.tiktok.util.TTConst.TTSDK_APP_ANONYMOUS_ID;
import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

public class TTUtil {
    private static final String TAG = TTUtil.class.getName();
    private static final TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    /**
     * All internal operations should be pushed to the internal {@link com.tiktok.appevents.TTAppEventLogger#eventLoop}
     * and run in a non-main thread
     *
     * @param tag
     */
    public static void checkThread(String tag) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            TTCrashHandler.handleCrash(tag, new IllegalStateException("Current method should be called in a non-main thread"), TTSDK_EXCEPTION_SDK_CATCH);
        }
    }

    /**
     * pretty print str
     *
     * @param o
     * @return
     */
    public static String ppStr(JSONObject o) {
        if (o == null) {
            return "null";
        }
        try {
            return o.toString(4);
        } catch (JSONException e) {
            return "";
        }
    }

    public static String ppStr(String str) {
        try {
            return ppStr(new JSONObject(str));
        } catch (JSONException e) {
            return "";
        }
    }

    public static String getOrGenAnoId(Context context, boolean forceGenerate) {
        TTKeyValueStore store = new TTKeyValueStore(context);
        String anoId = store.get(TTSDK_APP_ANONYMOUS_ID);
        if (TextUtils.isEmpty(anoId) || forceGenerate) {
            // TODO make sure anoId universally unique, also limit the length.
            anoId = UUID.randomUUID().toString();
            store.set(TTSDK_APP_ANONYMOUS_ID, anoId);
            logger.info("AnonymousId reset to " + anoId);
        }
        return anoId;
    }

    public static JSONObject getMetaWithTS(@Nullable Long ts) {
        if (ts == null) {
            ts = System.currentTimeMillis();
        }
        try {
            return new JSONObject().put("ts", ts);
        } catch (Exception ignored) {}
        return new JSONObject();
    }

    public static JSONObject getMonitorException(@Nullable Throwable ex, @Nullable Long ts, int type) {
        JSONObject monitor = new JSONObject();
        try {
            monitor.put("type", "exception");
            monitor.put("name", "exception");
            monitor.put("meta", getMetaException(ex, ts, type));
            monitor.put("extra", null);
        } catch (Exception ignored) {}
        return monitor;
    }

    public static JSONObject getMetaException(@Nullable Throwable ex, @Nullable Long ts, int type) {
        JSONObject meta = getMetaWithTS(ts);
        try {
            if (ex != null) {
                Throwable rootCause = ex;
                while(rootCause.getCause() != null && rootCause.getCause() != rootCause)
                    rootCause = rootCause.getCause();
                meta.put("ex_class", rootCause.getStackTrace()[0].getClassName());
                meta.put("ex_method", rootCause.getStackTrace()[0].getMethodName());
                String argMsg = rootCause.getStackTrace()[0].getFileName() +
                        " " + rootCause.getStackTrace()[0].getLineNumber();
                meta.put("ex_args", argMsg);
                meta.put("ex_msg", rootCause.getMessage());
                meta.put("ex_type", type);
                final int stackLimit = 15;
                String[] st = new String[stackLimit];
                for(int i = 0; i < stackLimit; i++) {
                    if (rootCause.getStackTrace()[i] != null)
                        st[i] = rootCause.getStackTrace()[i].toString();
                }
                meta.put("ex_stack", Arrays.toString(st));
                meta.put("success", false);
            } else {
                meta.put("success", true);
            }
        } catch (Exception ignored) {}
        return meta;
    }
}
