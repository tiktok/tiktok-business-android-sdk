/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class TTAppEventsQueue {

    private static final String TAG = "TTAppEventsQueue";
    private static volatile List<TTAppEvent> memory = new CopyOnWriteArrayList<>();

    private TTAppEventsQueue() {
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
        if (TikTokBusinessSdk.shouldDiscardEvents()) {
            return;
        }
        TTUtil.checkThread(TAG);
        memory.add(event);
        notifyChange();
    }

    public static synchronized int size() {
        return memory.size();
    }

    public static synchronized void clearAll() {
        TTUtil.checkThread(TAG);
        memory = new CopyOnWriteArrayList<>();
        notifyChange();
    }

    public static synchronized List<TTAppEvent> exportAllEvents() {
        List<TTAppEvent> appEvents = memory;
        memory = new CopyOnWriteArrayList<>();
        notifyChange();
        return appEvents;
    }

}
