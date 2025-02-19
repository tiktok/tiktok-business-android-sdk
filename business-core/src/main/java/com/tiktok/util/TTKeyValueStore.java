/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import static com.tiktok.util.TTConst.TTSDK_KEY_VALUE_STORE;

/** Util for SharedPreferences get & set */
public class TTKeyValueStore {
    private SharedPreferences preferences = null;

    public TTKeyValueStore(Context ctx) {
        try{
            preferences = ctx.getApplicationContext().getSharedPreferences(TTSDK_KEY_VALUE_STORE, Context.MODE_PRIVATE);
        }catch (Throwable e){

        }
    }

    /** get SharedPreferences value from key */
    public String get(String key) {
        if(preferences == null){
            return "";
        }
        return preferences.getString(key, null);
    }

    /** set SharedPreferences key-value */
    public void set(String key, Object value) {
        if(preferences == null){
            return;
        }
        preferences.edit().putString(key, value.toString()).apply();
    }

    /** set multiple SharedPreferences key-values */
    public void set(HashMap<String, Object> data) {
        if(preferences == null){
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue().toString());
        }
        editor.apply();
    }
}