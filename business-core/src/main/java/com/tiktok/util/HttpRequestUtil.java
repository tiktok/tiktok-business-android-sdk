/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_NET_ERROR;

import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTCrashHandler;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class HttpRequestUtil {
    private static final String TAG = "HttpRequestUtil";
    private static final String CHARSET_UTF8 = "UTF-8";

    public static class HttpResponse {
        public String url;
        public JSONObject body; //response body
        public int code = -1; // response code
        public int httpCode = -1; //http code
        public Throwable throwable; //some errors
        public long duration; //request duration

        public boolean isOK() {
            getErrCode();
            return code == 0 && httpCode == HttpURLConnection.HTTP_OK;
        }

        public int getErrCode() {
            if (code == -1 && body != null) {
                code = JSON.getInt(body, "code", -1);
            }
            return httpCode == HttpURLConnection.HTTP_OK ? code : httpCode;
        }

        public String getErrMsg() {
            String msg = JSON.getString(body, "message", "");
            if (throwable != null) {
                msg += "==" + throwable.getMessage();
            }
            return TextUtils.isEmpty(msg) ? "unknown" : msg;
        }
    }

    public static class HttpRequestOptions {
        private boolean isGzip = true;
        private static final int UNSET = -1;
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

    public static HttpsURLConnection connect(String url, Map<String, String> headerParamMap, HttpRequestOptions options, String method, String contentLength) throws Throwable {
        URL httpURL = new URL(url);

        HttpsURLConnection connection = (HttpsURLConnection) httpURL.openConnection();

        connection.setRequestMethod(method);
        options.configConnection(connection);
        connection.setDoInput(true);
        connection.setUseCaches(false);

        if (method.equals("GET")) {
            connection.setDoOutput(false);
        } else if (method.equals("POST")) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", contentLength);
        }

        if (headerParamMap != null && !headerParamMap.isEmpty()) {
            for (Map.Entry<String, String> entry : headerParamMap.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    connection.setRequestProperty(key, value);
                }
            }
        }

        if (options.isGzip) {
            connection.setRequestProperty("Content-Encoding", "gzip");
        }

        connection.connect();

        return connection;
    }

    public static boolean shouldRedirect(int status) {
        if (status != HttpURLConnection.HTTP_OK) {
            return status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307;
        }
        return false;
    }

    /**
     * Nothing may leave the device while the app turned the sdk off, whatever the caller is.
     * The remote kill switch is deliberately not part of this check, so that the global config
     * can still be fetched and lift it.
     */
    private static boolean isNetworkAllowed() {
        return Boolean.TRUE.equals(TikTokBusinessSdk.getSdkGlobalSwitch());
    }

    private static HttpResponse blockedResponse(String url) {
        HttpResponse response = new HttpResponse();
        response.url = url;
        return response;
    }

    public static HttpResponse doGet(String url, Map<String, String> headerParamMap, HttpRequestOptions options) {
        if (!isNetworkAllowed()) {
            return blockedResponse(url);
        }
        long initTimeMS = SystemClock.elapsedRealtime();

        HttpResponse response = new HttpResponse();
        response.url = url;

        HttpsURLConnection connection = null;

        try {
            connection = connect(url, headerParamMap, options, "GET", null);

            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                IOUtils.close(connection);
                connection = connect(redirectUrl, headerParamMap, options, "GET", null);
            }

            int httpCode = connection.getResponseCode();
            response.httpCode = httpCode;

            if (httpCode == HttpURLConnection.HTTP_OK) {
                String result = streamToString(connection.getInputStream());
                JSONObject json = JSON.build(result);
                if (json != null) {
                    response.body = json;
                    response.code = JSON.getInt(json, "code", -1);
                }
            }
        } catch (Throwable e) {
            response.throwable = e;
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            if (connection != null) {
                IOUtils.close(connection);
            }
        }


        response.duration = SystemClock.elapsedRealtime() - initTimeMS;

        monitorNetRequest(response);

        return response;
    }

    public static HttpResponse doPost(String url, Map<String, String> headerParamMap, String jsonStr) {
        return doPost(url, headerParamMap, jsonStr, true);
    }

    public static HttpResponse doPost(String url, Map<String, String> headerParamMap, String jsonStr, boolean needSignature) {
        HttpRequestOptions options = new HttpRequestOptions();

        try {
            if (url.contains(UrlConst.PATH_CONFIG2)
                    || url.contains(UrlConst.PATH_DDL)
                    || url.contains(UrlConst.PATH_CONFIG)) {
                options.connectTimeout = NetworkTimeout.sConfigTime;
                options.readTimeout = NetworkTimeout.sConfigTime * 3;
            } else {
                options.connectTimeout = NetworkTimeout.sEventTime;
                options.readTimeout = NetworkTimeout.sEventTime * 3;
            }
        } catch (Throwable ignore) {
            options.connectTimeout = 2000;
            options.readTimeout = 5000;
        }

        return doPost(url, headerParamMap, jsonStr, options, needSignature);
    }

    public static HttpResponse doPost(String url, Map<String, String> headerParamMap, String jsonStr, HttpRequestOptions options, boolean needSignature) {
        if (!isNetworkAllowed()) {
            return blockedResponse(url);
        }
        long initTimeMS = SystemClock.elapsedRealtime();

        HttpResponse response = new HttpResponse();
        response.url = url;

        HttpURLConnection connection = null;
        OutputStream outputStream = null;

        try {
            if (needSignature) {
                String securityKey = DecryptUtil.encryptWithHmac(jsonStr);
                headerParamMap.put("X-TT-Signature", securityKey);
            } else {
                headerParamMap.remove("X-TT-Signature");
            }

            GzipInfo info = compress2Gzip(jsonStr);
            String contentLength = "0";
            byte[] writeBytes = info.bytes;
            if (writeBytes != null && writeBytes.length > 0) {
                contentLength = String.valueOf(writeBytes.length);
            } else {
                options.isGzip = false;
                try {
                    writeBytes = jsonStr.getBytes(CHARSET_UTF8);
                    contentLength = String.valueOf(writeBytes.length);
                } catch (Throwable ignore) {
                }
            }

            if (writeBytes == null || writeBytes.length == 0) {
                monitorGzipData(info);
                writeBytes = new byte[]{};
            }

            connection = connect(url, headerParamMap, options, "POST", contentLength);
            outputStream = connection.getOutputStream();
            outputStream.write(writeBytes);
            outputStream.flush();
            boolean redirect = shouldRedirect(connection.getResponseCode());
            if (redirect) {
                String redirectUrl = connection.getHeaderField("Location");
                IOUtils.close(connection);
                connection = connect(redirectUrl, headerParamMap, options, "POST", contentLength);
                outputStream = connection.getOutputStream();
                outputStream.write(writeBytes);
                outputStream.flush();
            }

            int httpCode = connection.getResponseCode();
            response.httpCode = httpCode;

            if (httpCode == HttpURLConnection.HTTP_OK) {
                String result = streamToString(connection.getInputStream());
                JSONObject json = JSON.build(result);
                if (json != null) {
                    response.body = json;
                    response.code = JSON.getInt(json, "code", -1);
                }

            }
        } catch (Throwable e) {
            response.throwable = e;
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            IOUtils.close(outputStream);
            IOUtils.close(connection);
        }

        response.duration = SystemClock.elapsedRealtime() - initTimeMS;

        monitorNetRequest(response);

        return response;
    }

    private static void monitorNetRequest(HttpResponse response) {
        if (response == null) {
            return;
        }

        try {
            //monitor ignore
            if (TextUtils.isEmpty(response.url) || response.url.contains(UrlConst.PATH_MONITOR)) {
                return;
            }

            String path = Uri.parse(response.url).getPath();

            JSONObject meta = TTUtil.getMetaWithTS(null);
            JSON.putInt(meta, "result", response.isOK() ? 0 : 1);
            JSON.putInt(meta, "err_code", response.getErrCode());
            JSON.putObject(meta, "err_msg", response.getErrMsg());
            JSON.putLong(meta, "duration", response.duration);
            JSON.putObject(meta, "path", path);
            JSON.putObject(meta, "req_id", JSON.getString(response.body, "request_id"));
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("network_req", meta, null);
        } catch (Throwable ignore) {
        }
    }

    private static void monitorGzipData(GzipInfo info) {
        if (info == null) {
            return;
        }

        try {
            JSONObject meta = TTUtil.getMetaWithTS(null);
            JSON.putInt(meta, "code", -1);
            JSON.putLong(meta, "duration", info.duration);
            String msg = "";
            if (info.throwable1 != null) {
                msg += info.throwable1.getMessage();
            }
            if (info.throwable2 != null) {
                msg += "==" + info.throwable2.getMessage();
            }
            JSON.putObject(meta, "err_msg", msg);
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("gzip_err", meta, null);
        } catch (Throwable ignore) {
        }
    }

    private static class GzipInfo {
        public long duration;
        public byte[] bytes;
        public Throwable throwable1;
        public Throwable throwable2;
    }

    @NonNull
    private static GzipInfo compress2Gzip(String requestBody) {
        long start = SystemClock.elapsedRealtime();
        GzipInfo info = new GzipInfo();

        if (TextUtils.isEmpty(requestBody)) {
            info.duration = SystemClock.elapsedRealtime() - start;
            info.throwable1 = new Exception("request body is empty");
            return info;
        }
        ByteArrayOutputStream outputStream = null;
        GZIPOutputStream gzipOutputStream = null;
        byte[] bytes = new byte[]{};
        try {
            outputStream = new ByteArrayOutputStream();
            gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(requestBody.getBytes(CHARSET_UTF8));
        } catch (Throwable e) {
            info.throwable1 = e;
        } finally {
            IOUtils.close(gzipOutputStream);
            if (outputStream != null) {
                try {
                    bytes = outputStream.toByteArray();
                    info.bytes = bytes;
                } catch (Throwable e) {
                    info.throwable2 = e;
                }
                IOUtils.close(outputStream);
            }
        }

        info.duration = SystemClock.elapsedRealtime() - start;

        return info;
    }

    private static String streamToString(InputStream is) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(is, CHARSET_UTF8));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Throwable e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_NET_ERROR);
        } finally {
            IOUtils.close(bufferedReader);
        }
        return null;
    }
}