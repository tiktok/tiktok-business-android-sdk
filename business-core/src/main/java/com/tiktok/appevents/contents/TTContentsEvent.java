/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents.contents;

import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENTS;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENT_ID;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENT_TYPE;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CURRENCY;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_DESCRIPTION;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_VALUE;

import android.text.TextUtils;

import com.tiktok.appevents.base.TTBaseEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TTContentsEvent extends TTBaseEvent {
    TTContentsEvent(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }

    public static class Builder extends TTBaseEvent.Builder{

        Builder(String eventName, String eventId) {
            super(eventName, eventId);
        }

        public Builder setDescription(String description) {
            if(!TextUtils.isEmpty(description)){
                addProperty(EVENT_PROPERTY_DESCRIPTION, description);
            }
            return this;
        }

        public Builder setCurrency(TTContentsEventConstants.Currency currency) {
            if(currency != null) {
                addProperty(EVENT_PROPERTY_CURRENCY, currency);
            }
            return this;
        }

        public Builder setValue(double value) {
            safeAddProperty(EVENT_PROPERTY_VALUE, value);
            return this;
        }

        public Builder setContentType(String contentType) {
            if(!TextUtils.isEmpty(contentType)) {
                addProperty(EVENT_PROPERTY_CONTENT_TYPE, contentType);
            }
            return this;
        }

        public Builder setContentId(String contentId) {
            if(!TextUtils.isEmpty(contentId)) {
                addProperty(EVENT_PROPERTY_CONTENT_ID, contentId);
            }
            return this;
        }

        public Builder setContents(TTContentParams... contents) {
            if (contents != null) {
                JSONArray jsonArray = new JSONArray();
                for (TTContentParams content : contents) {
                    if(content != null) {
                        jsonArray.put(content.toJSONObject());
                    }
                }
                addProperty(EVENT_PROPERTY_CONTENTS, jsonArray);
            }
            return this;
        }

        private void safeAddProperty(String key, Object value) {
            try {
                properties.put(key, value);
            } catch (Throwable e) {}
        }

        public TTContentsEvent build() {
            TTContentsEvent event = new TTContentsEvent(eventName, properties, eventId);
            return event;
        }
    }
}
