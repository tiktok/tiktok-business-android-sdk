/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.internalmonitor;

import android.os.Handler;
import android.os.Message;

import com.tiktok.TikTokBusinessSdk;

public class TTSDKMonitor implements TikTokBusinessSdk.NetworkListener,
        TikTokBusinessSdk.MemoryListener, TikTokBusinessSdk.DiskStatusListener, TikTokBusinessSdk.NextTimeFlushListener {

    private Handler handler;

    public TTSDKMonitor(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onDiskChange(int diskSize, boolean read) {
        Message msg = new Message();
        msg.what = SDKEventHandler.UPDATE_DISK;
        msg.obj = diskSize + "";
        handler.sendMessage(msg);
    }

    @Override
    public void onDumped(int dumped) {
        Message msg = new Message();
        msg.what = SDKEventHandler.DUMPED;
        msg.obj = dumped + "";
        handler.sendMessage(msg);
    }

    @Override
    public void onMemoryChange(int size) {
        Message msg = new Message();
        msg.what = SDKEventHandler.UPDATE_MEMORY;
        msg.obj = size + "";
        handler.sendMessage(msg);
    }

    @Override
    public void onNetworkChange(int toBeSentRequests, int successfulRequest,
                                int failedRequests, int totalRequests, int totalSuccessfulRequests) {
        Message msg = new Message();
        msg.what = SDKEventHandler.UPDATE_NETWORK;
        msg.obj = toBeSentRequests + "," + successfulRequest + "," + failedRequests + ',' + totalRequests + ',' + totalSuccessfulRequests;
        handler.sendMessage(msg);
    }

    @Override
    public void timeLeft(int timeLeft) {
        Message msg = new Message();
        msg.what = SDKEventHandler.UPDATE_TIMER;
        msg.obj = timeLeft + "";
        handler.sendMessage(msg);
    }

    @Override
    public void thresholdLeft(int threshold, int left) {
        Message msg = new Message();
        msg.what = SDKEventHandler.UPDATE_THRESHOLD;
        msg.obj = threshold + "," + left;
        handler.sendMessage(msg);
    }
}
