package com.example;

import androidx.multidex.MultiDexApplication;

import com.tiktok.TikTokBusinessSdk;

public class TTApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        TikTokBusinessSdk.registerEDPLifecycleCallback(this);
    }
}
