/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A more universal way to construct a purchase item
 */
public class TTPurchaseItem {
    private final double price;
    private final int quantity;
    // typically used for ecommerce and describes the retail item (e.g. women's watches)
    private final String contentType;
    // an ID for the content type (e.g. SKU)
    private final String contentId;

    public TTPurchaseItem(double price, int quantity, String contentType, String contentId) {
        this.price = price;
        this.quantity = quantity;
        this.contentType = contentType;
        this.contentId = contentId;
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject()
                .put("price", price)
                .put("quantity", quantity)
                .put("content_type", contentType)
                .put("content_id", contentId);
    }

    public static JSONObject buildPurchaseProperties(String currency, TTPurchaseItem... items) throws JSONException {
        double value = 0;
        JSONArray contents = new JSONArray();
        for (TTPurchaseItem item : items) {
            double price = item.price;
            int quantity = item.quantity;
            contents.put(item.toJSONObject());
            if (price != 0 && quantity != 0) {
                value += (price * quantity);
            }
        }
        return new JSONObject()
                .put("currency", currency)
                .put("value", value)
                .put("contents", contents);
    }
}