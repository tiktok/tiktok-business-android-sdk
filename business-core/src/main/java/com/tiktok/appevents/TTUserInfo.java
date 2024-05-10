/*
 * Copyright (c) 2021. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 */

package com.tiktok.appevents;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import android.content.Context;

import com.tiktok.util.TTUtil;

import org.json.JSONObject;

import java.io.Serializable;
import java.security.MessageDigest;

public class TTUserInfo implements Cloneable, Serializable {
    static final String TAG = TTUserInfo.class.getName();
    String anonymousId;
    String externalId;
    String externalUserName;
    String phoneNumber;
    String email;
    transient boolean isIdentified = false;

    public static volatile TTUserInfo sharedInstance = new TTUserInfo();

    // clear the previous userInfo, useful when logging out
    public static void reset(Context context, boolean forceGenerateAnoId) {
        sharedInstance.anonymousId = TTUtil.getOrGenAnoId(context, forceGenerateAnoId);
        sharedInstance.externalId = null;
        sharedInstance.externalUserName = null;
        sharedInstance.phoneNumber = null;
        sharedInstance.email = null;
        sharedInstance.isIdentified = false;
    }

    private String toSha256(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : md.digest()) {
                result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
        }
        return null;
    }

    public boolean isIdentified() {
        return this.isIdentified;
    }

    public void setIdentified() {
        this.isIdentified = true;
    }

    public void setExternalId(String externalId) {
        this.externalId = toSha256(externalId);
    }

    public void setExternalUserName(String externalUserName) {
        this.externalUserName = toSha256(externalUserName);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = toSha256(phoneNumber);
    }

    public void setEmail(String email) {
        this.email = toSha256(email);
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("anonymous_id", anonymousId);
            if (externalId != null) {
                jsonObject.put("external_id", externalId);
            }
            if (externalUserName != null) {
                jsonObject.put("external_username", externalUserName);
            }
            if (phoneNumber != null) {
                jsonObject.put("phone_number", phoneNumber);
            }
            if (email != null) {
                jsonObject.put("email", email);
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
        }
        return jsonObject;
    }


    // Since there are no composition in this class, should be fairly safe
    // to do the default cloning
    public TTUserInfo clone() {
        try {
            return (TTUserInfo) super.clone();
        } catch (Exception e) {
            return new TTUserInfo();
        }
    }
}
