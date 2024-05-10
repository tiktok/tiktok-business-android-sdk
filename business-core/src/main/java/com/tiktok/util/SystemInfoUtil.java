/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.webkit.WebSettings;
import androidx.annotation.RequiresApi;

import com.tiktok.BuildConfig;
import com.tiktok.TikTokBusinessSdk;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

public class SystemInfoUtil {

    static PackageManager pm;
    static PackageInfo packageInfo;
    static Application application;

    static {
        try {
            application = TikTokBusinessSdk.getApplicationContext();
            pm = application.getPackageManager();
            packageInfo = pm.getPackageInfo(TikTokBusinessSdk.getApplicationContext().getPackageName(), 0);
        } catch (Exception ignored) {
        }
    }

    public static String getPackageName() {
        return packageInfo.packageName;
    }

    public static String getAppName() {
        return application.getApplicationInfo().loadLabel(pm).toString();
    }

    public static String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static String getAppVersionName() {
        if (packageInfo == null) {
            return "";
        }
        return packageInfo.versionName;
    }

    public static int getAppVersionCode() {
        if (packageInfo == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return (int) packageInfo.getLongVersionCode();
        }
        // noinspection deprecation
        return packageInfo.versionCode;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            return "";
        }
        return "";
    }

    public static String getLocale() {
        return Locale.getDefault().getLanguage();
    }

    private static String userAgent = null;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void initUserAgent() {
        if (userAgent != null) return;
        long initTimeMS = System.currentTimeMillis();
        TikTokBusinessSdk.getAppEventLogger().monitorMetric("ua_init", TTUtil.getMetaWithTS(initTimeMS), null);
        Throwable ex = null;
        try {
            userAgent = WebSettings.getDefaultUserAgent(TikTokBusinessSdk.getApplicationContext());
        } catch (Exception e) {
            ex = e;
            userAgent = System.getProperty("http.agent");
        }
        // to avoid loops
        if (userAgent == null) userAgent = "";
        long endTimeMS = System.currentTimeMillis();
        try {
            JSONObject meta = TTUtil.getMetaException(ex, endTimeMS, TTSDK_EXCEPTION_SDK_CATCH)
                    .put("latency", endTimeMS-initTimeMS);
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("ua_end", meta, null);
        } catch (Exception ignored) {}
    }

    public static String getUserAgent() {
        if (userAgent == null) {
            initUserAgent();
        }
        return userAgent;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.SDK_INT + "";
    }

    public static String getNetworkClass(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isConnected())
                return "-"; // not connected
            if (info.getType() == ConnectivityManager.TYPE_WIFI)
                return "WIFI";
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                int networkType = info.getSubtype();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                    case TelephonyManager.NETWORK_TYPE_GSM:
                        return "2G";
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                    case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                        return "3G";
                    case TelephonyManager.NETWORK_TYPE_LTE:
                    case TelephonyManager.NETWORK_TYPE_IWLAN:
                    case 19:
                        return "4G";
                    case TelephonyManager.NETWORK_TYPE_NR:
                        return "5G";
                    default:
                        return "?";
                }
            }
        } catch (Exception ignored) {}
        return "?";
    }

}
