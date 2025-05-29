package com.tiktok.util;

import android.os.Handler;
import android.os.HandlerThread;

public class TTHandlerUtil {
    private Handler sHandler = null;
    private static final TTHandlerUtil instance = new TTHandlerUtil();

    private TTHandlerUtil() {
        try {
            HandlerThread thread = new HandlerThread("tiktok");
            thread.start();
            sHandler = new Handler(thread.getLooper());
        } catch (Throwable throwable) {

        }
    }

    public static TTHandlerUtil getInstance() {
        return instance;
    }

    public void post(Runnable runnable) {
        try {
            if (runnable == null || sHandler == null) {
                return;
            }
            sHandler.post(runnable);
        } catch (Throwable throwable) {

        }
    }

    public void postDelayed(Runnable runnable, long delayMillis) {
        try {
            if (runnable == null || sHandler == null) {
                return;
            }
            sHandler.postDelayed(runnable, delayMillis);
        } catch (Throwable throwable) {

        }
    }

    public void removeCallbacks(Runnable runnable) {
        try {
            if (runnable == null || sHandler == null) {
                return;
            }
            sHandler.removeCallbacks(runnable);
        } catch (Throwable throwable) {

        }
    }
}
