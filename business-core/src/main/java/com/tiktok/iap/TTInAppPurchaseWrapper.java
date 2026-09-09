/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.iap;

import android.app.Activity;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.iap.billing.client.IBillingProxy;
import com.tiktok.iap.billing.client.TTBillingFactory;
import com.tiktok.util.JSON;
import com.tiktok.util.TTSafeRunnable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTInAppPurchaseWrapper {
    public static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();

    public static volatile int devAutoTrack = 0;//default0、1open、2close

    public static volatile boolean hasReportedHistoryInLife = false;
    public static volatile boolean autoTrackPaymentEnable = true;
    public static Set<Integer> autoTrackPaymentTypes = new CopyOnWriteArraySet<>();
    public static volatile boolean autoTrackPaymentJson = true;
    public static volatile boolean autoTrackPaymentHistory = true;
    public static volatile int autoTrackPaymentHistoryINAPP = 200;
    public static volatile int autoTrackPaymentHistorySUBS = 20;

    private static volatile IBillingProxy sBillingProxy;

    private static final String ACT_BILLING = "ProxyBillingActivity";
    private static volatile String sPreviousActivity;

    static {
        autoTrackPaymentTypes.add(1); //INAPP
        autoTrackPaymentTypes.add(2); //SUBS
    }

    public static void registerIapTrack() {
        if (!TikTokBusinessSdk.isSdkEnabled()) {
            return;
        }
        try {
            if (autoTrackPaymentEnable) {
                sExecutor.submit(new TTSafeRunnable() {
                    @Override
                    public void doSafeRun() {
                        final IBillingProxy proxy = getBillingProxy();
                        if (proxy != null) {
                            proxy.init();
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
        }
    }

    public static void tryReportIapEvent(Activity activity) {
        if (!TikTokBusinessSdk.isSdkEnabled()) {
            return;
        }
        try {
            if (activity == null) {
                return;
            }
            if (autoTrackPaymentEnable && autoTrackPaymentHistory) {
                final String actName = activity.getClass().getSimpleName();
                final boolean actIsBill = sPreviousActivity != null && sPreviousActivity.contains(ACT_BILLING)
                        && !actName.contains(ACT_BILLING);
                if (!hasReportedHistoryInLife || actIsBill) {
                    sExecutor.submit(new TTSafeRunnable() {
                        @Override
                        public void doSafeRun() {
                            final IBillingProxy proxy = getBillingProxy();
                            if (proxy != null) {
                                proxy.queryPurchaseHistory();
                            }
                        }
                    });
                }

                sPreviousActivity = actName;
            }
        } catch (Throwable ignored) {
        }
    }

    public static void updateConfig(JSONObject config) {
        try {
            if (config == null) {
                return;
            }

            autoTrackPaymentEnable = JSON.getBoolean(config, "auto_track_Payment_enable", true);
            autoTrackPaymentJson = autoTrackPaymentEnable && JSON.getInt(config, "auto_track_Payment_json", 1) == 1;
            autoTrackPaymentHistory = autoTrackPaymentEnable && JSON.getInt(config, "auto_track_Payment_history_enable", 1) == 1;
            autoTrackPaymentHistoryINAPP = JSON.getInt(config, "auto_track_Payment_history_inapp_size", 200);
            autoTrackPaymentHistorySUBS = JSON.getInt(config, "auto_track_Payment_history_subs_size", 20);

            JSONArray types = JSON.getJsonArray(config, "auto_track_Payment_types");
            if (types != null) {
                autoTrackPaymentTypes.clear();
                int count = types.length();
                for (int i = 0; i < count; i++) {
                    try {
                        int type = types.optInt(i, -2);
                        if (type > 0) {
                            autoTrackPaymentTypes.add(type);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        //register iap track
        registerIapTrack();
    }

    public static boolean canTrackINAPP() {
        return autoTrackPaymentEnable && autoTrackPaymentTypes.contains(1);
    }

    public static boolean canTrackSUBS() {
        return autoTrackPaymentEnable && autoTrackPaymentTypes.contains(2);
    }

    private static IBillingProxy getBillingProxy() {
        if (sBillingProxy == null) {
            synchronized (TTInAppPurchaseWrapper.class) {
                if (sBillingProxy == null) {
                    sBillingProxy = TTBillingFactory.createBillingProxy();
                }
            }
        }
        return sBillingProxy;
    }

}
