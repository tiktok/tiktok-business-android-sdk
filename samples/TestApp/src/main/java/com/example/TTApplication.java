package com.example;

import android.app.Application;

import com.tiktok.TikTokBusinessSdk;

public class TTApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TikTokBusinessSdk.registerEDPLifecycleCallback(this);
    }
}
