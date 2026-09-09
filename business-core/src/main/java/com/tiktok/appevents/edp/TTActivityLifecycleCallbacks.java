/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents.edp;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.iap.TTInAppPurchaseWrapper;

import java.lang.ref.WeakReference;

public class TTActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private boolean mIsBackground = true;

    private int mRefCount = 0;

    private int index = 0;

    public boolean hasSendPageShow = false;

    private WeakReference<Activity> activityWeakReference;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    public void registerFirstActivity() {
        if (activityWeakReference != null && activityWeakReference.get() != null && !hasSendPageShow) {
            registerEDPListener(activityWeakReference, index, mIsBackground);
        }
    }

    private void registerEDPListener(WeakReference<Activity> activity, int index, boolean isBackground) {
        if (!EDPConfig.enable_sdk || !TikTokBusinessSdk.isSdkEnabled()) {
            return;
        }
        try {
            Activity act = activity.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) {
                return;
            }
            Window window = act.getWindow();
            if (window == null) {
                return;
            }

            View decorView = window.getDecorView();
            decorView.post(new Runnable() {
                @Override
                public void run() {
                    //double check
                    if (act == null || act.isFinishing() || act.isDestroyed()) {
                        return;
                    }

                    try {
                        if (EDPConfig.enable_sdk && EDPConfig.enable_page_show_track) {
                            if (TTEDPEventTrack.pageShowIsSending) {
                                return;
                            }
                            TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                                @Override
                                public void run() {
                                    if (TTEDPEventTrack.pageShowIsSending) {
                                        return;
                                    }
                                    try {
                                        TTEDPEventTrack.pageShowIsSending = true;
                                        TTEDPEventTrack.trackPageShow(act.getClass().getSimpleName(), index, isBackground,
                                                TTHierarchyHelper.getViewHierarchy(new WeakReference<>(decorView), EDPConfig.page_detail_upload_deep_count),
                                                TTHierarchyHelper.getViewHierarchyCountAndRegisterOnTouch(new WeakReference<>(decorView), activity));
                                        TTEDPEventTrack.pageShowIsSending = false;
                                    } catch (Throwable ignore) {
                                    }
                                }
                            });
                        }
                    } catch (Throwable ignore) {
                    }

                    try {
                        if (EDPConfig.enable_sdk && EDPConfig.enable_click_track) {
                            TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        TTHierarchyHelper.getViewHierarchyCountAndRegisterOnTouch(new WeakReference<>(decorView), activity);
                                    } catch (Throwable ignore) {
                                    }
                                }
                            });
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activityWeakReference == null || activityWeakReference.get() == null || activityWeakReference.get() != activity) {
            index++;
        }
        activityWeakReference = new WeakReference<>(activity);

        TTInAppPurchaseWrapper.tryReportIapEvent(activity);

        if (EDPConfig.enable_sdk && TikTokBusinessSdk.isSdkEnabled() && EDPConfig.enable_app_launch_track && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && mIsBackground) {
            try {
                final Uri refer = activity.getReferrer();
                if (refer != null) {
                    TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                        @Override
                        public void run() {
                            if (EDPConfig.enable_sdk && EDPConfig.enable_app_launch_track) {
                                try {
                                    String data = "";
                                    if (activityWeakReference != null) {
                                        try {
                                            final Activity act = activityWeakReference.get();
                                            if (act != null) {
                                                data = act.getIntent().getData().toString();
                                            }
                                        } catch (Throwable ignore) {
                                        }
                                    }

                                    if (refer != null) {
                                        TTEDPEventTrack.trackAppLaunch(refer.toString(), data);
                                    }
                                } catch (Throwable ignore) {
                                }
                            }
                        }
                    });
                }
            } catch (Throwable ignore) {
            }
        }

        final boolean isBackground = mIsBackground;
        if (TikTokBusinessSdk.isInitialized()) {
            hasSendPageShow = true;
            registerEDPListener(activityWeakReference, index, isBackground);
        }
        mRefCount++;
        mIsBackground = false;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mRefCount--;
        if (mRefCount <= 0) {
            mRefCount = 0;
            mIsBackground = true;
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
