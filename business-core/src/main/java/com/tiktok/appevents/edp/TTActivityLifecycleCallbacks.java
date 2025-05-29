/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents.edp;

import static com.tiktok.appevents.edp.EDPConfig.enable_app_launch_track;
import static com.tiktok.appevents.edp.EDPConfig.enable_page_show_track;
import static com.tiktok.appevents.edp.EDPConfig.page_detail_upload_deep_count;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.TikTokBusinessSdk;

import java.lang.ref.WeakReference;

public class TTActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks{
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
        try {
            activity.get().getWindow().getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    if (enable_page_show_track) {
                        if(TTEDPEventTrack.pageShowIsSending){
                            return;
                        }
                        TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    TTEDPEventTrack.pageShowIsSending = true;
                                    TTEDPEventTrack.trackPageShow(activity.get().getClass().getSimpleName(), index, isBackground,
                                            TTHierarchyHelper.getViewHierarchy(new WeakReference<>(activity.get().getWindow().getDecorView()), page_detail_upload_deep_count),
                                            TTHierarchyHelper.getViewHierarchyCountAndRegisterOnTouch(new WeakReference<>(activity.get().getWindow().getDecorView()), activity));
                                }catch (Throwable e){

                                }
                            }
                        });
                    } else {
                        TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    TTHierarchyHelper.getViewHierarchyCountAndRegisterOnTouch(new WeakReference<>(activity.get().getWindow().getDecorView()), activity);
                                }catch (Throwable e){

                                }
                            }
                        });
                    }
                }
            });
        }catch (Throwable e){

        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if(activityWeakReference == null || activityWeakReference.get() == null || activityWeakReference.get() != activity){
            index++;
        }
        activityWeakReference = new WeakReference<>(activity);
        if (enable_app_launch_track && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && mIsBackground && activity.getReferrer() != null) {
            TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                @Override
                public void run() {
                    try{
                        TTEDPEventTrack.trackAppLaunch(activityWeakReference.get().getReferrer().toString(),
                                activityWeakReference.get().getIntent() != null && activityWeakReference.get().getIntent().getData() != null
                                        ? activityWeakReference.get().getIntent().getData().toString() : "");
                    }catch (Throwable e){

                    }
                }
            });
        }
        final boolean isBackground = mIsBackground;
        if(TikTokBusinessSdk.isInitialized()) {
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
