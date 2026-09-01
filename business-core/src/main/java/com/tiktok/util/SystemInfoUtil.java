/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import static com.tiktok.appevents.edp.EDPConfig.sensig_filtering_regex_list;
import static com.tiktok.appevents.edp.EDPConfig.sensig_filtering_regex_version;
import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;
import static com.tiktok.util.TTConst.TTSDK_USER_AGENT;

import android.app.Application;
import android.content.Context;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebSettings;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.tiktok.BuildConfig;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.ReferrerInfo;
import com.tiktok.appevents.edp.Sensig;

import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

public class SystemInfoUtil {

    private static String sAPPName = "";
    private static String sPackageName = "";
    private static String sVerName = "";
    private static int sVerCode = 0;


    private static volatile String sAppSessionId = "";
    private static ReferrerInfo sReferrerInfo = null;

    private static void initInfo() {
        try {
            Application application = TikTokBusinessSdk.getApplicationContext();
            if (application == null) {
                return;
            }

            sPackageName = application.getPackageName();


            PackageManager pm = application.getPackageManager();
            sAPPName = application.getApplicationInfo().loadLabel(pm).toString();

            PackageInfo info = pm.getPackageInfo(sPackageName, 0);

            sVerName = info.versionName;

            if (Build.VERSION.SDK_INT >= 28) {
                sVerCode = Long.valueOf(info.getLongVersionCode()).intValue();
            } else {
                sVerCode = info.versionCode;
            }

        } catch (Throwable ignored) {
        }
    }

    public static String getPackageName() {
        if (TextUtils.isEmpty(sPackageName)) {
            initInfo();
        }
        return sPackageName == null ? "" : sPackageName;
    }

    public static String getAppName() {
        if (TextUtils.isEmpty(sAPPName)) {
            initInfo();
        }
        return sAPPName == null ? "" : sAPPName;
    }

    public static String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static String getAppVersionName() {
        if (TextUtils.isEmpty(sVerName)) {
            initInfo();
        }
        return sVerName == null ? "" : sVerName;
    }

    public static int getAppVersionCode() {
        if (sVerCode == 0) {
            initInfo();
        }

        return sVerCode;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    public static String getLocale() {
        return Locale.getDefault().getLanguage();
    }

    public static void updateSensigInfo() {
        try {
            Sensig sensig = TTUtil.getSensigInfo(TikTokBusinessSdk.getApplicationContext());
            if (sensig == null) {
                return;
            }
            if (!TextUtils.isEmpty(sensig.getRegexList())) {
                sensig_filtering_regex_version = sensig.getVersion();
                sensig_filtering_regex_list = sensig.getRegexList();
            }
        } catch (Throwable ignore) {
        }
    }

    private static String sUserAgent = null;

    public static void initUserAgent() {
        if (!TextUtils.isEmpty(sUserAgent)) return;

        long initTimeMS = System.currentTimeMillis();
        Throwable ex = null;
        try {
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("ua_init", TTUtil.getMetaWithTS(initTimeMS), null);
            TTKeyValueStore store = new TTKeyValueStore(TikTokBusinessSdk.getApplicationContext());
            sUserAgent = store.get(TTSDK_USER_AGENT);

            if (TextUtils.isEmpty(sUserAgent)) {
                sUserAgent = WebSettings.getDefaultUserAgent(TikTokBusinessSdk.getApplicationContext());
                store.set(TTSDK_USER_AGENT, sUserAgent);
            }
        } catch (Throwable e) {
            ex = e;
        }

        try {
            if (TextUtils.isEmpty(sUserAgent)) {
                sUserAgent = System.getProperty("http.agent");
            }
        } catch (Throwable e) {
            ex = e;
        }


        // to avoid loops
        if (TextUtils.isEmpty(sUserAgent)) sUserAgent = "";
        long endTimeMS = System.currentTimeMillis();
        try {
            JSONObject meta = TTUtil.getMetaException(ex, endTimeMS, TTSDK_EXCEPTION_SDK_CATCH);
            JSON.putLong(meta, "latency", endTimeMS - initTimeMS);
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("ua_end", meta, null);
        } catch (Throwable ignored) {
        }
    }

    public static void initAppSessionId() {
        try {
            if (TextUtils.isEmpty(sAppSessionId)) {
                sAppSessionId = UUID.randomUUID().toString();

                //update for local cache
                LastSessionUtil.setLast(sAppSessionId);
            }
        } catch (Throwable ignore) {
        }
    }

    public static String getAppSessionId() {
        if (TextUtils.isEmpty(sAppSessionId)) {
            initAppSessionId();
        }
        return sAppSessionId;
    }

    public static void initInstallReferrer() {
        try {
            if (sReferrerInfo != null) {
                return;
            }
            InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(TikTokBusinessSdk.getApplicationContext()).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    try {
                        TTHandlerUtil.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    switch (responseCode) {
                                        case InstallReferrerClient.InstallReferrerResponse.OK:
                                            try {
                                                ReferrerDetails response = referrerClient.getInstallReferrer();
                                                String referrerUrl = response.getInstallReferrer();
                                                long referrerClickTime = response.getReferrerClickTimestampSeconds();
                                                long appInstallTime = response.getInstallBeginTimestampSeconds();
                                                sReferrerInfo = new ReferrerInfo(referrerUrl, appInstallTime, referrerClickTime);
                                            } catch (Throwable ignore) {
                                            }
                                            break;
                                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                                            break;
                                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                                            break;
                                    }
                                    referrerClient.endConnection();
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                    } catch (Throwable ignore) {
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {

                }
            });
        } catch (Throwable ignore) {
        }
    }

    public static ReferrerInfo getInstallReferrer() {
        if (sReferrerInfo == null) {
            initInstallReferrer();
        }
        return sReferrerInfo;
    }

    public static String getUserAgent() {
        if (TextUtils.isEmpty(sUserAgent)) {
            initUserAgent();
        }
        return sUserAgent;
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
        } catch (Throwable ignored) {
        }
        return "?";
    }

    private static int sScreenWidth = -1;
    private static int sScreenHeight = -1;
    private static float sDensity = -1F;

    private static void initScreenWidthAndHeight() {
        final Context ctx = TikTokBusinessSdk.getApplicationContext();
        if (ctx != null) {
            try {
                WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                DisplayMetrics dm = new DisplayMetrics();
                display.getRealMetrics(dm);
                sDensity = dm.density;
                sScreenWidth = dm.widthPixels;
                sScreenHeight = dm.heightPixels;
            } catch (Throwable ignore) {
                try {
                    DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
                    sDensity = dm.density;
                    sScreenWidth = dm.widthPixels;
                    sScreenHeight = dm.heightPixels;
                } catch (Throwable ignore2) {
                }
            }
        }
    }

    public static int getsScreenWidth() {
        if (sScreenWidth <= 0) {
            initScreenWidthAndHeight();
        }
        return Math.max(sScreenWidth, 0);
    }

    public static int getsScreenHeight() {
        if (sScreenHeight <= 0) {
            initScreenWidthAndHeight();
        }
        return Math.max(sScreenHeight, 0);
    }

    public static float getsDensity() {
        if (sDensity <= 0) {
            initScreenWidthAndHeight();
        }
        return Math.max(sDensity, 0);
    }

    private static boolean sHasGetUnity = false;
    private static boolean sIsUnity = false;
    private static String sLibraryName = "";

    private static boolean sHasGetRN = false;
    private static boolean sIsRN = false;

    public static boolean isUnity() {
        if (!sHasGetUnity) {
            try {
                Class.forName("com.unity3d.player.UnityPlayer");
                sIsUnity = true;
            } catch (Throwable ignore) {
                sIsUnity = false;
            }
            sHasGetUnity = true;
        }

        return sIsUnity;
    }

    public static boolean isRN() {
        if (!sHasGetRN) {
            try {
                Class.forName("com.tiktokbusinessreactnativesdk.TiktokBusinessReactNativeSdkModule");
                sIsRN = true;
            } catch (Throwable ignore) {
                sIsRN = false;
            }
            sHasGetRN = true;
        }
        return sIsRN;
    }


    public static String getLibraryName() {
        if (TextUtils.isEmpty(sLibraryName)) {
            if (isUnity()) {
                sLibraryName = "tiktok-business-unity-android-sdk";
            } else if (isRN()) {
                sLibraryName = "tiktok-business-rn-android-sdk";
            } else {
                sLibraryName = "tiktok-business-android-sdk";
            }
        }

        return sLibraryName;
    }


    public static String getBcp47Language() {
        try {
            Locale loc = getCurrentLocale();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return loc.toLanguageTag();
            }

            // we will use a dash as per BCP 47
            final char SEP = '-';
            String language = loc.getLanguage();
            String region = loc.getCountry();
            String variant = loc.getVariant();

            // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
            // this goes before the string matching since "NY" wont pass the variant checks
            if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
                language = "nn";
                region = "NO";
                variant = "";
            }

            if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}")) {
                language = "und";       // Follow the Locale#toLanguageTag() implementation
                // which says to return "und" for Undetermined
            } else if (language.equals("iw")) {
                language = "he";        // correct deprecated "Hebrew"
            } else if (language.equals("in")) {
                language = "id";        // correct deprecated "Indonesian"
            } else if (language.equals("ji")) {
                language = "yi";        // correct deprecated "Yiddish"
            }

            // ensure valid country code, if not well formed, it's omitted
            if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}")) {
                region = "";
            }

            // variant subtags that begin with a letter must be at least 5 characters long
            if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}")) {
                variant = "";
            }

            StringBuilder bcp47Tag = new StringBuilder(language);
            if (!region.isEmpty()) {
                bcp47Tag.append(SEP).append(region);
            }
            if (!variant.isEmpty()) {
                bcp47Tag.append(SEP).append(variant);
            }

            return bcp47Tag.toString();
        } catch (Throwable ignore) {
            return "-";
        }
    }

    private static Locale getCurrentLocale() {
        try {
            Context context = TikTokBusinessSdk.getApplicationContext();
            if (context != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return context.getResources().getConfiguration().getLocales().get(0);
                } else {
                    // noinspection deprecation
                    return context.getResources().getConfiguration().locale;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String sInstallSource;

    private static void initInstallSource() {
        try {
            Context context = TikTokBusinessSdk.getApplicationContext();
            if (context == null) {
                return;
            }

            final PackageManager pm = context.getPackageManager();
            final String pkgName = getPackageName();
            if (pm == null || TextUtils.isEmpty(pkgName)) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo install = pm.getInstallSourceInfo(pkgName);
                sInstallSource = install.getInitiatingPackageName();
            } else {
                sInstallSource = pm.getInstallerPackageName(pkgName);
            }
        } catch (Throwable ignore) {
            sInstallSource = "Unknown";
        }
    }

    public static String getInstallSource() {
        if (TextUtils.isEmpty(sInstallSource)) {
            initInstallSource();
        }

        return TextUtils.isEmpty(sInstallSource) ? "Unknown" : sInstallSource;
    }

}
