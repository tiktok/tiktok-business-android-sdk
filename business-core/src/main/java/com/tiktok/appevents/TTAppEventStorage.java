/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.content.Context;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

class TTAppEventStorage {
    private static final String TAG = TTAppEventStorage.class.getCanonicalName();

    private static final TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    private static final String EVENT_STORAGE_FILE = "events_cache";

    private static final int MAX_PERSIST_EVENTS_NUM = 500;

    /**
     * write events into file
     *
     * @param failedEvents if flush failed, failedEvents is not null
     */
    public synchronized static void persist(List<TTAppEvent> failedEvents) {
        TTUtil.checkThread(TAG);

        logger.debug("Tried to persist to disk");
        if (!TikTokBusinessSdk.isSystemActivated()) {
            logger.debug("Quit persisting to disk because global switch is turned off");
            return;
        }

        List<TTAppEvent> eventsFromMemory = TTAppEventsQueue.exportAllEvents();

        TTAppEventPersist eventsFromDisk = readFromDisk();

        if (eventsFromMemory.isEmpty() && eventsFromDisk.isEmpty() &&
                (failedEvents == null || failedEvents.isEmpty())) {
            return;
        }

        TTAppEventPersist toBeSaved = new TTAppEventPersist();
        // maintain events ordering, the events in the network should be earlier than the
        // events on the disk, finally come the events in the memory
        if (failedEvents != null) {
            toBeSaved.addEvents(failedEvents);
        }
        toBeSaved.addEvents(eventsFromDisk.getAppEvents());
        toBeSaved.addEvents(eventsFromMemory);

        //If end up persisting more than 10,000 events, persist the latest 10,000 events by timestamp
        discardOldEvents(toBeSaved, MAX_PERSIST_EVENTS_NUM);
        saveToDisk(toBeSaved);
    }

    /**
     * discard old events
     * In order not to overwhelm users' disk, only maxPersistNum is allowed to be persisted to disk
     *
     * @param ttAppEventPersist
     */
    private static void discardOldEvents(TTAppEventPersist ttAppEventPersist, int maxPersistNum) {
        if (ttAppEventPersist == null || ttAppEventPersist.isEmpty()) {
            return;
        }

        List<TTAppEvent> appEvents = ttAppEventPersist.getAppEvents();

        int size = appEvents.size();

        if (size > maxPersistNum) {
            logger.debug("Way too many events(%d), slim it!", size);
            TTAppEventLogger.totalDumped += size - maxPersistNum;
            TikTokBusinessSdk.diskListener.onDumped(TTAppEventLogger.totalDumped);
            ttAppEventPersist.setAppEvents(new ArrayList<>(appEvents.subList(size - maxPersistNum, size)));
        }
    }

    private static boolean saveToDisk(TTAppEventPersist appEventPersist) {
        if (appEventPersist.isEmpty()) {
            return false;
        }
        long initTimeMS = System.currentTimeMillis();
//        try {
//            JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
//                    .put("size", appEventPersist.getAppEvents().size());
//            TikTokBusinessSdk.getAppEventLogger().monitorMetric("file_w_start", meta, null);
//        } catch (Exception ignored) {}
        Context context = TikTokBusinessSdk.getApplicationContext();
        boolean success = false;
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(context.openFileOutput(EVENT_STORAGE_FILE, Context.MODE_PRIVATE)))) {
            oos.writeObject(appEventPersist);
            logger.debug("Saving %d events to disk", appEventPersist.getAppEvents().size());
            if (TikTokBusinessSdk.diskListener != null) {
                TikTokBusinessSdk.diskListener.onDiskChange(appEventPersist.getAppEvents().size(), false);
            }
            success = true;
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
        try {
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                    .put("latency", endTimeMS-initTimeMS)
                    .put("success", success)
                    .put("size", appEventPersist.getAppEvents().size());
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("file_w", meta, null);
        } catch (Exception ignored) {}
        return  success;
    }

    private static void deleteFile(File f) {
        if (f.exists()) {
            f.delete();
        }
    }

    synchronized static TTAppEventPersist readFromDisk() {
        long initTimeMS = System.currentTimeMillis();
        TTUtil.checkThread(TAG);

        Context context = TikTokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
        if (!f.exists()) {
            return new TTAppEventPersist();
        }

        TTAppEventPersist appEventPersist = new TTAppEventPersist();

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(context.openFileInput(EVENT_STORAGE_FILE)))) {
            appEventPersist = (TTAppEventPersist) ois.readObject();
            logger.debug("disk read data: %s", appEventPersist);
            deleteFile(f);
            if (TikTokBusinessSdk.diskListener != null) {
                TikTokBusinessSdk.diskListener.onDiskChange(0, true);
            }
        } catch (Exception e) {
            deleteFile(f);
            TTCrashHandler.handleCrash(TAG, e);
        }

        try {
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(endTimeMS)
                    .put("latency", endTimeMS-initTimeMS)
                    .put("size", appEventPersist.getAppEvents().size());
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("file_r", meta, null);
        } catch (Exception ignored) {}

        return appEventPersist;
    }

    public synchronized static void clearAll() {
        TTUtil.checkThread(TAG);

        Context context = TikTokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
        deleteFile(f);
        if (TikTokBusinessSdk.diskListener != null) {
            TikTokBusinessSdk.diskListener.onDiskChange(0, true);
        }
    }
}
