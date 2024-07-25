/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.init;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.ui.home.HomeViewModel;
import com.tiktok.TikTokBusinessSdk;

public class InitFragment extends Fragment {

    private InitViewModel initViewModel;
    private EditText appId;
    private EditText ttAppId;
    private Button init;
    private Button startTrack;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        initViewModel = new ViewModelProvider(this).get(InitViewModel.class);
        View root = inflater.inflate(R.layout.fragment_init, container, false);
        appId = root.findViewById(R.id.app_id);
        ttAppId = root.findViewById(R.id.tt_app_id);
        init = root.findViewById(R.id.init);
        startTrack = root.findViewById(R.id.startTrack);
        ((Switch)(root.findViewById(R.id.autostart_status))).setChecked(InitViewModel.autoStart);
        ((Switch)(root.findViewById(R.id.auto_events_status))).setChecked(InitViewModel.autoEvent);
        ((Switch)(root.findViewById(R.id.install_logging_status))).setChecked(InitViewModel.loggingStatus);
        ((Switch)(root.findViewById(R.id.launch_logging_status))).setChecked(InitViewModel.launchStatus);
        ((Switch)(root.findViewById(R.id.retention_logging_status))).setChecked(InitViewModel.retentionStatus);
        ((Switch)(root.findViewById(R.id.id_collection_status))).setChecked(InitViewModel.advertiserIDCollectionEnable);
        ((Switch)(root.findViewById(R.id.monitor_status))).setChecked(InitViewModel.Metrics);
        ((Switch)(root.findViewById(R.id.debug_status))).setChecked(InitViewModel.debugModeSwitch);
        ((Switch)(root.findViewById(R.id.limited_status))).setChecked(InitViewModel.lduModeSwitch);
        ((Switch)(root.findViewById(R.id.iap_status))).setChecked(InitViewModel.autoIapTrack);
        ((Switch)(root.findViewById(R.id.init_with_callback))).setChecked(InitViewModel.initWithCallback);
        init.setOnClickListener(v -> {
            HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

            if (savedInstanceState == null) {
                // !!!!!!!!!!!!!!!!!!!!!!!!!
                // in order for this app to be runnable, plz create a resource file containing the relevant string resources
                // Tiktok sdk init start
                TikTokBusinessSdk.LogLevel logLevel = TikTokBusinessSdk.LogLevel.DEBUG;
                TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(getActivity().getApplicationContext());
                try{
                    int t = Integer.parseInt(String.valueOf(((EditText)(root.findViewById(R.id.level_label_et))).getText()));
                    switch (t){
                        case 0:
                            logLevel = TikTokBusinessSdk.LogLevel.NONE;
                            break;
                        case 1:
                            logLevel = TikTokBusinessSdk.LogLevel.INFO;
                            break;
                        case 2:
                            logLevel = TikTokBusinessSdk.LogLevel.WARN;
                            break;
                        case 3:
                            logLevel = TikTokBusinessSdk.LogLevel.DEBUG;
                            break;

                    }
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }finally {
                    ttConfig.setLogLevel(logLevel);
                }
                try{
                    int t = Integer.parseInt(String.valueOf(((EditText)(root.findViewById(R.id.flush_time_et))).getText()));
                    ttConfig.setFlushTimeInterval(t);
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }
                if(!((Switch)(root.findViewById(R.id.autostart_status))).isChecked()){
                    ttConfig.disableAutoStart();
                    InitViewModel.autoStart = false;
                }else {
                    InitViewModel.autoStart = true;
                }
                if(!((Switch)(root.findViewById(R.id.auto_events_status))).isChecked()){
                    ttConfig.disableAutoEvents();
                    InitViewModel.autoEvent = false;
                }else {
                    InitViewModel.autoEvent = true;
                }
                if(!((Switch)(root.findViewById(R.id.install_logging_status))).isChecked()){
                    ttConfig.disableInstallLogging();
                    InitViewModel.loggingStatus = false;
                }else {
                    InitViewModel.loggingStatus = true;
                }
                if(!((Switch)(root.findViewById(R.id.launch_logging_status))).isChecked()){
                    ttConfig.disableLaunchLogging();
                    InitViewModel.launchStatus = false;
                }else {
                    InitViewModel.launchStatus = true;
                }
                if(!((Switch)(root.findViewById(R.id.retention_logging_status))).isChecked()){
                    ttConfig.disableRetentionLogging();
                    InitViewModel.retentionStatus = false;
                }else {
                    InitViewModel.retentionStatus = true;
                }
                if(!((Switch)(root.findViewById(R.id.id_collection_status))).isChecked()){
                    ttConfig.disableAdvertiserIDCollection();
                    InitViewModel.advertiserIDCollectionEnable = false;
                }else {
                    InitViewModel.advertiserIDCollectionEnable = true;
                }
                if(!((Switch)(root.findViewById(R.id.monitor_status))).isChecked()){
                    ttConfig.disableMonitor();
                    InitViewModel.Metrics = false;
                }else {
                    InitViewModel.Metrics = true;
                }
                if(((Switch)(root.findViewById(R.id.debug_status))).isChecked()){
                    ttConfig.openDebugMode();
                    InitViewModel.debugModeSwitch = true;
                } else {
                    InitViewModel.debugModeSwitch = false;
                }
                if(((Switch)(root.findViewById(R.id.limited_status))).isChecked()){
                    ttConfig.enableLimitedDataUse();
                    InitViewModel.lduModeSwitch = true;
                } else {
                    InitViewModel.lduModeSwitch = false;
                }
                if(((Switch)(root.findViewById(R.id.iap_status))).isChecked()){
                    ttConfig.enableAutoIapTrack();
                    InitViewModel.autoIapTrack = true;
                }else {
                    InitViewModel.autoIapTrack = false;
                }
                if(((Switch)(root.findViewById(R.id.init_with_callback))).isChecked()){
                    InitViewModel.initWithCallback = true;
                }else {
                    InitViewModel.initWithCallback = false;
                }
                if(appId.getText().toString() == null || appId.getText().toString().isEmpty()){
                    ttConfig.setAppId("com.tiktok.iabtest");
                } else {
                    ttConfig.setAppId(appId.getText().toString());
                }
                if(ttAppId.getText().toString() == null || ttAppId.getText().toString().isEmpty()){
                    ttConfig.setTTAppId("123456");
                } else {
                    ttConfig.setTTAppId(ttAppId.getText().toString());
                }
                Log.e("Initialization state", TikTokBusinessSdk.isInitialized()+"");
                if(InitViewModel.initWithCallback){
                    TikTokBusinessSdk.initializeSdk(ttConfig, new TikTokBusinessSdk.TTInitCallback() {
                        @Override
                        public void success() {
                            WebSettings.getDefaultUserAgent(TikTokBusinessSdk.getApplicationContext());
                            TikTokBusinessSdk.trackEvent("test");
                        }

                        @Override
                        public void fail(int code, String msg) {
                            Log.e("init error", "code:"+code+", msg:"+msg);
                        }
                    });
                }else {
                    TikTokBusinessSdk.initializeSdk(ttConfig);
                }
                Log.e("Initialization state", TikTokBusinessSdk.isInitialized()+"");
                // check if user info is cached & init
                // homeViewModel.checkInitTTAM();

                TikTokBusinessSdk.setOnCrashListener((thread, ex) -> android.util.Log.i("TikTokBusinessSdk", "setOnCrashListener" + thread.getName(), ex));

                // testing delay tracking, implementing a 6 sec delay manually
                // ideally has to be after accepting tracking permission
                if(InitViewModel.autoStart) {
                    new Handler(Looper.getMainLooper()).postDelayed(TikTokBusinessSdk::startTrack, 10000);
                }
            }
        });
        startTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TikTokBusinessSdk.startTrack();
            }
        });
        return root;
    }

}