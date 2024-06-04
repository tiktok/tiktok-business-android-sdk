/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.deeplink;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;

import com.example.utils.DeepLinkUtil;

public class DeepLinkViewModel extends AndroidViewModel {

    public DeepLinkViewModel(Application application) {
        super(application);
    }

    public void openDeepLink(String deepLink){
        DeepLinkUtil.openDeepLink(getApplication(), deepLink);
    }

}