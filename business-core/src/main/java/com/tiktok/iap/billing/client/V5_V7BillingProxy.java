package com.tiktok.iap.billing.client;

import android.content.Context;
import android.text.TextUtils;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseInfo;
import com.tiktok.appevents.edp.EDPConfig;
import com.tiktok.appevents.edp.TTEDPEventTrack;
import com.tiktok.iap.TTInAppPurchaseWrapper;
import com.tiktok.iap.billing.GPBillVersions;
import com.tiktok.iap.billing.model.TTPayData;
import com.tiktok.util.JSON;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTReflect;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class V5_V7BillingProxy implements IBillingProxy {
    private static final String CLASS_BILLING_CLIENT = "com.android.billingclient.api.BillingClient";
    private static final String CLASS_BILLING_CLIENT_BUILDER = "com.android.billingclient.api.BillingClient$Builder";
    private static final String CLASS_BILLING_RESPONSE_CODE = "com.android.billingclient.api.BillingClient$BillingResponseCode";
    private static final String CLASS_SKU_TYPE = "com.android.billingclient.api.BillingClient$SkuType";
    private static final String CLASS_BILLING_CLIENT_STATE_LISTENER = "com.android.billingclient.api.BillingClientStateListener";
    private static final String CLASS_PURCHASES_UPDATED_LISTENER = "com.android.billingclient.api.PurchasesUpdatedListener";
    private static final String CLASS_SKU_DETAILS_PARAMS = "com.android.billingclient.api.SkuDetailsParams";
    private static final String CLASS_SKU_DETAILS_PARAMS_BUILDER = "com.android.billingclient.api.SkuDetailsParams$Builder";
    private static final String CLASS_SKU_DETAILS_RESPONSE_LISTENER = "com.android.billingclient.api.SkuDetailsResponseListener";
    private static final String CLASS_QUERY_PURCHASE_HISTORY_PARAMS = "com.android.billingclient.api.QueryPurchaseHistoryParams";
    private static final String CLASS_QUERY_PURCHASE_HISTORY_PARAMS_BUILDER = "com.android.billingclient.api.QueryPurchaseHistoryParams$Builder";
    private static final String CLASS_PURCHASE_HISTORY_RESPONSE_LISTENER = "com.android.billingclient.api.PurchaseHistoryResponseListener";
    private static final String CLASS_QUERY_PRODUCT_DETAILS_PARAMS = "com.android.billingclient.api.QueryProductDetailsParams";
    private static final String CLASS_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER = "com.android.billingclient.api.QueryProductDetailsParams$Builder";
    private static final String CLASS_QUERY_PRODUCT_DETAILS_PRODUCT = "com.android.billingclient.api.QueryProductDetailsParams$Product";
    private static final String CLASS_QUERY_PRODUCT_DETAILS_PRODUCT_BUILDER = "com.android.billingclient.api.QueryProductDetailsParams$Product$Builder";
    private static final String CLASS_PRODUCT_DETAILS_RESPONSE_LISTENER = "com.android.billingclient.api.ProductDetailsResponseListener";

    private static final TTLogger ttLogger = new TTLogger("BillingProxyV5", TikTokBusinessSdk.getLogLevel());

    private final AtomicBoolean mIsInitLoading = new AtomicBoolean(false);
    private final AtomicBoolean mInitSuccess = new AtomicBoolean(false);
    private volatile Object mBillingClient;

    private final Map<String, TTPayData> mHistorySubs = new ConcurrentHashMap<>();
    private final Map<String, TTPayData> mHistoryInApp = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> mProductDetails = new ConcurrentHashMap<>();

    private final Object mUpdateListener = createPurchasesUpdatedListener();

    @Override
    public GPBillVersions.GPBillingVer getVersion() {
        return GPBillVersions.GPBillingVer.V5_V7;
    }

    @Override
    public void init() {
        tryCreateAndStartBillingClient();
    }

    private boolean isAutoIAPTrackEnable() {
        return TTInAppPurchaseWrapper.autoTrackPaymentEnable;
    }

    private void sendPurchase(Object billingResult, List<?> list) {
        if (billingResult == null || list == null || !isOkResponse(billingResult)) {
            return;
        }

        if (isAutoIAPTrackEnable()) {
            for (Object purchase : list) {
                if (purchase == null) {
                    continue;
                }
                List<String> skus = getPurchaseSkus(purchase);
                if (skus == null || skus.isEmpty()) {
                    continue;
                }
                querySkuAndTrack(skus, purchase, true);
            }
        }
    }

    private void querySkuAndTrack(List<String> skus, Object purchase, boolean isInAppPurchase) {
        try {
            Object params = buildSkuDetailsParams(skus, isInAppPurchase);
            if (params == null) {
                sendNoSkuIapTrack(skus, purchase);
                return;
            }
            Object listener = createSkuDetailsResponseListener(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    try {
                        if (method == null || !"onSkuDetailsResponse".equals(method.getName()) || args == null || args.length < 2) {
                            return null;
                        }
                        Object billingResult = args[0];
                        List<?> skuDetailsList = castList(args[1]);
                        if (billingResult != null && isOkResponse(billingResult) && skuDetailsList != null) {
                            if (skuDetailsList.size() > 0) {
                                List<TTPurchaseInfo> purchaseInfos = new ArrayList<>();
                                try {
                                    for (Object skuDetails : skuDetailsList) {
                                        try {
                                            TTPurchaseInfo purchaseInfo = new TTPurchaseInfo(
                                                    JSON.build(getOriginalJson(purchase)),
                                                    JSON.build(getOriginalJson(skuDetails)));
                                            purchaseInfo.setAutoTrack(true);
                                            purchaseInfo.setSubs(!isInAppPurchase);
                                            purchaseInfos.add(purchaseInfo);
                                        } catch (Throwable ignore) {
                                        }
                                    }
                                    TikTokBusinessSdk.trackGooglePlayPurchase(purchaseInfos);
                                } catch (Throwable e) {
                                    ttLogger.error(e, "query Sku And Track google play purchase error");
                                }
                            } else if (isInAppPurchase) {
                                querySkuAndTrack(skus, purchase, false);
                            } else {
                                sendNoSkuIapTrack(skus, purchase);
                            }
                        } else {
                            sendNoSkuIapTrack(skus, purchase);
                        }
                    } catch (Throwable e) {
                        ttLogger.error(e, "query Sku And Track error");
                    }
                    return null;
                }
            });
            invokeBillingClientMethod("querySkuDetailsAsync",
                    new Class<?>[]{getClassByName(CLASS_SKU_DETAILS_PARAMS), getClassByName(CLASS_SKU_DETAILS_RESPONSE_LISTENER)},
                    params, listener);
        } catch (Throwable e) {
            ttLogger.error(e, "query Sku And Track error2");
        }
    }

    private static void sendNoSkuIapTrack(List<String> skus, Object purchase) {
        try {
            JSONArray contents = JSON.buildArr();
            for (String sku : skus) {
                if (sku == null || sku.isEmpty()) {
                    continue;
                }
                JSONObject item = JSON.build();
                JSON.putInt(item, "quantity", getPurchaseQuantity(purchase));
                JSON.putObject(item, "content_id", sku);

                JSON.putArr(contents, item);
            }
            JSONObject content = JSON.build();
            JSON.putObject(content, "contents", contents);
            TikTokBusinessSdk.trackEvent("Purchase", content);
        } catch (Throwable e) {
            ttLogger.error(e, "Track Purchase error");
        }
    }

    private void sendPageShow(Object billingResult, List<?> list) {
        if (billingResult == null || list == null) {
            return;
        }
        try {
            if (EDPConfig.enable_pay_show_track) {
                JSONArray arrPurchase = JSON.buildArr();
                for (Object purchase : list) {
                    JSONObject json = JSON.build(getOriginalJson(purchase));
                    JSON.putArr(arrPurchase, json);
                }
                TTEDPEventTrack.trackPayShow(getBillingResponseCode(billingResult), arrPurchase);
            }
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void queryPurchaseHistory() {
        if (!TTInAppPurchaseWrapper.autoTrackPaymentHistory) {
            return;
        }
        if (!isStartSuccess()) {
            tryCreateAndStartBillingClient();
            return;
        }

        try {
            doQueryPurchaseHistory();
            TTInAppPurchaseWrapper.hasReportedHistoryInLife = true;
        } catch (Throwable ignore) {
        }
    }

    private void doQueryPurchaseHistory() {
        try {
            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackINAPP()) {
                Object paramsINAPP = buildQueryPurchaseHistoryParams(false);
                Object listener = createPurchaseHistoryResponseListener(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method == null || !"onPurchaseHistoryResponse".equals(method.getName()) || args == null || args.length < 2) {
                            return null;
                        }
                        if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackINAPP()) {
                            queryProductDetailHistory(false, castList(args[1]));
                        }
                        return null;
                    }
                });
                invokeBillingClientMethod("queryPurchaseHistoryAsync",
                        new Class<?>[]{getClassByName(CLASS_QUERY_PURCHASE_HISTORY_PARAMS), getClassByName(CLASS_PURCHASE_HISTORY_RESPONSE_LISTENER)},
                        paramsINAPP, listener);
            }
        } catch (Throwable e) {
            ttLogger.error(e, "query h inapp error");
        }


        try {
            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackSUBS()) {
                Object paramsSUBS = buildQueryPurchaseHistoryParams(true);
                Object listener = createPurchaseHistoryResponseListener(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method == null || !"onPurchaseHistoryResponse".equals(method.getName()) || args == null || args.length < 2) {
                            return null;
                        }
                        if (isOkResponse(args[0])) {
                            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackSUBS()) {
                                queryProductDetailHistory(true, castList(args[1]));
                            }
                        }
                        return null;
                    }
                });
                invokeBillingClientMethod("queryPurchaseHistoryAsync",
                        new Class<?>[]{getClassByName(CLASS_QUERY_PURCHASE_HISTORY_PARAMS), getClassByName(CLASS_PURCHASE_HISTORY_RESPONSE_LISTENER)},
                        paramsSUBS, listener);
            }
        } catch (Throwable e) {
            ttLogger.error(e, "query h subs error");
        }
    }

    private void queryProductDetailHistory(boolean isSubs, List<?> list) {
        try {
            if (list == null || list.isEmpty()) {
                return;
            }

            final List<String> idList = new ArrayList<>();
            for (Object history : list) {
                try {
                    String data = getOriginalJson(history);
                    JSONObject json = JSON.build(data);
                    String pid = JSON.getString(json, "productId");
                    if (!TextUtils.isEmpty(pid)) {
                        checkDataAndAddNeedParam(json, null);
                        TTPayData pay = new TTPayData();
                        pay.productId = pid;
                        pay.data = json;
                        pay.purchaseTime = getPurchaseTime(history);
                        if (isSubs) {
                            mHistorySubs.put(pid, pay);
                        } else {
                            mHistoryInApp.put(pid, pay);
                        }

                        if (!mProductDetails.containsKey(pid)) {
                            idList.add(pid);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }

            if (idList.isEmpty()) {
                //直接上报
                tryUploadHistoryLog();
            } else {
                doQueryProductDetails(isSubs, idList);
            }
        } catch (Throwable e) {
            ttLogger.error(e, "query h product details error");
        }
    }

    private void tryUploadHistoryLog() {
        if (!TTInAppPurchaseWrapper.autoTrackPaymentHistory) {
            return;
        }
        if (mHistorySubs.isEmpty() && mHistoryInApp.isEmpty() && mProductDetails.isEmpty()) {
            return;
        }

        try {
            if (TTInAppPurchaseWrapper.canTrackSUBS()) {
                Map<String, TTPayData> map = new HashMap<>(mHistorySubs);
                map = filterPurchase(true, map);
                sendHistoryLog(true, map);
            }
        } catch (Throwable ignore) {
        }


        if (TTInAppPurchaseWrapper.canTrackINAPP()) {
            Map<String, TTPayData> map = new HashMap<>(mHistoryInApp);
            map = filterPurchase(false, map);
            sendHistoryLog(false, map);
        }

        mHistorySubs.clear();
        mHistoryInApp.clear();
    }

    private void sendHistoryLog(boolean isSubs, Map<String, TTPayData> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            long maxTime = 0;
            List<TTPurchaseInfo> list = new ArrayList<>();
            for (Map.Entry<String, TTPayData> entry : map.entrySet()) {
                try {
                    String pid = entry.getKey();
                    TTPayData payData = entry.getValue();
                    JSONObject sku = mProductDetails.get(pid);
                    if (sku != null && sku.length() > 0) {
                        checkDataAndAddNeedParam(payData.data, sku);
                        TTPurchaseInfo info = new TTPurchaseInfo(payData.data, sku);
                        info.setAutoTrack(true);
                        info.setSubs(isSubs);
                        list.add(info);

                        maxTime = Math.max(maxTime, payData.purchaseTime);
                    }
                } catch (Throwable e) {
                    ttLogger.error(e, "send history error");
                }
            }
            if (!list.isEmpty()) {
                TikTokBusinessSdk.getAppEventLogger().trackPurchase(true, list);

                if (maxTime > 0) {
                    if (isSubs) {
                        BillCache.getInstance().saveSUBSLast(maxTime);
                    } else {
                        BillCache.getInstance().saveINAPPLast(maxTime);
                    }
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private void checkDataAndAddNeedParam(JSONObject purchase, JSONObject sku) {
        if (purchase != null && purchase.isNull("orderId")) {
            JSON.putObject(purchase, "orderId", "");
        }
        if (sku != null && sku.isNull("price")) {
            JSON.putObject(sku, "price", "");
        }
    }

    private Map<String, TTPayData> filterPurchase(boolean isSubs, Map<String, TTPayData> map) {
        Map<String, TTPayData> filterMap = new HashMap<>();

        try {
            // filter purchase time
            if (map != null && !map.isEmpty()) {
                long last = isSubs ? BillCache.getInstance().getSUBSLast() : BillCache.getInstance().getINAPPLast();
                for (Map.Entry<String, TTPayData> entry : map.entrySet()) {
                    try {
                        TTPayData pay = entry.getValue();
                        if (pay != null) {
                            if (pay.purchaseTime > last) {
                                filterMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            //filter total number
            List<TTPayData> allPays = new ArrayList<>();
            for (Map.Entry<String, TTPayData> entry : filterMap.entrySet()) {
                if (entry != null && entry.getValue() != null) {
                    allPays.add(entry.getValue());
                }
            }

            //按照时间倒序
            Collections.sort(allPays, new Comparator<TTPayData>() {
                @Override
                public int compare(TTPayData o1, TTPayData o2) {
                    return o1 == null || o2 == null ? 0 : Long.valueOf(o2.purchaseTime - o1.purchaseTime).intValue();
                }
            });

            filterMap = new HashMap<>();
            int total = isSubs ? TTInAppPurchaseWrapper.autoTrackPaymentHistorySUBS : TTInAppPurchaseWrapper.autoTrackPaymentHistoryINAPP;
            total = Math.min(allPays.size(), total);
            for (int i = 0; i < total; i++) {
                try {
                    TTPayData pay = allPays.get(i);
                    if (pay != null && !TextUtils.isEmpty(pay.productId)) {
                        filterMap.put(pay.productId, pay);
                    }
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }

        return filterMap;
    }

    private void doQueryProductDetails(boolean isSubs, List<String> idList) {
        Object params = buildQueryProductDetailsParams(isSubs, idList);
        if (params == null) {
            return;
        }
        Object listener = createProductDetailsResponseListener(new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (method == null || !"onProductDetailsResponse".equals(method.getName()) || args == null || args.length < 2) {
                    return null;
                }
                if (isOkResponse(args[0])) {
                    List<?> list = extractProductDetails(args[1]);
                    if (list != null && !list.isEmpty()) {
                        for (Object detail : list) {
                            try {
                                if (detail != null) {
                                    String jsonStr = BillUtils.parserJsonFromProductDetail(String.valueOf(detail));
                                    JSONObject json = JSON.build(jsonStr);
                                    if (json != null && json.length() > 0) {
                                        checkDataAndAddNeedParam(null, json);
                                        mProductDetails.put(getProductId(detail), json);
                                    }
                                }
                            } catch (Throwable ignore) {
                            }
                        }

                        tryUploadHistoryLog();
                    }
                }
                return null;
            }
        });
        invokeBillingClientMethod("queryProductDetailsAsync",
                new Class<?>[]{getClassByName(CLASS_QUERY_PRODUCT_DETAILS_PARAMS), getClassByName(CLASS_PRODUCT_DETAILS_RESPONSE_LISTENER)},
                params, listener);
    }

    private boolean isStartSuccess() {
        return !mIsInitLoading.get() && mInitSuccess.get() && mBillingClient != null && invokeBooleanMethod(mBillingClient, "isReady");
    }

    private void tryCreateAndStartBillingClient() {
        if (isStartSuccess()) {
            return;
        }

        mIsInitLoading.set(true);

        try {
            Object builder = callStaticMethod(CLASS_BILLING_CLIENT, "newBuilder", new Class<?>[]{Context.class}, TikTokBusinessSdk.getApplicationContext());
            if (builder == null) {
                throw new IllegalStateException("billing builder is null");
            }
            callMethod(builder, "setListener", new Class<?>[]{getClassByName(CLASS_PURCHASES_UPDATED_LISTENER)}, mUpdateListener);
            callMethod(builder, "enablePendingPurchases", new Class<?>[0]);
            mBillingClient = callMethod(builder, "build", new Class<?>[0]);
            Object stateListener = createBillingClientStateListener();
            invokeBillingClientMethod("startConnection", new Class<?>[]{getClassByName(CLASS_BILLING_CLIENT_STATE_LISTENER)}, stateListener);
        } catch (Throwable e) {
            ttLogger.error(e, "billing client init error");

            mIsInitLoading.set(false);
            mInitSuccess.set(false);
        }
    }

    private Object createPurchasesUpdatedListener() {
        return createListener(CLASS_PURCHASES_UPDATED_LISTENER, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                try {
                    if (method == null || !"onPurchasesUpdated".equals(method.getName()) || args == null || args.length != 2) {
                        return null;
                    }
                    Object billingResult = args[0];
                    List<?> list = castList(args[1]);
                    sendPageShow(billingResult, list);
                    sendPurchase(billingResult, list);
                    ttLogger.info("on billing result: " + String.valueOf(billingResult));
                } catch (Throwable ignore) {
                }
                return null;
            }
        });
    }

    private Object createBillingClientStateListener() {
        return createListener(CLASS_BILLING_CLIENT_STATE_LISTENER, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (method == null) {
                    return null;
                }
                if ("onBillingServiceDisconnected".equals(method.getName())) {
                    mIsInitLoading.set(false);
                    mInitSuccess.set(false);
                    return null;
                }
                if ("onBillingSetupFinished".equals(method.getName()) && args != null && args.length > 0) {
                    mIsInitLoading.set(false);
                    mInitSuccess.set(isOkResponse(args[0]));
                }
                return null;
            }
        });
    }

    private Object createSkuDetailsResponseListener(InvocationHandler handler) {
        return createListener(CLASS_SKU_DETAILS_RESPONSE_LISTENER, handler);
    }

    private Object createPurchaseHistoryResponseListener(InvocationHandler handler) {
        return createListener(CLASS_PURCHASE_HISTORY_RESPONSE_LISTENER, handler);
    }

    private Object createProductDetailsResponseListener(InvocationHandler handler) {
        return createListener(CLASS_PRODUCT_DETAILS_RESPONSE_LISTENER, handler);
    }

    private Object createListener(String className, InvocationHandler handler) {
        try {
            Class<?> listenerClass = getClassByName(className);
            if (listenerClass == null) {
                return null;
            }
            return Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, handler);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private Object buildSkuDetailsParams(List<String> skus, boolean isInAppPurchase) {
        List<String> skuList = new ArrayList<>();
        for (String sku : skus) {
            if (!TextUtils.isEmpty(sku)) {
                skuList.add(sku);
            }
        }
        if (skuList.isEmpty()) {
            return null;
        }
        Object builder = callStaticMethod(CLASS_SKU_DETAILS_PARAMS, "newBuilder", new Class<?>[0]);
        if (builder == null) {
            return null;
        }
        callMethod(builder, "setSkusList", new Class<?>[]{List.class}, skuList);
        callMethod(builder, "setType", new Class<?>[]{String.class}, getSkuType(isInAppPurchase));
        return callMethod(builder, "build", new Class<?>[0]);
    }

    private Object buildQueryPurchaseHistoryParams(boolean isSubs) {
        Object builder = callStaticMethod(CLASS_QUERY_PURCHASE_HISTORY_PARAMS, "newBuilder", new Class<?>[0]);
        if (builder == null) {
            return null;
        }
        callMethod(builder, "setProductType", new Class<?>[]{String.class}, getProductType(isSubs));
        return callMethod(builder, "build", new Class<?>[0]);
    }

    private Object buildQueryProductDetailsParams(boolean isSubs, List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return null;
        }
        List<Object> products = new ArrayList<>();
        for (String pid : idList) {
            if (TextUtils.isEmpty(pid)) {
                continue;
            }
            Object productBuilder = callStaticMethod(CLASS_QUERY_PRODUCT_DETAILS_PRODUCT, "newBuilder", new Class<?>[0]);
            if (productBuilder == null) {
                continue;
            }
            callMethod(productBuilder, "setProductType", new Class<?>[]{String.class}, getProductType(isSubs));
            callMethod(productBuilder, "setProductId", new Class<?>[]{String.class}, pid);
            Object product = callMethod(productBuilder, "build", new Class<?>[0]);
            if (product != null) {
                products.add(product);
            }
        }
        if (products.isEmpty()) {
            return null;
        }
        Object paramsBuilder = callStaticMethod(CLASS_QUERY_PRODUCT_DETAILS_PARAMS, "newBuilder", new Class<?>[0]);
        if (paramsBuilder == null) {
            return null;
        }
        callMethod(paramsBuilder, "setProductList", new Class<?>[]{List.class}, products);
        return callMethod(paramsBuilder, "build", new Class<?>[0]);
    }

    private void invokeBillingClientMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        callMethod(mBillingClient, methodName, parameterTypes, args);
    }

    private static Object callStaticMethod(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        Method method = TTReflect.getMethod(className, methodName, parameterTypes);
        return TTReflect.callMethod(method, null, args);
    }

    private static Object callMethod(Object receiver, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (receiver == null) {
            return null;
        }
        Method method = TTReflect.getMethod(receiver.getClass(), methodName, parameterTypes);
        return TTReflect.callMethod(method, receiver, args);
    }

    private static Class<?> getClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String getSkuType(boolean isInAppPurchase) {
        return String.valueOf(TTReflect.on(CLASS_SKU_TYPE).findField(isInAppPurchase ? "INAPP" : "SUBS").getValue(null));
    }

    private static String getProductType(boolean isSubs) {
        return String.valueOf(TTReflect.on(CLASS_SKU_TYPE).findField(isSubs ? "SUBS" : "INAPP").getValue(null));
    }

    private static int getBillingResponseCode(Object billingResult) {
        Object code = callMethod(billingResult, "getResponseCode", new Class<?>[0]);
        return code instanceof Integer ? (Integer) code : Integer.MIN_VALUE;
    }

    private static boolean isOkResponse(Object billingResult) {
        Object okCode = TTReflect.on(CLASS_BILLING_RESPONSE_CODE).findField("OK").getValue(null);
        return okCode instanceof Integer && getBillingResponseCode(billingResult) == (Integer) okCode;
    }

    private static boolean invokeBooleanMethod(Object receiver, String methodName) {
        Object value = callMethod(receiver, methodName, new Class<?>[0]);
        return value instanceof Boolean && (Boolean) value;
    }

    @SuppressWarnings("unchecked")
    private static List<?> castList(Object value) {
        return value instanceof List ? (List<?>) value : null;
    }

    private static String getOriginalJson(Object value) {
        Object json = callMethod(value, "getOriginalJson", new Class<?>[0]);
        return json instanceof String ? (String) json : null;
    }

    private static List<String> getPurchaseSkus(Object purchase) {
        Object skus = callMethod(purchase, "getSkus", new Class<?>[0]);
        if (skus instanceof List) {
            List<?> values = (List<?>) skus;
            List<String> result = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof String) {
                    result.add((String) value);
                }
            }
            return result;
        }
        return null;
    }

    private static int getPurchaseQuantity(Object purchase) {
        Object quantity = callMethod(purchase, "getQuantity", new Class<?>[0]);
        return quantity instanceof Integer ? (Integer) quantity : 0;
    }

    private static long getPurchaseTime(Object purchaseOrHistory) {
        Object time = callMethod(purchaseOrHistory, "getPurchaseTime", new Class<?>[0]);
        return time instanceof Long ? (Long) time : 0L;
    }

    private static String getProductId(Object detail) {
        Object pid = callMethod(detail, "getProductId", new Class<?>[0]);
        return pid instanceof String ? (String) pid : null;
    }

    @SuppressWarnings("unchecked")
    private static List<?> extractProductDetails(Object result) {
        if (result instanceof List) {
            return (List<?>) result;
        }
        Object list = callMethod(result, "getProductDetailsList", new Class<?>[0]);
        return list instanceof List ? (List<?>) list : null;
    }

}
