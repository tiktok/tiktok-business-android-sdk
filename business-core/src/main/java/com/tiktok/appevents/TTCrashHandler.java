/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.tiktok.util.TTConst.TTSDK_PREFIX;

/**
 * A global crash handler which mainly does
 * 1. send error details to a remote analytic tool
 * 2. Prevent from app from crash
 */
public class TTCrashHandler {
    private static final String TAG = TTCrashHandler.class.getCanonicalName();
    private static final TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    private static final String CRASH_REPORT_FILE = "tt_crash_log";

    private static final int MONITOR_RETRY_LIMIT = 2;
    private static final int MONITOR_BATCH_MAX = 5;

    static TTCrashReport crashReport = new TTCrashReport();

    public static void handleCrash(String originTag, Throwable ex, int type) {
        ttLogger.error(ex, "Error caused by sdk at " + originTag +
                "\n" + ex.getMessage() + "\n" + getStackTrace(ex));
        persistException(ex, type);
    }

    public static void retryLater(JSONObject monitor) {
        crashReport.addReport(monitor.toString(), System.currentTimeMillis(), 0);
    }

    public static void persistToFile() {
        for (TTCrashReport.Monitor m : crashReport.reports) {
            ttLogger.info("persistToFile %s", m.monitor);
        }
        saveToFile(crashReport);
        crashReport = new TTCrashReport();
    }

    public static void initCrashReporter() {
        // read any from file if exists
        TTCrashReport fileReport = readFromFile();
        if (fileReport != null) {
            crashReport.reports.addAll(fileReport.reports);
            try {
                Context context = TikTokBusinessSdk.getApplicationContext();
                File f = new File(context.getFilesDir(), CRASH_REPORT_FILE);
                if (f.exists()) f.delete();
            } catch (Exception ignored) {}
        }
        crashReport = reportMonitor(crashReport);
        saveToFile(crashReport);
        crashReport = new TTCrashReport();
    }

    private static TTCrashReport reportMonitor(@NonNull TTCrashReport cr) {
        if (cr.reports.size() == 0) return cr;
        TTCrashReport ttCrashReport = new TTCrashReport();
        // batch send monitor events
        for (int i = 0; i < cr.reports.size(); i += MONITOR_BATCH_MAX) {
            int j = i + MONITOR_BATCH_MAX;
            if (j > cr.reports.size()) j = cr.reports.size();
            List<TTCrashReport.Monitor> batch = cr.reports.subList(i, j);
            List<JSONObject> batchReq = new ArrayList<>();
            for (TTCrashReport.Monitor m : batch) {
                try {
                    batchReq.add(new JSONObject(m.monitor));
                } catch (Exception ignored) {}
            }
            JSONObject req = TTRequestBuilder.getBasePayloadWithTs();
            try {
                req.put("batch", new JSONArray(batchReq));
            } catch (Exception ignored) {}
            String resp = TTRequest.reportMonitorEvent(req);
            if (HttpRequestUtil.getCodeFromApi(resp) != 0) {
                for (TTCrashReport.Monitor o : batch) {
                    ttCrashReport.addReport(o.monitor, System.currentTimeMillis(), o.attempt+1);
                }
            }
        }
        return ttCrashReport;
    }

    static class TTCrashReport implements Serializable {
        static class Monitor implements Serializable {
            public final String monitor;
            public long ts;
            public int attempt;
            public Monitor(String o, long t, int a) {
                this.monitor = o;
                this.ts = t;
                this.attempt = a;
            }
        }
        List<Monitor> reports = new ArrayList<>();
        public void addReport(String o, long t, int a) {
            if (a < MONITOR_RETRY_LIMIT)
                this.reports.add(new Monitor(o, t, a));
        }
    }

    private static void persistException(Throwable ex, int type) {
        JSONObject stat = null;
        try {
            stat = TTRequestBuilder.getHealthMonitorBase();
            JSONObject monitor = TTUtil.getMonitorException(ex, null, type);
            stat.put("monitor", monitor);
            crashReport.addReport(stat.toString(), System.currentTimeMillis(), 0);
            saveToFile(crashReport);
            crashReport = new TTCrashReport();
        } catch (Exception e) {
            // exception during saving exception to file, post direct
            if (stat != null) {
                List<JSONObject> batchReq = new ArrayList<>();
                batchReq.add(stat);
                JSONObject req = TTRequestBuilder.getBasePayloadWithTs();
                try {
                    req.put("batch", new JSONArray(batchReq));
                } catch (Exception ignored) {}
                TTRequest.reportMonitorEvent(req);
            }
        }
    }

    private static void saveToFile(TTCrashReport cr) {
        try {
            Context context = TikTokBusinessSdk.getApplicationContext();
            FileOutputStream fos = context.openFileOutput(CRASH_REPORT_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(cr);
            os.close();
            fos.close();
        } catch (Throwable e) {
            // save failed, report instant if possible
            reportMonitor(cr);
        }
    }

    private static TTCrashReport readFromFile() {
        TTCrashReport meta = new TTCrashReport();
        Context context = TikTokBusinessSdk.getApplicationContext();
        try {
            FileInputStream fis = context.openFileInput(CRASH_REPORT_FILE);
            meta = TTSafeReadObjectUtil.safeReadTTCrashHandler(fis);
            fis.close();
        } catch (Exception ignored) {}
        return meta;
    }

    private static String getStackTrace(Throwable t) {
        StringBuilder buffer = new StringBuilder();
        for (StackTraceElement curr : t.getStackTrace()) {
            buffer.append(curr.toString()).append("\n");
        }
        return buffer.toString();
    }

    public static boolean isTTSDKRelatedException(Throwable e) {
        if (e == null) return false;
        // loop check cause
        Throwable prev = null;
        Throwable t = e;
        while (t != null && t != prev) {
            if (isTTSDKRelatedException(t.getStackTrace())) return true;
            prev = t;
            t = t.getCause();
        }
        return false;
    }

    public static boolean isTTSDKRelatedException(StackTraceElement[] elts) {
        if (elts == null) return false;
        for (StackTraceElement element : elts) {
            if (element.getClassName().startsWith(TTSDK_PREFIX)) {
                return true;
            }
        }
        return false;
    }
}