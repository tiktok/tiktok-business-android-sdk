/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.iap;

import static com.tiktok.TikTokBusinessSdk.getApplicationContext;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseInfo;
import com.tiktok.util.TTLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TTInAppPurchaseWrapper {

    private static Context mContext;
    private static BillingClient mBillingClient;
    static final String TAG = TTInAppPurchaseWrapper.class.getName();

    private static final TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    public static void registerIapTrack() {
        try {
            if (getApplicationContext() == null) {
                return;
            }
            mContext = getApplicationContext();
            PurchasesUpdatedListener purchaseUpdateListener = (billingResult, purchases) -> {

                if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && purchases != null) {
                    for (Purchase purchase : purchases) {
                        if (purchase == null) {
                            continue;
                        }
                        List<String> skus = purchase.getSkus();
                        if (skus == null || skus.size() == 0) {
                            continue;
                        }
                        querySkuAndTrack(skus, purchase, true);
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    ttLogger.info("user canceled");
                } else {
                    ttLogger.info("otherErr : %s", billingResult.getDebugMessage());
                }
            };
            mBillingClient = BillingClient.newBuilder(mContext)
                    .setListener(purchaseUpdateListener)
                    .enablePendingPurchases()
                    .build();
            startBillingClient();
        } catch (Throwable ignored) {
            ttLogger.error(ignored, "register Iap track error");
        }
    }

    public static void startBillingClient() {
        try {
            if (mBillingClient == null || mBillingClient.isReady()) {
                return;
            }
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        ttLogger.info("billing setup finished");
                    } else {
                        ttLogger.info("billing setup error %s", billingResult.getDebugMessage());
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    ttLogger.info("billing service disconnected");
                }
            });
        } catch (Throwable ignored) {
            ttLogger.error(ignored, "start billing client connection error");
        }
    }

    private static void querySkuAndTrack(List<String> skus, Purchase purchase, boolean isInAppPurchase) {
        try {
            List<String> skuList = new ArrayList<>();
            for (String sku : skus) {
                if (sku == null || sku.isEmpty()) {
                    continue;
                }
                skuList.add(sku);
            }
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (isInAppPurchase) {
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            } else {
                params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
            }
            mBillingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
                if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && skuDetailsList != null) {
                    if (skuDetailsList.size() > 0) {
                        List<TTPurchaseInfo> purchaseInfos = new ArrayList<>();
                        try {
                            for (SkuDetails skuDetails : skuDetailsList) {
                                purchaseInfos.add(new TTPurchaseInfo(new JSONObject(purchase.getOriginalJson()),
                                        new JSONObject(skuDetails.getOriginalJson())));
                            }
                            TikTokBusinessSdk.trackGooglePlayPurchase(purchaseInfos);
                        } catch (Throwable e) {
                            ttLogger.error(e, "query Sku And Track google play purchase error");
                        }
                    } else {
                        if (isInAppPurchase) {
                            querySkuAndTrack(skus, purchase, false);
                        } else {
                            sendNoSkuIapTrack(skus, purchase);
                        }
                    }
                } else {
                    sendNoSkuIapTrack(skus, purchase);
                }
            });
        } catch (Throwable ignored) {
            ttLogger.error(ignored, "query Sku And Track error");
        }
    }

    private static void sendNoSkuIapTrack(List<String> skus, Purchase purchase) {
        try {
            JSONArray contents = new JSONArray();
            for (String sku : skus) {
                if (sku == null || sku.isEmpty()) {
                    continue;
                }
                JSONObject item = new JSONObject()
                        .put("quantity", purchase.getQuantity())
                        .put("content_id", sku);
                contents.put(item);
            }
            JSONObject content = new JSONObject().put("contents", contents);
            TikTokBusinessSdk.trackEvent("Purchase", content);
        } catch (Throwable ignored) {
            ttLogger.error(ignored, "Track Purchase error");
        }
    }
}
