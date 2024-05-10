/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_NET_ERROR;

import androidx.annotation.Nullable;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTCrashHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HttpRequestUtil {

    private static final String MONITOR_API_TYPE = "/app/monitor/";
    private static final String API_ERR = "api_err";

    public static class HttpRequestOptions {
        private static int UNSET = -1;
        public int connectTimeout = UNSET;
        public int readTimeout = UNSET;

        public void configConnection(HttpURLConnection connection) {
            if (connectTimeout != UNSET) {
                connection.setConnectTimeout(connectTimeout);
            }
            if (readTimeout != UNSET) {
                connection.setReadTimeout(readTimeout);
            }
        }
    }

    private static final String TAG = HttpRequestUtil.class.getCanonicalName();

    private static final TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    public static String doGet(String url, Map<String, String> headerParamMap) {
        HttpRequestOptions options = new HttpRequestOptions();
        options.connectTimeout = 2000;
        options.readTimeout = 5000;
        return doGet(url, headerParamMap, options);
    }

    public static HttpsURLConnection connect(String url, Map<String, String> headerParamMap, HttpRequestOptions options, String method, String contentLength) {
        HttpsURLConnection connection = null;

        try {
            URL httpURL = new URL(url);
            connection = (HttpsURLConnection) httpURL.openConnection();
            connection.setRequestMethod(method);
            options.configConnection(connection);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            if(method.equals("GET")) {
                connection.setDoOutput(false);
            } else if(method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", contentLength);
            }

            for (Map.Entry<String, String> entry : headerParamMap.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            connection.connect();
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
            if (connection != null) {
                try {
                    connection.disconnect();
                }catch (Exception exc){
                    TTCrashHandler.handleCrash(TAG, exc, TTSDK_EXCEPTION_NET_ERROR);
                }
            }
        }
        return connection;
    }

    public static boolean shouldRedirect(int status) {
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307)
                return true;
        }
        return false;
    }

    public static String doGet(String url, Map<String, String> headerParamMap, HttpRequestOptions options) {
        long initTimeMS = System.currentTimeMillis();
        String result = null;
        int responseCode = 0;
        String apiType = "";
        try {
            URL uri = new URL(url);
            if (uri.getPath().contains(TikTokBusinessSdk.getApiAvailableVersion())) {
                apiType = uri.getPath().split(TikTokBusinessSdk.getApiAvailableVersion())[1];
            } else {
                apiType = uri.getPath().split("open_api")[1];
            }
        } catch (MalformedURLException ignored) {}
        HttpsURLConnection connection = connect(url, headerParamMap, options, "GET", null);
        if (connection == null) return result;
        try{
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                connection = connect(redirectUrl, headerParamMap, options, "GET", null);
            }

            responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
                }
            }
        }
        long endTimeMS = System.currentTimeMillis();
        try {
            if (getCodeFromApi(result) != 0) {
                JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                        .put("latency", endTimeMS-initTimeMS)
                        .put("api_type", apiType)
                        .put("status_code", responseCode)
                        .put("log_id", getLogIDFromApi(result));
                TikTokBusinessSdk.getAppEventLogger().monitorMetric(API_ERR, meta, null);
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr) {
        HttpRequestOptions options = new HttpRequestOptions();
        options.connectTimeout = 2000;
        options.readTimeout = 5000;
        return doPost(url, headerParamMap, jsonStr, options);
    }

    public static String doPost(String url, Map<String, String> headerParamMap, String jsonStr, HttpRequestOptions options) {
        long initTimeMS = System.currentTimeMillis();
        String result = null;
        int responseCode = 0;
        String apiType = "";
        try {
            URL uri = new URL(url);
            if (uri.getPath().contains(TikTokBusinessSdk.getApiAvailableVersion())) {
                apiType = uri.getPath().split(TikTokBusinessSdk.getApiAvailableVersion())[1];
            } else {
                apiType = uri.getPath().split("open_api")[1];
            }
        } catch (MalformedURLException ignored) {}

        HttpURLConnection connection = null;
        OutputStream outputStream = null;

        try {
            byte[] writeBytes = jsonStr.getBytes("UTF-8");
            String contentLength = String.valueOf(writeBytes.length);

            connection = connect(url, headerParamMap, options, "POST", contentLength);
            if (connection == null) return result;
            outputStream = connection.getOutputStream();
            outputStream.write(writeBytes);
            outputStream.flush();
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                connection = connect(redirectUrl, headerParamMap, options, "POST", contentLength);
                outputStream = connection.getOutputStream();
                outputStream.write(writeBytes);
                outputStream.flush();
            }

            responseCode = connection.getResponseCode();
            // http code is different from the code returned by api
            if (responseCode == HttpURLConnection.HTTP_OK) {
                result = streamToString(connection.getInputStream());
            }
            if(TikTokBusinessSdk.isInSdkDebugMode()) {
                ttLogger.info("doPost request body: %s", jsonStr);
                ttLogger.info("doPost result: %s", result == null ? String.valueOf(responseCode) : result);
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e){
                    TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
                }
            }
        }
        long endTimeMS = System.currentTimeMillis();
        try {
            if (getCodeFromApi(result) != 0 && !url.contains(MONITOR_API_TYPE)) {
                JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                        .put("latency", endTimeMS-initTimeMS)
                        .put("api_type", apiType)
                        .put("status_code", responseCode)
                        .put("log_id", getLogIDFromApi(result));
                TikTokBusinessSdk.getAppEventLogger().monitorMetric(API_ERR, meta, null);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static String streamToString(InputStream is) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        }
        return null;
    }

    public static int getCodeFromApi(@Nullable String resp) {
        if (resp != null) {
            try {
                JSONObject respJson = new JSONObject(resp);
                return respJson.getInt("code");
            } catch (Exception ignored) {
                return -2;
            }
        }
        return -1;
    }

    public static String getLogIDFromApi(@Nullable String resp) {
        if (resp != null) {
            try {
                JSONObject respJson = new JSONObject(resp);
                return respJson.getString("request_id");
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}