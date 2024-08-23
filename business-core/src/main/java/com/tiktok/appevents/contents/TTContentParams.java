/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents.contents;


import android.text.TextUtils;

import org.json.JSONObject;

public class TTContentParams {
    private float price;
    private int quantity;
    private String contentId;
    private String contentCategory;
    private String contentName;
    private String brand;
    private boolean priceAvailable = false;
    private boolean quantityAvailable = false;

    public static TTContentParams.Builder newBuilder() {
        return new TTContentParams.Builder();
    }

    public static class Builder {
        private float price = Float.NaN;
        private int quantity = -1;
        private String contentId;
        private String contentCategory;
        private String contentName;
        private String brand;
        private boolean priceAvailable = false;
        private boolean quantityAvailable = false;

        public Builder setPrice(float price) {
            this.price = price;
            priceAvailable = true;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            quantityAvailable = true;
            return this;
        }

        public Builder setContentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        public Builder setContentCategory(String contentCategory) {
            this.contentCategory = contentCategory;
            return this;
        }

        public Builder setContentName(String contentName) {
            this.contentName = contentName;
            return this;
        }

        public Builder setBrand(String brand) {
            this.brand = brand;
            return this;
        }

        public TTContentParams build() {
            TTContentParams params = new TTContentParams();
            params.price = price;
            params.priceAvailable = priceAvailable;
            params.quantity = quantity;
            params.quantityAvailable = quantityAvailable;
            params.contentId = contentId;
            params.contentCategory = contentCategory;
            params.contentName = contentName;
            params.brand = brand;
            return params;
        }
    }

    public JSONObject toJSONObject(){
        JSONObject jsonObject = null;
        try{
            jsonObject = new JSONObject();
            if(quantityAvailable) {
                jsonObject.put("quantity", quantity);
            }
            if(!TextUtils.isEmpty(contentId)){
                jsonObject.put("content_id", contentId);
            }
            if(!TextUtils.isEmpty(contentCategory)) {
                jsonObject.put("content_category", contentCategory);
            }
            if(!TextUtils.isEmpty(contentName)) {
                jsonObject.put("content_name", contentName);
            }
            if(!TextUtils.isEmpty(brand)) {
                jsonObject.put("brand", brand);
            }
            if (priceAvailable && price != Float.NaN) {
                jsonObject.put("price", Double.parseDouble(String.valueOf(price)));
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
        return jsonObject;
    }
}
