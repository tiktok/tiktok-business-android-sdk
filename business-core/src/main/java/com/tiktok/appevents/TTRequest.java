/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import android.text.TextUtils;

import com.tiktok.BuildConfig;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;
import com.tiktok.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class TTRequest {
    private static final String TAG = TTRequest.class.getCanonicalName();
    private static final TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    private static final int MAX_EVENT_SIZE = 50;

    // stats for the current batch
    private static int toBeSentRequests = 0;
    private static int failedRequests = 0;
    private static int successfulRequests = 0;

    // stats for the whole lifecycle
    private static final TreeSet<Long> allRequestIds = new TreeSet<>();
    private static final List<TTAppEvent> successfullySentRequests = new ArrayList<>();

    private static final Map<String, String> headParamMap = new HashMap<>();
    private static final Map<String, String> getHeadParamMap = new HashMap<>();

    static {
        // these fields wont change, so cache it locally to enhance performance
        headParamMap.put("Content-Type", "application/json");
        headParamMap.put("Connection", "Keep-Alive");

        String ua = String.format("tiktok-business-android-sdk/%s/%s",
                BuildConfig.VERSION_NAME,
                TikTokBusinessSdk.getApiAvailableVersion());
        headParamMap.put("User-Agent", ua);
        // no content-type application/json for get requests
        getHeadParamMap.put("Connection", "Keep-Alive");
        getHeadParamMap.put("User-Agent", ua);
    }

    public static JSONObject getBusinessSDKConfig(Map<String, Object> options) {
        long initTimeMS = System.currentTimeMillis();
//        TikTokBusinessSdk.getAppEventLogger().monitorMetric("config_api_start", TTUtil.getMetaWithTS(initTimeMS), null);
        logger.info("Try to fetch global configs");
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("app_id", TikTokBusinessSdk.getAppId());
        // the rest params are for the sake of simplicity of debugging
        paramsMap.put("client", "android");
        paramsMap.put("sdk_version", SystemInfoUtil.getSDKVersion());
        //  for fix bug in lower Android API edition. Maybe there is something wrong with language package, url can not be parsed successfully with some special char
        //  paramsMap.put("app_name", SystemInfoUtil.getAppName());
        paramsMap.put("app_version", SystemInfoUtil.getAppVersionName());
        paramsMap.put("tiktok_app_id", TikTokBusinessSdk.getFirstTTAppIds());
        paramsMap.putAll(options);

        String url = TTUtil.mapToString("https://business-api.tiktok.com/open_api/business_sdk_config/get/", paramsMap);
        logger.debug(url);
        String result = HttpRequestUtil.doGet(url, getHeadParamMap);
        logger.debug(result);
        JSONObject config = null;
        if (result != null) {
            try {
                JSONObject resultJson = new JSONObject(result);
                Integer code = (Integer) resultJson.get("code");
                if (code == 0) {
                    config = (JSONObject) resultJson.get("data");
                }
                logger.info("Global config fetched: " + TTUtil.ppStr(config));
            } catch (Exception e) {
                // might be api returning something wrong
                TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
            }
        }
        try {
            long endTimeMS = System.currentTimeMillis();
            JSONObject meta = TTUtil.getMetaWithTS(initTimeMS)
                    .put("latency", endTimeMS-initTimeMS)
                    .put("success", config != null)
                    .put("log_id", HttpRequestUtil.getLogIDFromApi(result));
            TikTokBusinessSdk.getAppEventLogger().monitorMetric("config_api", meta, null);
        } catch (Exception ignored) {}
        // might be api returning something wrong
        return config;
    }

    // for debugging purpose
    public static synchronized List<TTAppEvent> getSuccessfullySentRequests() {
        return successfullySentRequests;
    }

    /**
     * Try to send events to api with MTU set to 1000 app events,
     * If there are more than 1000 events, they will be split into several chunks and
     * then be sent separately,
     * Any failed events will be accumulated and finally returned.
     *
     * @param appEventList
     * @return the accumulation of all failed events
     */
    public static synchronized List<TTAppEvent> reportAppEvent(JSONObject basePayload, List<TTAppEvent> appEventList) {
        TTUtil.checkThread(TAG);
        if (appEventList == null || appEventList.size() == 0) {
            return new ArrayList<>();
        }

        toBeSentRequests = appEventList.size();
        for (TTAppEvent event : appEventList) {
            allRequestIds.add(event.getUniqueId());
        }
        failedRequests = 0;
        successfulRequests = 0;
        notifyChange();
        //  dynamic req domain and version
        String url = "https://" + TikTokBusinessSdk.getApiTrackDomain() + "/open_api/" + TikTokBusinessSdk.getApiAvailableVersion() + "/app/batch/";

        List<TTAppEvent> failedEventsToBeSaved = new ArrayList<>();
        List<TTAppEvent> failedEventsToBeDiscarded = new ArrayList<>();

        List<List<TTAppEvent>> chunks = averageAssign(appEventList, MAX_EVENT_SIZE);

        for (List<TTAppEvent> currentBatch : chunks) {
//            long initTimeMS = System.currentTimeMillis();
//            try {
//                JSONObject initMeta = TTUtil.getMetaWithTS(initTimeMS)
//                        .put("size", currentBatch.size())
//                        .put("total", appEventList.size());
//                TikTokBusinessSdk.getAppEventLogger().monitorMetric("track_api_start", initMeta, null);
//            } catch (Exception ignored) {}

            List<JSONObject> batch = new ArrayList<>();
            for (TTAppEvent event : currentBatch) {
                JSONObject propertiesJson = transferJson(event);
                if (propertiesJson == null) {
                    continue;
                }
                batch.add(propertiesJson);
            }

            JSONObject bodyJson = basePayload;
            try {
                bodyJson.put("batch", new JSONArray(batch));
            } catch (Exception e) {
                failedEventsToBeSaved.addAll(currentBatch);
                TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
                continue;
            }

            try {
                String bodyStr = bodyJson.toString(4);
                logger.debug("To Api:\n" + bodyStr);
            } catch (JSONException ignored) {}

            String result = HttpRequestUtil.doPost(url, headParamMap, bodyJson.toString());

            if (result == null) {
                failedEventsToBeSaved.addAll(currentBatch);
                failedRequests += currentBatch.size();
            } else {
                try {
                    JSONObject resultJson = new JSONObject(result);
                    int code = resultJson.getInt("code");

                    if (TikTokBusinessSdk.isInSdkDebugMode() || code == TTConst.ApiErrorCodes.API_ERROR.code) {
                        failedEventsToBeDiscarded.addAll(currentBatch);
                        failedRequests += currentBatch.size();
                    }
                    // some events made it while others not.
                    else if (code == TTConst.ApiErrorCodes.PARTIAL_SUCCESS.code) {
                        try {
                            JSONArray partialFailedEvents = resultJson.getJSONObject("data").getJSONArray("failed_events");
                            int length = partialFailedEvents.length();
                            Set<Integer> failedIndices = new HashSet<>();
                            for (int i = 0; i < length; i++) {
                                JSONObject errorObj = partialFailedEvents.getJSONObject(i);
                                failedIndices.add(errorObj.getInt("order_in_batch"));
                            }
                            int totalSize = currentBatch.size();
                            for (int i = 0; i < totalSize; i++) {
                                TTAppEvent curr = currentBatch.get(i);
                                if (failedIndices.contains(i)) {
                                    failedEventsToBeDiscarded.add(curr);
                                    failedRequests += 1;
                                } else {
                                    successfullySentRequests.add(curr);
                                    successfulRequests += 1;
                                }
                            }
                        } catch (Exception e) {
                            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
                            failedEventsToBeSaved.addAll(currentBatch);
                            failedRequests += currentBatch.size();
                        }
                    } else if (code != 0) {
                        failedEventsToBeSaved.addAll(currentBatch);
                        failedRequests += currentBatch.size();
                    } else {
                        successfulRequests += currentBatch.size();
                        successfullySentRequests.addAll(currentBatch);
                    }
                } catch (JSONException e) {
                    failedRequests += currentBatch.size();
                    failedEventsToBeSaved.addAll(currentBatch);
                    TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
                }
                logger.debug(TTUtil.ppStr(result));
            }
            notifyChange();

//            long endTimeMS = System.currentTimeMillis();
//            try {
//                JSONObject endMeta = TTUtil.getMetaWithTS(endTimeMS)
//                        .put("size", currentBatch.size())
//                        .put("total", appEventList.size())
//                        .put("log_id", HttpRequestUtil.getLogIDFromApi(result))
//                        .put("latency", endTimeMS-initTimeMS)
//                        .put("status_code", HttpRequestUtil.getCodeFromApi(result))
//                        .put("success", result != null);
//                TikTokBusinessSdk.getAppEventLogger().monitorMetric("track_api_end", endMeta, null);
//            } catch (Exception ignored) {}
        }
        logger.debug("Flushed %d events successfully", successfulRequests);

        // might be due to network disconnection
        if (failedEventsToBeSaved.size() != 0) {
            logger.debug("Failed to flush %d events, will save them to disk", failedEventsToBeSaved.size());
        }
        // api returns some unrecoverable error
        int discardedEventCount = failedEventsToBeDiscarded.size();
        if (discardedEventCount != 0) {
            logger.debug("Failed to flush " + discardedEventCount + " events, will discard them");
            TTAppEventLogger.totalDumped += discardedEventCount;
            if (TikTokBusinessSdk.diskListener != null) {
                TikTokBusinessSdk.diskListener.onDumped(TTAppEventLogger.totalDumped);
            }
        }
        logger.debug("Failed to flush %d events in total", failedRequests);

        toBeSentRequests = 0;
        failedRequests = 0;
        successfulRequests = 0;
        notifyChange();
        return failedEventsToBeSaved;
    }

    private static void notifyChange() {
        if (TikTokBusinessSdk.networkListener != null) {
            TikTokBusinessSdk.networkListener.onNetworkChange(toBeSentRequests, successfulRequests,
                    failedRequests, allRequestIds.size() + TTAppEventsQueue.size(), successfullySentRequests.size());
        }
    }

    private static JSONObject transferJson(TTAppEvent event) {
        if (event == null) {
            return null;
        }
        try {
            JSONObject propertiesJson = new JSONObject();
            propertiesJson.put("type", event.getType());
            if (event.getEventName() != null) {
                propertiesJson.put("event", event.getEventName());
            }
            if (!TextUtils.isEmpty(event.getEventId())) {
                propertiesJson.put("event_id", event.getEventId());
            }
            propertiesJson.put("timestamp", TimeUtil.getISO8601Timestamp(event.getTimeStamp()));
            if (TikTokBusinessSdk.isInSdkLDUMode()) {
                propertiesJson.put("limited_data_use", true);
            }
            JSONObject properties = new JSONObject(event.getPropertiesJson());
            if (properties.length() != 0) {
                propertiesJson.put("properties", properties);
            }
            propertiesJson.put("context", TTRequestBuilder.getContextForApi(event));
            return propertiesJson;
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
            return null;
        }
    }

    /**
     * split event list
     *
     * @param sourceList
     * @param splitNum
     * @param <T>
     */
    public static <T> List<List<T>> averageAssign(List<T> sourceList, int splitNum) {
        List<List<T>> result = new ArrayList<>();

        int size = sourceList.size();
        int times = size % splitNum == 0 ? size / splitNum : size / splitNum + 1;
        for (int i = 0; i < times; i++) {
            int start = i * splitNum;
            int end = i * splitNum + splitNum;
            result.add(new ArrayList<>(sourceList.subList(start, Math.min(size, end))));
        }
        return result;
    }

    public static String reportMonitorEvent(JSONObject stat) {
        String url = "https://" + TikTokBusinessSdk.getApiTrackDomain() + "/open_api/" + TikTokBusinessSdk.getApiAvailableVersion() + "/app/monitor/";
        return HttpRequestUtil.doPost(url, headParamMap, stat.toString());
    }
}