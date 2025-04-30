/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTUtil;

import java.util.ArrayList;
import java.util.List;

class TTEdpAppEventsQueue {

    private static String TAG = TTEdpAppEventsQueue.class.getCanonicalName();
    private static List<TTAppEvent> memory = new ArrayList<>();

    private TTEdpAppEventsQueue() {
    }

    private static void notifyChange() {
        if (TikTokBusinessSdk.memoryListener != null) {
            TikTokBusinessSdk.memoryListener.onMemoryChange(memory.size());
        }

        if (TikTokBusinessSdk.nextTimeFlushListener != null) {
            int left = TTAppEventLogger.THRESHOLD - size();
            TikTokBusinessSdk.nextTimeFlushListener.thresholdLeft(TTAppEventLogger.THRESHOLD, Math.max(left, 0));
        }
    }

    public static synchronized void addEvent(TTAppEvent event) {
        TTUtil.checkThread(TAG);
        memory.add(event);
        notifyChange();
    }

    public static synchronized int size() {
        return memory.size();
    }

    public static synchronized void clearAll() {
        try {
            TTUtil.checkThread(TAG);
            memory = new ArrayList<>();
            notifyChange();
        }catch (Throwable e){

        }
    }

    public static synchronized List<TTAppEvent> exportAllEvents() {
        List<TTAppEvent> appEvents = memory;
        memory = new ArrayList<>();
        notifyChange();
        return appEvents;
    }

}
