/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.TikTokBusinessSdk.enableAutoIapTrack;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.iap.TTInAppPurchaseWrapper;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;
import org.json.JSONObject;

class TTActivityLifecycleCallbacksListener extends TTLifeCycleCallbacksAdapter {

    private final TTAppEventLogger appEventLogger;
    private static boolean isPaused = false;

    private long fgStart;
    private long bgStart = 0;

    public TTActivityLifecycleCallbacksListener(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
        this.fgStart = System.currentTimeMillis();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (isPaused) {
            reportBackground(bgStart);
            fgStart = System.currentTimeMillis();
            appEventLogger.fetchGlobalConfig(0);
            appEventLogger.restartScheduler();
            appEventLogger.autoEventsManager.track2DayRetentionEvent();
        }
        isPaused = false;
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        reportForeground(fgStart);
        bgStart = System.currentTimeMillis();
        appEventLogger.stopScheduler();
        isPaused = true;
        if(enableAutoIapTrack()) {
            TTInAppPurchaseWrapper.startBillingClient();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        appEventLogger.persistEvents();
        appEventLogger.persistMonitor();
    }

    // TODO might never be called as per Android's doc
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        appEventLogger.stopScheduler();
    }

    private void reportForeground(@NonNull long ts) {
        try {
            long latency = System.currentTimeMillis() - ts;
            JSONObject meta = TTUtil.getMetaWithTS(ts).put("latency", latency);
            appEventLogger.monitorMetric("foreground", meta, null);
        } catch (Exception ignored) {}
    }

    private void reportBackground(@NonNull long ts) {
        try {
            long latency = System.currentTimeMillis() - ts;
            JSONObject meta = TTUtil.getMetaWithTS(ts).put("latency", latency);
            appEventLogger.monitorMetric("background", meta, null);
        } catch (Exception ignored) {}
    }

    public static boolean isBackground(){
        return isPaused;
    }
}

