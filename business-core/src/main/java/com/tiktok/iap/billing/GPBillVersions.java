package com.tiktok.iap.billing;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTReflect;

public class GPBillVersions {

    private static volatile String sVersion;

    public static GPBillingVer getMajorVersion() {
        try {
            return parseMajorVersion(getVersion());
        } catch (Throwable ignore) {
        }
        return GPBillingVer.NONE;
    }

    static GPBillingVer parseMajorVersion(String version) {
        if (version == null) {
            return GPBillingVer.NONE;
        }
        try {
            String[] vers = version.split("\\.");
            int major = Integer.parseInt(vers[0]);
            if (major == 1) {
                return GPBillingVer.V1;
            } else if (major > 1 && major < 5) {
                return GPBillingVer.V2_V4;
            } else if (major >= 5 && major <= 7) {
                return GPBillingVer.V5_V7;
            } else if (major == 8) {
                return GPBillingVer.V8;
            } else if (major == 9) {
                return GPBillingVer.V9;
            }
        } catch (Throwable ignore) {
        }
        return GPBillingVer.NONE;
    }

    public static String getVersion() {
        if (!TextUtils.isEmpty(sVersion)) {
            return sVersion;
        }

        sVersion = readFromMeta();
        if (!TextUtils.isEmpty(sVersion)) {
            return sVersion;
        }

        sVersion = readFromBuildConfig();
        if (!TextUtils.isEmpty(sVersion)) {
            return sVersion;
        }

        return "";
    }

    private static String readFromMeta() {
        try {
            Context context = TikTokBusinessSdk.getApplicationContext();
            if (context != null) {
                context.getPackageManager().getInstallerPackageName("");
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                String ver = info.metaData.getString("com.google.android.play.billingclient.version", null);
                if (ver != null && ver.length() > 2) {
                    return ver;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String readFromBuildConfig() {
        try {
            String ver = (String) TTReflect.on("com.android.billingclient.BuildConfig")
                    .findField("VERSION_NAME")
                    .getValue(null);
            if (ver != null && ver.length() > 2) {
                return ver;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    public enum GPBillingVer {
        NONE, V1, V2_V4, V5_V7, V8, V9
    }

}
