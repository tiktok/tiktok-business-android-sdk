/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.home;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.R;
import com.tiktok.TikTokBusinessSdk;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mSubsText;
    private final MutableLiveData<String> mLogText;
    SharedPreferences sharedPreferences;

    public HomeViewModel(Application application) {
        super(application);
        mText = new MutableLiveData<>();
        mText.setValue("Purchase");
        mSubsText = new MutableLiveData<>();
        mSubsText.setValue("Subscribe");
        mLogText = new MutableLiveData<>();
        mLogText.setValue("init");
        sharedPreferences = getApplication().getSharedPreferences("TT_IDENTIFY", Context.MODE_PRIVATE);
    }

    public void checkInitTTAM() {
        if (checkAMCacheExist()) {
            TikTokBusinessSdk.identify(getExternalID(), getExternalUsername(),
                    getExternalPhoneNumber(), getExternalEmail());
        }
    }

    public void setText(String txt) {
        mText.setValue(txt);
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getSubscribeText() {
        return mSubsText;
    }

    public void setSubscribeText(String txt) {
        mSubsText.setValue(txt);
    }

    public LiveData<String> getLogText() {
        return mLogText;
    }

    public void setLogText(String txt) {
        mLogText.setValue(txt);
    }

    public void setNewCache(String externalId,
                            @Nullable String externalUserName,
                            @Nullable String phoneNumber,
                            @Nullable String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getApplication().getString(R.string.tt_am_external_id), externalId);
        if (externalUserName != null) {
            editor.putString(getApplication().getString(R.string.tt_am_external_username), externalUserName);
        }
        if (phoneNumber != null) {
            editor.putString(getApplication().getString(R.string.tt_am_external_phone), phoneNumber);
        }
        if (email != null) {
            editor.putString(getApplication().getString(R.string.tt_am_external_email), email);
        }
        editor.putBoolean(getApplication().getString(R.string.tt_am_init), true);
        editor.apply();
    }

    public void resetCache() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(getApplication().getString(R.string.tt_am_external_id));
        editor.remove(getApplication().getString(R.string.tt_am_external_username));
        editor.remove(getApplication().getString(R.string.tt_am_external_phone));
        editor.remove(getApplication().getString(R.string.tt_am_external_email));
        editor.remove(getApplication().getString(R.string.tt_am_init));
        editor.apply();
    }

    public boolean checkAMCacheExist() {
        return sharedPreferences.getBoolean(getApplication().getString(R.string.tt_am_init), false);
    }

    public String getExternalID() {
        return sharedPreferences.getString(getApplication().getString(R.string.tt_am_external_id), null);
    }

    public String getExternalUsername() {
        return sharedPreferences.getString(getApplication().getString(R.string.tt_am_external_username), null);
    }

    public String getExternalPhoneNumber() {
        return sharedPreferences.getString(getApplication().getString(R.string.tt_am_external_phone), null);
    }

    public String getExternalEmail() {
        return sharedPreferences.getString(getApplication().getString(R.string.tt_am_external_email), null);
    }
}