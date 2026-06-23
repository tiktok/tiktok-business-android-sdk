package com.tiktok.iap.billing.client;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseInfo;
import com.tiktok.appevents.edp.EDPConfig;
import com.tiktok.appevents.edp.TTEDPEventTrack;
import com.tiktok.iap.TTInAppPurchaseWrapper;
import com.tiktok.iap.billing.GPBillVersions;
import com.tiktok.iap.billing.model.TTPayData;
import com.tiktok.util.JSON;
import com.tiktok.util.TTLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class V8BillingProxy implements IBillingProxy {
    private static final TTLogger ttLogger = new TTLogger("BillingProxyV8", TikTokBusinessSdk.getLogLevel());

    private final AtomicBoolean mIsInitLoading = new AtomicBoolean(false);
    private final AtomicBoolean mInitSuccess = new AtomicBoolean(false);
    private volatile BillingClient mBillingClient;
    private final GPBillVersions.GPBillingVer mBillingVersion;

    private final Map<String, TTPayData> mHistorySubs = new ConcurrentHashMap<>();
    private final Map<String, TTPayData> mHistoryInApp = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> mProductDetails = new ConcurrentHashMap<>();

    private final PurchasesUpdatedListener mUpdateListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
            sendPageShow(billingResult, list);
            sendPurchase(billingResult, list);
            ttLogger.info("on billing result: " + String.valueOf(billingResult));
        }
    };

    public V8BillingProxy(GPBillVersions.GPBillingVer ver) {
        mBillingVersion = ver;
    }

    @Override
    public GPBillVersions.GPBillingVer getVersion() {
        return mBillingVersion;
    }

    @Override
    public void init() {
        tryCreateAndStartBillingClient();
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

    private boolean isAutoIAPTrackEnable() {
        return TTInAppPurchaseWrapper.autoTrackPaymentEnable;
    }

    private void sendPurchase(BillingResult billingResult, List<Purchase> list) {
        if (billingResult == null || list == null
                || billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            return;
        }

        if (isAutoIAPTrackEnable()) {
            for (Purchase purchase : list) {
                if (purchase == null) {
                    continue;
                }
                List<String> productIds = purchase.getProducts();
                if (productIds == null || productIds.isEmpty()) {
                    continue;
                }
                queryProductDetailsAndTrack(productIds, purchase, true);
            }
        }
    }

    private void queryProductDetailsAndTrack(List<String> productIds, Purchase purchase, boolean isInAppPurchase) {
        try {
            QueryProductDetailsParams params = buildProductDetailsParams(productIds, isInAppPurchase);
            if (params == null) {
                sendNoSkuIapTrack(productIds, purchase);
                return;
            }
            mBillingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
                try {
                    List<ProductDetails> detailList = result == null ? null : result.getProductDetailsList();
                    if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && detailList != null && !detailList.isEmpty()) {
                        List<TTPurchaseInfo> purchaseInfos = new ArrayList<>();
                        Set<String> resolvedIds = new HashSet<>();
                        for (ProductDetails detail : detailList) {
                            try {
                                JSONObject detailJson = V8ProductDetailCompat.toCompatDetailJson(detail, !isInAppPurchase);
                                if (detailJson == null || detailJson.length() == 0) {
                                    continue;
                                }
                                TTPurchaseInfo purchaseInfo = new TTPurchaseInfo(buildPurchaseJson(purchase, detail.getProductId()), detailJson);
                                purchaseInfo.setAutoTrack(true);
                                purchaseInfo.setSubs(!isInAppPurchase);
                                purchaseInfos.add(purchaseInfo);
                                resolvedIds.add(detail.getProductId());
                            } catch (Throwable ignore) {
                            }
                        }
                        if (!purchaseInfos.isEmpty()) {
                            TikTokBusinessSdk.trackGooglePlayPurchase(purchaseInfos);
                        }
                        List<String> unresolvedIds = getUnresolvedProductIds(productIds, resolvedIds);
                        if (unresolvedIds.isEmpty()) {
                            return;
                        }
                        if (isInAppPurchase) {
                            queryProductDetailsAndTrack(unresolvedIds, purchase, false);
                        } else {
                            sendNoSkuIapTrack(unresolvedIds, purchase);
                        }
                        return;
                    }
                    if (isInAppPurchase) {
                        queryProductDetailsAndTrack(productIds, purchase, false);
                    } else {
                        sendNoSkuIapTrack(productIds, purchase);
                    }
                } catch (Throwable e) {
                    ttLogger.error(e, "query ProductDetails And Track error");
                    sendNoSkuIapTrack(productIds, purchase);
                }
            });
        } catch (Throwable e) {
            ttLogger.error(e, "query ProductDetails And Track error2");
            sendNoSkuIapTrack(productIds, purchase);
        }
    }

    private QueryProductDetailsParams buildProductDetailsParams(List<String> productIds, boolean isInAppPurchase) {
        if (productIds == null || productIds.isEmpty()) {
            return null;
        }

        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String productId : productIds) {
            if (TextUtils.isEmpty(productId)) {
                continue;
            }
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(isInAppPurchase ? BillingClient.ProductType.INAPP : BillingClient.ProductType.SUBS)
                    .build());
        }
        if (products.isEmpty()) {
            return null;
        }
        return QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();
    }

    private JSONObject buildPurchaseJson(Purchase purchase, String productId) {
        JSONObject json = JSON.build(purchase == null ? null : purchase.getOriginalJson());
        if (json == null) {
            json = JSON.build();
        }
        JSON.putObject(json, "productId", productId);
        if (purchase != null) {
            JSON.putLong(json, "purchaseTime", purchase.getPurchaseTime());
            if (json.isNull("quantity")) {
                JSON.putInt(json, "quantity", purchase.getQuantity());
            }
        }
        if (json.isNull("orderId")) {
            JSON.putObject(json, "orderId", "");
        }
        return json;
    }

    private static void sendNoSkuIapTrack(List<String> productIds, Purchase purchase) {
        try {
            JSONArray contents = JSON.buildArr();
            for (String productId : productIds) {
                if (TextUtils.isEmpty(productId)) {
                    continue;
                }
                JSONObject item = JSON.build();
                JSON.putInt(item, "quantity", purchase == null ? 0 : purchase.getQuantity());
                JSON.putObject(item, "content_id", productId);
                JSON.putArr(contents, item);
            }
            JSONObject content = JSON.build();
            JSON.putObject(content, "contents", contents);
            TikTokBusinessSdk.trackEvent("Purchase", content);
        } catch (Throwable e) {
            ttLogger.error(e, "Track Purchase error");
        }
    }

    private void sendPageShow(BillingResult billingResult, List<Purchase> list) {
        if (billingResult == null || list == null) {
            return;
        }
        try {
            if (EDPConfig.enable_pay_show_track) {
                JSONArray arrPurchase = JSON.buildArr();
                for (Purchase purchase : list) {
                    JSONObject json = JSON.build(purchase.getOriginalJson());
                    JSON.putArr(arrPurchase, json);
                }
                TTEDPEventTrack.trackPayShow(billingResult.getResponseCode(), arrPurchase);
            }
        } catch (Throwable ignore) {
        }
    }

    private void doQueryPurchaseHistory() {
        // Billing v8 no longer exposes the old purchase-history callback path.
        // The accepted product trade-off here is to backfill from currently owned / active purchases only,
        // while still preserving BillCache-based dedupe and trimming semantics on top of that narrower source set.
        try {
            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackINAPP()) {
                QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
                mBillingClient.queryPurchasesAsync(params, new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && TTInAppPurchaseWrapper.autoTrackPaymentHistory
                                && TTInAppPurchaseWrapper.canTrackINAPP()) {
                            queryProductDetailHistory(false, list);
                        }
                    }
                });
            }
        } catch (Throwable e) {
            ttLogger.error(e, "query v8 inapp error");
        }

        try {
            if (TTInAppPurchaseWrapper.autoTrackPaymentHistory && TTInAppPurchaseWrapper.canTrackSUBS()) {
                QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();
                mBillingClient.queryPurchasesAsync(params, new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && TTInAppPurchaseWrapper.autoTrackPaymentHistory
                                && TTInAppPurchaseWrapper.canTrackSUBS()) {
                            queryProductDetailHistory(true, list);
                        }
                    }
                });
            }
        } catch (Throwable e) {
            ttLogger.error(e, "query v8 subs error");
        }
    }

    private void queryProductDetailHistory(boolean isSubs, List<Purchase> list) {
        try {
            if (list == null || list.isEmpty()) {
                return;
            }

            List<String> idList = new ArrayList<>();
            Set<String> added = new HashSet<>();
            for (Purchase purchase : list) {
                try {
                    if (purchase == null) {
                        continue;
                    }
                    List<String> products = purchase.getProducts();
                    if (products == null || products.isEmpty()) {
                        continue;
                    }
                    for (String productId : products) {
                        if (TextUtils.isEmpty(productId)) {
                            continue;
                        }
                        JSONObject json = buildPurchaseJson(purchase, productId);
                        TTPayData pay = new TTPayData();
                        pay.productId = productId;
                        pay.data = json;
                        pay.purchaseTime = purchase.getPurchaseTime();
                        if (isSubs) {
                            mHistorySubs.put(productId, pay);
                        } else {
                            mHistoryInApp.put(productId, pay);
                        }
                        if (!mProductDetails.containsKey(productId) && added.add(productId)) {
                            idList.add(productId);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }

            if (idList.isEmpty()) {
                tryUploadHistoryLog();
            } else {
                doQueryProductDetails(isSubs, idList);
            }
        } catch (Throwable e) {
            ttLogger.error(e, "query v8 product details error");
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
                List<String> sentIds = sendHistoryLog(true, map);
                for (String sentId : sentIds) {
                    mHistorySubs.remove(sentId);
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            if (TTInAppPurchaseWrapper.canTrackINAPP()) {
                Map<String, TTPayData> map = new HashMap<>(mHistoryInApp);
                map = filterPurchase(false, map);
                List<String> sentIds = sendHistoryLog(false, map);
                for (String sentId : sentIds) {
                    mHistoryInApp.remove(sentId);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private List<String> sendHistoryLog(boolean isSubs, Map<String, TTPayData> map) {
        List<String> sentIds = new ArrayList<>();
        if (map == null || map.isEmpty()) {
            return sentIds;
        }

        try {
            long maxTime = 0;
            List<TTPurchaseInfo> list = new ArrayList<>();
            for (Map.Entry<String, TTPayData> entry : map.entrySet()) {
                try {
                    String productId = entry.getKey();
                    TTPayData payData = entry.getValue();
                    JSONObject detailJson = mProductDetails.get(productId);
                    if (detailJson != null && detailJson.length() > 0) {
                        JSONObject purchaseJson = payData == null ? null : payData.data;
                        checkDataAndAddNeedParam(purchaseJson, detailJson, productId);
                        TTPurchaseInfo info = new TTPurchaseInfo(purchaseJson, detailJson);
                        info.setAutoTrack(true);
                        info.setSubs(isSubs);
                        list.add(info);
                        sentIds.add(productId);
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
        return sentIds;
    }

    private void checkDataAndAddNeedParam(JSONObject purchase, JSONObject detailJson, String productId) {
        if (purchase != null) {
            JSON.putObject(purchase, "productId", productId);
            if (purchase.isNull("orderId")) {
                JSON.putObject(purchase, "orderId", "");
            }
            if (purchase.isNull("quantity")) {
                JSON.putInt(purchase, "quantity", 1);
            }
        }
        if (detailJson != null) {
            JSON.putObject(detailJson, "productId", productId);
            if (detailJson.isNull("price")) {
                JSON.putObject(detailJson, "price", "");
            }
            if (detailJson.isNull("price_amount_micros")) {
                JSON.putLong(detailJson, "price_amount_micros", 0L);
            }
            if (detailJson.isNull("price_currency_code")) {
                JSON.putObject(detailJson, "price_currency_code", "");
            }
        }
    }

    private Map<String, TTPayData> filterPurchase(boolean isSubs, Map<String, TTPayData> map) {
        long last = isSubs ? BillCache.getInstance().getSUBSLast() : BillCache.getInstance().getINAPPLast();
        int total = isSubs ? TTInAppPurchaseWrapper.autoTrackPaymentHistorySUBS : TTInAppPurchaseWrapper.autoTrackPaymentHistoryINAPP;
        return filterPurchaseByTimeAndLimit(map, last, total);
    }

    static Map<String, TTPayData> filterPurchaseByTimeAndLimit(Map<String, TTPayData> map, long last, int total) {
        Map<String, TTPayData> filterMap = new HashMap<>();

        try {
            if (map != null && !map.isEmpty()) {
                for (Map.Entry<String, TTPayData> entry : map.entrySet()) {
                    try {
                        TTPayData pay = entry.getValue();
                        if (pay != null && pay.purchaseTime > last) {
                            filterMap.put(entry.getKey(), pay);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        try {
            List<TTPayData> allPays = new ArrayList<>();
            for (Map.Entry<String, TTPayData> entry : filterMap.entrySet()) {
                if (entry != null && entry.getValue() != null) {
                    allPays.add(entry.getValue());
                }
            }
            Collections.sort(allPays, new Comparator<TTPayData>() {
                @Override
                public int compare(TTPayData o1, TTPayData o2) {
                    return o1 == null || o2 == null ? 0 : Long.compare(o2.purchaseTime, o1.purchaseTime);
                }
            });

            filterMap = new HashMap<>();
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
        QueryProductDetailsParams params = buildProductDetailsParams(idList, !isSubs);
        if (params == null) {
            tryUploadHistoryLog();
            return;
        }

        mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull com.android.billingclient.api.QueryProductDetailsResult result) {
                if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<ProductDetails> list = result.getProductDetailsList();
                    if (list != null && !list.isEmpty()) {
                        for (ProductDetails detail : list) {
                            try {
                                if (detail != null) {
                                    JSONObject json = V8ProductDetailCompat.toCompatDetailJson(detail, isSubs);
                                    if (json != null && json.length() > 0) {
                                        checkDataAndAddNeedParam(null, json, detail.getProductId());
                                        mProductDetails.put(detail.getProductId(), json);
                                    }
                                }
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                    tryUploadHistoryLog();
                }
            }
        });
    }

    private List<String> getUnresolvedProductIds(List<String> requestedIds, Set<String> resolvedIds) {
        List<String> unresolvedIds = new ArrayList<>();
        if (requestedIds == null || requestedIds.isEmpty()) {
            return unresolvedIds;
        }
        for (String requestedId : requestedIds) {
            if (!TextUtils.isEmpty(requestedId) && (resolvedIds == null || !resolvedIds.contains(requestedId))) {
                unresolvedIds.add(requestedId);
            }
        }
        return unresolvedIds;
    }

    private boolean isStartSuccess() {
        return !mIsInitLoading.get() && mInitSuccess.get() && mBillingClient != null && mBillingClient.isReady();
    }

    private void tryCreateAndStartBillingClient() {
        if (isStartSuccess()) {
            return;
        }

        mIsInitLoading.set(true);
        try {
            mBillingClient = BillingClient.newBuilder(TikTokBusinessSdk.getApplicationContext())
                    .setListener(mUpdateListener)
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .enablePrepaidPlans()
                            .build())
                    .build();
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    mIsInitLoading.set(false);
                    mInitSuccess.set(false);
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    mIsInitLoading.set(false);
                    mInitSuccess.set(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK);
                }
            });
        } catch (Throwable e) {
            ttLogger.error(e, "billing client init error");
            mIsInitLoading.set(false);
            mInitSuccess.set(false);
        }
    }
}
