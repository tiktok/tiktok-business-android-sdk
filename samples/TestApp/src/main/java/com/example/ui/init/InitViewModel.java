/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.init;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;

public class InitViewModel extends AndroidViewModel {

    public static boolean autoEvent = true;
    public static boolean advertiserIDCollectionEnable = true;
    public static boolean autoStart = true;
    public static boolean Metrics = true;
    public static boolean debugModeSwitch = true;
    public static boolean lduModeSwitch = false;
    public static boolean autoIapTrack = true;
    public static boolean loggingStatus = true;
    public static boolean launchStatus = true;
    public static boolean retentionStatus = true;
    public static boolean initWithCallback = true;

    public InitViewModel(Application application) {
        super(application);
    }


}