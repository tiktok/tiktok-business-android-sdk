/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import androidx.annotation.NonNull;
import com.tiktok.TikTokBusinessSdk;

import java.util.concurrent.ThreadFactory;

public class TTThreadFactory implements ThreadFactory {
    static final String TAG = TTInAppPurchaseManager.class.getCanonicalName();

    @Override
    public Thread newThread(Runnable r) {
        final Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                TTCrashHandler.handleCrash(TAG, throwable);
                if (TikTokBusinessSdk.getCrashListener() != null) {
                    TikTokBusinessSdk.getCrashListener().onCrash(thread, throwable);
                }
            }
        });
        return t;
    }
}
