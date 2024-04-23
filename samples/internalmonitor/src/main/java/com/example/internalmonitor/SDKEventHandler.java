/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.internalmonitor;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class SDKEventHandler extends Handler {
    static final int UPDATE_MEMORY = 0;
    static final int UPDATE_DISK = 1;
    static final int UPDATE_NETWORK = 2;
    static final int DUMPED = 3;
    static final int UPDATE_TIMER = 4;
    static final int UPDATE_THRESHOLD = 5;

    private Activity activity;

    public SDKEventHandler(Activity context) {
        this.activity = context;
    }

    public void handleMessage(@NonNull Message msg) {
        if (msg.what == UPDATE_MEMORY) {
            TextView view = activity.findViewById(R.id.memoryStatus);
            view.setText(msg.obj.toString());
        } else if (msg.what == UPDATE_DISK) {
            TextView view = activity.findViewById(R.id.diskStatus);
            view.setText(msg.obj.toString());
        } else if (msg.what == UPDATE_NETWORK) {
            TextView toBeSent = activity.findViewById(R.id.toBeSent);
            String[] data = msg.obj.toString().split(",");

            toBeSent.setText(data[0]);

            TextView succeeded = activity.findViewById(R.id.succeeded);
            succeeded.setText(data[1]);

            TextView failed = activity.findViewById(R.id.failed);
            failed.setText(data[2]);

            TextView historyTotal = activity.findViewById(R.id.historyTotal);
            historyTotal.setText(data[3]);

            TextView historySucceeded = activity.findViewById(R.id.historySucceeded);
            historySucceeded.setText(data[4]);
        } else if (msg.what == DUMPED) {
            TextView failed = activity.findViewById(R.id.dumped);
            failed.setText(msg.obj.toString());
        } else if (msg.what == UPDATE_TIMER) {
            TextView nextFlush = activity.findViewById(R.id.nextFlush);
            nextFlush.setText(msg.obj.toString());
        } else if (msg.what == UPDATE_THRESHOLD) {
            String[] data = msg.obj.toString().split(",");
            TextView threshold = activity.findViewById(R.id.threshold);
            threshold.setText(data[0]);

            TextView countToThreshold = activity.findViewById(R.id.countToThreshold);
            countToThreshold.setText(data[1]);
        } else {
            super.handleMessage(msg);
        }
    }
}
