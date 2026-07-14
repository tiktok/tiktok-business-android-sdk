/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.util.TTConst.TRACK_TYPE;
import static com.tiktok.util.TTConst.TRACK_TYPE_AUTO;

import android.text.TextUtils;

import com.tiktok.iap.billing.GPBillVersions;
import com.tiktok.util.JSON;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;

class TTInAppPurchaseManager {


    static JSONObject getPurchaseProps(TTPurchaseInfo purchaseInfo) {
        try {
            JSONObject properties = JSON.build();

            if (purchaseInfo.isAutoTrack()) {
                JSON.putObject(properties, TRACK_TYPE, TRACK_TYPE_AUTO);
            }

            JSON.putObject(properties, "currency", JSON.getString(purchaseInfo.getSkuDetails(), "price_currency_code"));
            JSON.putDouble(properties, "value", getPrice(purchaseInfo.getSkuDetails()));
            JSON.putDouble(properties, "code", JSON.getInt(purchaseInfo.getPurchase(), "purchaseState", 1));

            //original json
            JSONObject original = JSON.build();
            JSON.putObject(original, "purchase", purchaseInfo.getPurchase());
            JSON.putObject(original, "sku", purchaseInfo.getSkuDetails());
            JSON.putObject(original, "gp_ver", GPBillVersions.getVersion());
            JSON.putObject(properties, "original_json", original);

            //contents
            JSONArray arrContents = getContents(purchaseInfo);
            JSON.putObject(properties, "contents", arrContents);

            //order
            JSONObject order = getOrder(purchaseInfo);
            JSON.putObject(properties, "order", order);

            return properties;
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static JSONObject getOrder(TTPurchaseInfo purchaseInfo) {
        JSONObject order = JSON.build();

        try {
            JSON.putObject(order, "order_id", JSON.getString(purchaseInfo.getPurchase(), "orderId"));
            JSON.putLong(order, "order_time", JSON.getLong(purchaseInfo.getPurchase(), "purchaseTime"));

            //token
            String fallbackToken = JSON.getString(purchaseInfo.getPurchase(), "purchaseToken");
            String token = JSON.getString(purchaseInfo.getPurchase(), "token", fallbackToken);
            JSON.putObject(order, "order_token", token);

            //auto renewing
            JSON.putObject(order, "is_auto_renewing", JSON.getBoolean(purchaseInfo.getPurchase(), "autoRenewing", false));
        } catch (Throwable ignore) {
        }

        return order;
    }

    private static JSONArray getContents(TTPurchaseInfo purchaseInfo) {
        JSONArray contentArr = JSON.buildArr();

        try {
            JSONObject content = JSON.build();


            JSON.putObject(content, "content_id", JSON.getString(purchaseInfo.getPurchase(), "productId"));
            JSON.putObject(content, "content_type", purchaseInfo.isSubs() ? "SUB" : "SKU");
            JSON.putInt(content, "quantity", JSON.getInt(purchaseInfo.getPurchase(), "quantity"));
            JSON.putDouble(content, "price", getPrice(purchaseInfo.getSkuDetails()));
            JSON.putObject(content, "title", JSON.getString(purchaseInfo.getSkuDetails(), "title"));
            JSON.putObject(content, "description", JSON.getString(purchaseInfo.getSkuDetails(), "description"));
            JSON.putObject(content, "subscription_period", JSON.getString(purchaseInfo.getSkuDetails(), "subscriptionPeriod"));
            JSON.putInt(content, "subscription_period_number", JSON.getInt(purchaseInfo.getSkuDetails(), "subscriptionPeriodNumber"));
            JSON.putObject(content, "free_trial_period", JSON.getString(purchaseInfo.getSkuDetails(), "freeTrialPeriod"));

            //offers
            JSONArray offerArr = JSON.buildArr();
            JSONObject offer = JSON.build();
            JSON.putObject(offer, "offer_id", JSON.getString(purchaseInfo.getSkuDetails(), "offer_id"));
            JSON.putObject(offer, "type", JSON.getString(purchaseInfo.getSkuDetails(), "offer_type"));
            JSON.putObject(offer, "price", JSON.getString(purchaseInfo.getSkuDetails(), "price"));
            if (!TextUtils.isEmpty(JSON.getString(purchaseInfo.getSkuDetails(), "freeTrialPeriod"))) {
                JSON.putObject(offer, "payment_mode", "pay_as_you_go");
            }

            JSON.putArr(offerArr, offer);
            JSON.putObject(content, "offers", offerArr);


            JSON.putArr(contentArr, content);
        } catch (Throwable ignore) {
        }

        return contentArr;
    }

    private static double getPrice(JSONObject skuDetails) {
        double price = 0;
        try {
            price = BigDecimal.valueOf(JSON.getLong(skuDetails, "price_amount_micros", 0) / 1000000.0).doubleValue();
        } catch (Throwable ignored) {
        }
        return price;
    }

}
