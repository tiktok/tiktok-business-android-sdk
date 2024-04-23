/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import org.json.JSONObject;

public class TTPurchaseInfo {
    private final JSONObject purchase;
    private final JSONObject skuDetails;

    public static class InvalidTTPurchaseInfoException extends Exception {

        public InvalidTTPurchaseInfoException(String str) {
            super(str);
        }
    }

    /**
     * @param purchase for google billing v3, simply pass the purchase JSONObject.
     *                 for v4, you may try using new JSONObject(purchase.getOriginalJson()
     * @param skuDetails for google billing v3, simply pass the skuDetails JSONObject.
     *                   for v4, you may try using new JSONObject(skuDetails.getOriginalJson()
     * @throws InvalidTTPurchaseInfoException if either the purchase or the skuDetails object are not valid
     * or the productId does not match
     */
    public TTPurchaseInfo(JSONObject purchase, JSONObject skuDetails) throws InvalidTTPurchaseInfoException {
        if (!isValidPurchase(purchase)) {
            throw new InvalidTTPurchaseInfoException("Not a valid purchase object");
        }
        if (!isValidSkuDetails(skuDetails)) {
            throw new InvalidTTPurchaseInfoException("Not a valid skuDetails Object");
        }
        if (!purchase.optString("productId").equals(skuDetails.optString("productId"))) {
            throw new InvalidTTPurchaseInfoException("Product Id does not match");
        }
        this.purchase = purchase;
        this.skuDetails = skuDetails;
    }

    public JSONObject getPurchase() {
        return purchase;
    }

    public JSONObject getSkuDetails() {
        return skuDetails;
    }

    /**
     * {
     * "packageName":"com.example",
     * "acknowledged":false,
     * "orderId":"transactionId.android.test.purchased",
     * "productId":"android.test.purchased", // the same productId should also exist in the skuDetails
     * "developerPayload":"",
     * "purchaseTime":0,
     * "purchaseState":0,
     * "purchaseToken":"inapp:com.example:android.test.purchased"
     * }
     */
    private boolean isValidPurchase(JSONObject purchase) {
        return !purchase.isNull("orderId")
                && !purchase.isNull("productId");
    }

    /**
     *{
     *  "skuDetailsToken":"blahblah",
     *  "productId":"android.test.purchased",
     *  "type":"inapp",
     *  "price":"₹72.41",
     *  "price_amount_micros":72407614,
     *  "price_currency_code":"INR",
     *  "title":"Sample Title",
     *  "description":"Sample description for product: android.test.purchased."
     * }
     */
    private boolean isValidSkuDetails(JSONObject skuDetails) {
        return !skuDetails.isNull("price")
                && !skuDetails.isNull("productId");
    }
}
