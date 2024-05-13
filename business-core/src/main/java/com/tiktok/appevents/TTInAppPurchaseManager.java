/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import static com.tiktok.util.TTConst.TTSDK_EXCEPTION_SDK_CATCH;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TTInAppPurchaseManager {
    static final String TAG = TTInAppPurchaseManager.class.getName();

    /**
     * p
     */
    static JSONObject getPurchaseProps(TTPurchaseInfo purchaseInfo) {
        String productId = null;
        try {
            productId = purchaseInfo.getPurchase().getString("productId");
            JSONObject skuDetail = purchaseInfo.getSkuDetails();
            return getPurchaseProperties(productId, skuDetail);
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e, TTSDK_EXCEPTION_SDK_CATCH);
            return null;
        }
    }

    /**
     * returns content_id -> sku always
     */
    private static JSONObject getPurchaseProperties(String sku, JSONObject skuDetails) throws JSONException {
        JSONObject props = new JSONObject();
        JSONObject content = new JSONObject().put("content_id", sku);
        if (skuDetails != null) {
            content.put("content_type", safeJsonGetString(skuDetails, "type"));
            String currencyCode = safeJsonGetString(skuDetails, "price_currency_code");
            props.put("currency", currencyCode);
            content.put("quantity", 1);
            String price = safeJsonGetString(skuDetails, "price");
            double dPrice = 0;
            try {
                // trying to remove the currency symbol from price
                if (!currencyCode.equals("") && !price.equals("")) {
                    price = price.replace(",", "");
                    Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)");
                    Matcher matcher = regex.matcher(price);
                    if (matcher.find()) {
                        price = matcher.group(1);
                        dPrice = Double.parseDouble(price);
                    }
                }
            } catch (Exception ignored) {
            }
            content.put("price", dPrice);
            props.put("value", dPrice);
        }
        props.put("contents", new JSONArray().put(content));
        return props;
    }

    /**
     * safe get key from jsonobject
     *
     * @param jsonObject
     * @param key
     * @return
     */
    private static String safeJsonGetString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.get(key).toString();
        } catch (JSONException e) {
            return "";
        }
    }

}
