/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok;

import android.content.Context;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.tiktok.util.TTConst.TTSDK_APP_FIRST_INSTALL;
import static com.tiktok.util.TTConst.TTSDK_KEY_VALUE_STORE;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class TikTokBusinessSdkTest {
    private static final String TAG = TikTokBusinessSdkTest.class.getCanonicalName();

    @Test
    public void quickSdkInit() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        String appId = appContext.getResources().getString(R.string.tiktok_business_app_id);
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .setAppId(appId);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // check singleton is set
        assertTrue(TikTokBusinessSdk.isInitialized());

        Bundle metaData = null;
        try {
            metaData = appContext.getPackageManager().getApplicationInfo(
                    appContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (metaData == null) {
            fail("Bundle metadata null");
            return;
        }

        // check app id from meta data
        assertEquals(TikTokBusinessSdk.getAppId(), metaData.get("com.tiktok.sdk.AppId").toString());
    }

    @Test
    public void customSdkInit() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        String newAppID = "newAppID";
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .setAppId(newAppID)
                .disableAutoStart();
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // check singleton is set
        assertTrue(TikTokBusinessSdk.isInitialized());

        // check app id from meta data
        assertEquals(TikTokBusinessSdk.getAppId(), newAppID);
        // check auto event flag
        assertFalse(TikTokBusinessSdk.getNetworkSwitch());
    }

    @Test
    public void launchAppTriggerSuccessTest() throws InterruptedException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // disable all other auto events other than LaunchAPP
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .disableInstallLogging()
                .disableRetentionLogging()
                .disableAutoStart()
                .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // clear all persisted events
        TikTokBusinessSdk.clearAll();

        MessageHandler messageHandler = new MessageHandler();
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                messageHandler.handleMessage(message);
            }
        };
        TTSDKMonitor ttsdkMonitor = new TTSDKMonitor(mHandler);
        TikTokBusinessSdk.setUpSdkListeners(ttsdkMonitor, ttsdkMonitor, ttsdkMonitor, ttsdkMonitor);

        while (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            sleep(1);
        }

        // start delay track & force flush
        // this will only trigger LaunchAPP event
        TikTokBusinessSdk.startTrack();

        sleep(2);

        // no events in disk, coz no failures
        assertEquals(0, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 1 total
        assertEquals(1,messageHandler.totalRequests);
        // 1 total success
        assertEquals(1, messageHandler.totalSuccessfulRequests);
    }

    @Test
    public void installAppTriggerSuccessTest() throws InterruptedException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // clear sh cache
        appContext.getApplicationContext()
                .getSharedPreferences(TTSDK_KEY_VALUE_STORE, Context.MODE_PRIVATE).edit().clear().commit();

        // disable all other auto events other than InstallApp
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .disableLaunchLogging()
                .disableRetentionLogging()
                .disableAutoStart()
                .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // clear all persisted events
        TikTokBusinessSdk.clearAll();

        MessageHandler messageHandler = new MessageHandler();
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                messageHandler.handleMessage(message);
            }
        };
        TTSDKMonitor ttsdkMonitor = new TTSDKMonitor(mHandler);
        TikTokBusinessSdk.setUpSdkListeners(ttsdkMonitor, ttsdkMonitor, ttsdkMonitor, ttsdkMonitor);

        while (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            sleep(1);
        }

        // start delay track & force flush
        // this will only trigger InstallApp event
        TikTokBusinessSdk.startTrack();

        sleep(2);

        // no events in disk, coz no failures
        assertEquals(0, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 1 total
        assertEquals(1,messageHandler.totalRequests);
        // 1 total success
        assertEquals(1, messageHandler.totalSuccessfulRequests);
    }

    @Test
    public void twoDayRetentionTriggerSuccessTest() throws InterruptedException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // clear sh cache
        SharedPreferences sh = appContext.getApplicationContext()
                .getSharedPreferences(TTSDK_KEY_VALUE_STORE, Context.MODE_PRIVATE);
        sh.edit().clear().commit();

        // Set Install cache to Date() - 1 DAY
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        Date yesterday = c.getTime();
        sh.edit().putString(TTSDK_APP_FIRST_INSTALL, timeFormat.format(yesterday)).commit();

        // disable all other auto events other than 2Dretention
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .disableLaunchLogging()
                .disableInstallLogging()
                .disableAutoStart()
                .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // clear all persisted events
        TikTokBusinessSdk.clearAll();

        MessageHandler messageHandler = new MessageHandler();
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                messageHandler.handleMessage(message);
            }
        };
        TTSDKMonitor ttsdkMonitor = new TTSDKMonitor(mHandler);
        TikTokBusinessSdk.setUpSdkListeners(ttsdkMonitor, ttsdkMonitor, ttsdkMonitor, ttsdkMonitor);

        while (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            sleep(1);
        }

        // start delay track & force flush
        // this will only trigger 2Dretention event
        TikTokBusinessSdk.startTrack();

        sleep(2);

        // no events in disk, coz no failures
        assertEquals(0, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 1 total
        assertEquals(1,messageHandler.totalRequests);
        // 1 total success
        assertEquals(1, messageHandler.totalSuccessfulRequests);
    }

    @Test
    public void customEventTriggerSuccessTest() throws InterruptedException, JSONException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // disable all other auto events other than LaunchAPP
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .disableAutoStart()
                .disableAutoEvents()
                .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // clear all persisted events
        TikTokBusinessSdk.clearAll();

        MessageHandler messageHandler = new MessageHandler();
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                messageHandler.handleMessage(message);
            }
        };
        TTSDKMonitor ttsdkMonitor = new TTSDKMonitor(mHandler);
        TikTokBusinessSdk.setUpSdkListeners(ttsdkMonitor, ttsdkMonitor, ttsdkMonitor, ttsdkMonitor);

        while (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            sleep(1);
        }

        // sample event with no custom properties
        TikTokBusinessSdk.trackEvent("Login");
        // custom event with nested properties
        JSONObject props = new JSONObject()
                .put("level_number", "L5")
                .put("score", 1500)
                .put("ttProperty", new JSONObject().put("nestedTTProperty", 1))
                .put("jsonObject", new JSONObject().put("nestedJSONObject", 1));
        TikTokBusinessSdk.trackEvent("AchieveLevel", props);

        // common purchase event
        // Order with 2 items in cart
        TikTokBusinessSdk.trackEvent("Purchase");

        // start delay track & force flush
        TikTokBusinessSdk.startTrack();

        sleep(2);

        // no events in disk, coz no failures
        assertEquals(0, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 3 total
        assertEquals(3,messageHandler.totalRequests);
        // 3 total success
        assertEquals(3, messageHandler.totalSuccessfulRequests);
    }

    @Test
    public void customEventTriggerFailTest() throws InterruptedException, JSONException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // disable all other auto events other than LaunchAPP
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .disableAutoStart()
                .disableAutoEvents()
                .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // clear all persisted events
        TikTokBusinessSdk.clearAll();

        MessageHandler messageHandler = new MessageHandler();
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                messageHandler.handleMessage(message);
            }
        };
        TTSDKMonitor ttsdkMonitor = new TTSDKMonitor(mHandler);
        TikTokBusinessSdk.setUpSdkListeners(ttsdkMonitor, ttsdkMonitor, ttsdkMonitor, ttsdkMonitor);

        while (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            sleep(1);
        }

        // sample event with no custom properties
        TikTokBusinessSdk.trackEvent("Login");
        // custom event with nested properties
        JSONObject props = new JSONObject()
                .put("level_number", "L5")
                .put("score", 1500)
                .put("ttProperty", new JSONObject().put("nestedTTProperty", 1))
                .put("jsonObject", new JSONObject().put("nestedJSONObject", 1));
        TikTokBusinessSdk.trackEvent("AchieveLevel", props);

        TikTokBusinessSdk.trackEvent("Purchase");

        // start delay track & force flush
        // this will only trigger LaunchAPP event
        TikTokBusinessSdk.startTrack();

        sleep(2);

        // 3 failed events to be cached in disk
        assertEquals(3, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 3 total
        assertEquals(3,messageHandler.totalRequests);
        // 0 total success
        assertEquals(0, messageHandler.totalSuccessfulRequests);
    }

    @Test
    public void customEventScheduleTest() throws InterruptedException, JSONException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // disable all other auto events other than LaunchAPP
        TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(appContext)
                .disableAutoStart()
                .disableAutoEvents()
                .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
        TikTokBusinessSdk.initializeSdk(ttConfig);

        // clear all persisted events
        TikTokBusinessSdk.clearAll();

        MessageHandler messageHandler = new MessageHandler();
        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                messageHandler.handleMessage(message);
            }
        };
        TTSDKMonitor ttsdkMonitor = new TTSDKMonitor(mHandler);
        TikTokBusinessSdk.setUpSdkListeners(ttsdkMonitor, ttsdkMonitor, ttsdkMonitor, ttsdkMonitor);

        while (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            sleep(1);
        }

        // start delay track & force flush
        TikTokBusinessSdk.startTrack();

        // sample event with no custom properties
        TikTokBusinessSdk.trackEvent("Login");

        sleep(5);

        // new Login event in memory
        assertEquals(1, messageHandler.memorySize);

        sleep(11);

        // 15 second triggers 1st batch

        // no events in disk, coz no failures
        assertEquals(0, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 1 total
        assertEquals(1,messageHandler.totalRequests);
        // 1 total success
        assertEquals(1, messageHandler.totalSuccessfulRequests);


        // custom event with nested properties
        JSONObject props = new JSONObject()
                .put("level_number", "L5")
                .put("score", 1500)
                .put("ttProperty", new JSONObject().put("nestedTTProperty", 1))
                .put("jsonObject", new JSONObject().put("nestedJSONObject", 1));
        TikTokBusinessSdk.trackEvent("AchieveLevel", props);

        sleep(5);

        // new AchieveLevel event in memory
        assertEquals(1, messageHandler.memorySize);

        sleep(11);

        // 15 second triggers 2nd batch

        // no events in disk, coz no failures
        assertEquals(0, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 2 total
        assertEquals(2,messageHandler.totalRequests);
        // 2 total success
        assertEquals(2, messageHandler.totalSuccessfulRequests);

        TikTokBusinessSdk.trackEvent("Purchase");

        sleep(5);

        // new AchieveLevel event in memory
        assertEquals(1, messageHandler.memorySize);

        sleep(12);

        // 3rd batch fail test

        // no events in disk, coz no failures
        assertEquals(1, messageHandler.diskSize);
        // no new events in memory
        assertEquals(0, messageHandler.memorySize);

        // no new requests pending
        assertEquals(0, messageHandler.toBeSentRequests);
        // no failed
        assertEquals(0, messageHandler.failedRequests);
        // 3 total
        assertEquals(3,messageHandler.totalRequests);
        // 2 total success
        assertEquals(2, messageHandler.totalSuccessfulRequests);
    }

    private void sleep(int sec) throws InterruptedException {
        Thread.sleep(sec * 1000);
    }

    private static void print(Object... obj) {
        System.out.println(Arrays.toString(obj));
    }

    static class MessageHandler {
        static final int UPDATE_MEMORY = 0;
        static final int UPDATE_DISK = 1;
        static final int UPDATE_NETWORK = 2;
        static final int DUMPED = 3;
        static final int UPDATE_TIMER = 4;
        static final int UPDATE_THRESHOLD = 5;

        int diskSize;
        int dumped;
        int memorySize;
        int toBeSentRequests, successfulRequest, failedRequests, totalRequests, totalSuccessfulRequests;
        int timeLeft;
        int threshold, thresholdLeft;

        public void handleMessage(@NonNull Message msg) {
            String[] arr;
            switch (msg.what) {
                case UPDATE_MEMORY:
                    memorySize = Integer.parseInt(String.valueOf(msg.obj));
                    print(TAG + " => UPDATE_MEMORY : " + memorySize);
                    break;
                case UPDATE_DISK:
                    diskSize = Integer.parseInt(String.valueOf(msg.obj));
                    print(TAG + " => UPDATE_DISK : " + diskSize);
                    break;
                case UPDATE_NETWORK:
                    arr = String.valueOf(msg.obj).split(",");
                    toBeSentRequests = Integer.parseInt(arr[0]);
                    successfulRequest = Integer.parseInt(arr[1]);
                    failedRequests = Integer.parseInt(arr[2]);
                    totalRequests = Integer.parseInt(arr[3]);
                    totalSuccessfulRequests = Integer.parseInt(arr[4]);
                    print(TAG + " => UPDATE_NETWORK : ", toBeSentRequests, successfulRequest, failedRequests, totalRequests, totalSuccessfulRequests);
                    break;
                case DUMPED:
                    dumped = Integer.parseInt(String.valueOf(msg.obj));
                    print(TAG + " => DUMPED : " + dumped);
                    break;
                case UPDATE_TIMER:
                    timeLeft = Integer.parseInt(String.valueOf(msg.obj));
                    print(TAG + " => UPDATE_TIMER : " + timeLeft);
                    break;
                case UPDATE_THRESHOLD:
                    arr = String.valueOf(msg.obj).split(",");
                    threshold = Integer.parseInt(arr[0]);
                    thresholdLeft = Integer.parseInt(arr[1]);
//                    print(TAG + " => UPDATE_THRESHOLD : " + threshold, thresholdLeft);
                    break;
            }
        }
    }

    static class TTSDKMonitor implements TikTokBusinessSdk.NetworkListener,
            TikTokBusinessSdk.MemoryListener, TikTokBusinessSdk.DiskStatusListener, TikTokBusinessSdk.NextTimeFlushListener {

        private final Handler handler;

        public TTSDKMonitor(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onDiskChange(int diskSize, boolean read) {
            Message msg = new Message();
            msg.what = MessageHandler.UPDATE_DISK;
            msg.obj = diskSize;
            handler.sendMessage(msg);
        }

        @Override
        public void onDumped(int dumped) {
            Message msg = new Message();
            msg.what = MessageHandler.DUMPED;
            msg.obj = dumped;
            handler.sendMessage(msg);
        }

        @Override
        public void onMemoryChange(int size) {
            Message msg = new Message();
            msg.what = MessageHandler.UPDATE_MEMORY;
            msg.obj = size;
            handler.sendMessage(msg);
        }

        @Override
        public void onNetworkChange(int toBeSentRequests, int successfulRequest,
                                    int failedRequests, int totalRequests, int totalSuccessfulRequests) {
            Message msg = new Message();
            msg.what = MessageHandler.UPDATE_NETWORK;
            msg.obj = toBeSentRequests + "," + successfulRequest + "," + failedRequests + "," + totalRequests + "," + totalSuccessfulRequests;
            handler.sendMessage(msg);
        }

        @Override
        public void timeLeft(int timeLeft) {
            Message msg = new Message();
            msg.what = MessageHandler.UPDATE_TIMER;
            msg.obj = timeLeft;
            handler.sendMessage(msg);
        }

        @Override
        public void thresholdLeft(int threshold, int left) {
            Message msg = new Message();
            msg.what = MessageHandler.UPDATE_THRESHOLD;
            msg.obj = threshold + "," + left;
            handler.sendMessage(msg);
        }
    }

}