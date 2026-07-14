/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.util.TTConst.TTSDK_PREFIX;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.IOUtils;
import com.tiktok.util.JSON;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A global crash handler which mainly does
 * 1. send error details to a remote analytic tool
 * 2. Prevent from app from crash
 */
public class TTCrashHandler {
    private static final String TAG = "TTCrashHandler";
    private static final TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    private static final String CRASH_REPORT_FILE = "tt_crash_log";

    private static final int MONITOR_RETRY_LIMIT = 2;
    private static final int MONITOR_BATCH_MAX = 5;

    static volatile TTCrashReport crashReport = new TTCrashReport();

    public static void handleCrash(String originTag, Throwable ex, int type) {
        if (ex != null) {
            if (TextUtils.isEmpty(originTag)) {
                originTag = "";
            }
            ttLogger.error(ex, "Error caused by sdk at " + originTag + "\n" + ex.getMessage());
            persistException(ex, type);
        }
    }

    public static void retryLater(JSONObject monitor) {
        try {
            if (crashReport != null) {
                crashReport.addReport(monitor.toString(), System.currentTimeMillis(), 0);
            }
        } catch (Throwable ignore) {
        }
    }

    public static void persistToFile() {
        try {
            if (crashReport != null && !crashReport.reports.isEmpty()) {
                saveToFile(crashReport);
                crashReport = new TTCrashReport();
            }
        } catch (Throwable ignore) {
        }
    }

    public static void initCrashReporter() {
        // read any from file if exists
        try {
            TTCrashReport fileReport = readFromFile();
            if (fileReport != null && fileReport.reports != null) {
                crashReport.reports.addAll(fileReport.reports);
            }

            try {
                Context context = TikTokBusinessSdk.getApplicationContext();
                if (context != null) {
                    File f = new File(context.getFilesDir(), CRASH_REPORT_FILE);
                    if (f.exists()) f.delete();
                }
            } catch (Throwable ignored) {
            }

            TTCrashReport failed = reportMonitor(crashReport);
            saveToFile(failed);
            crashReport = new TTCrashReport();
        } catch (Throwable ignore) {
        }
    }

    private static TTCrashReport reportMonitor(@NonNull TTCrashReport cr) {
        if (cr.reports == null || cr.reports.isEmpty()) return cr;

        TTCrashReport failedReport = new TTCrashReport();
        try {
            List<String> duplicate = new ArrayList<>();
            // batch send monitor events
            for (int i = 0; i < cr.reports.size(); i += MONITOR_BATCH_MAX) {
                try {
                    int j = i + MONITOR_BATCH_MAX;
                    if (j > cr.reports.size()) j = cr.reports.size();
                    List<TTCrashReport.Monitor> batch = cr.reports.subList(i, j);
                    JSONArray batchReq = JSON.buildArr();
                    for (TTCrashReport.Monitor m : batch) {
                        try {
                            String data = m.monitor;
                            if (TextUtils.isEmpty(data) || duplicate.contains(data)) {
                                continue;
                            }
                            duplicate.add(data);
                            JSONObject js = JSON.build(data);
                            if (js != null && js.length() > 0) {
                                JSON.putArr(batchReq, js);
                            }
                        } catch (Throwable ignored) {
                        }
                    }

                    if (batchReq.length() > 0) {
                        JSONObject req = TTRequestBuilder.getBasePayloadWithTs();
                        JSON.putObject(req, "batch", batchReq);

                        HttpRequestUtil.HttpResponse resp = TTRequest.reportMonitorEvent(req);
                        if (resp == null || !resp.isOK()) {
                            for (TTCrashReport.Monitor o : batch) {
                                failedReport.addReport(o.monitor, System.currentTimeMillis(), o.attempt + 1);
                            }
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        return failedReport;
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

        List<Monitor> reports = new CopyOnWriteArrayList<>();

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
            JSON.putObject(stat, "monitor", monitor);
            crashReport.addReport(stat.toString(), System.currentTimeMillis(), 0);
            saveToFile(crashReport);
            crashReport = new TTCrashReport();
        } catch (Throwable ignore) {
            // exception during saving exception to file, post direct
            if (stat != null && stat.has("monitor")) {
                JSONArray batchReq = JSON.buildArr();
                JSON.putArr(batchReq, stat);

                JSONObject req = TTRequestBuilder.getBasePayloadWithTs();
                JSON.putObject(req, "batch", batchReq);

                TTRequest.reportMonitorEvent(req);
            }
        }
    }

    private static void saveToFile(TTCrashReport cr) {
        if (cr == null || cr.reports == null || cr.reports.isEmpty()) {
            return;
        }

        FileOutputStream fos = null;
        ObjectOutputStream os = null;
        try {
            Context context = TikTokBusinessSdk.getApplicationContext();
            if (context != null) {
                fos = context.openFileOutput(CRASH_REPORT_FILE, Context.MODE_PRIVATE);
                os = new ObjectOutputStream(fos);
                os.writeObject(cr);
            }
        } catch (Throwable e) {
            // save failed, report instant if possible
            reportMonitor(cr);
        } finally {
            IOUtils.close(fos, os);
        }
    }

    private static TTCrashReport readFromFile() {
        Context context = TikTokBusinessSdk.getApplicationContext();
        if (context == null) {
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = context.openFileInput(CRASH_REPORT_FILE);
            return TTSafeReadObjectUtil.safeReadTTCrashHandler(fis);
        } catch (Throwable ignored) {
        } finally {
            IOUtils.close(fis);
        }
        return null;
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
        if (elts == null || elts.length < 1) return false;
        for (StackTraceElement element : elts) {
            if (element != null && element.getClassName().startsWith(TTSDK_PREFIX)) {
                return true;
            }
        }
        return false;
    }
}