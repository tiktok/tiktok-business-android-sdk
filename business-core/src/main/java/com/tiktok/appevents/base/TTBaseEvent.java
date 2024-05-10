/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents.base;


import org.json.JSONException;
import org.json.JSONObject;

public class TTBaseEvent {
    public JSONObject properties;
    public String eventName;
    public String eventId;

    public TTBaseEvent(String eventName, JSONObject properties, String eventId) {
        this.eventName = eventName;
        this.properties = properties;
        this.eventId = eventId;
    }

    public static Builder newBuilder(String eventName) {
        return new Builder(eventName);
    }

    public static Builder newBuilder(String eventName, String eventId) {
        return new Builder(eventName, eventId);
    }

    public static class Builder {
        public JSONObject properties = new JSONObject();
        public String eventName;
        public String eventId;
        public Builder(String eventName){
            this.eventName = eventName;
        }
        public Builder(String eventName, String eventId){
            this.eventName = eventName;
            this.eventId = eventId;
        }
        public Builder addProperty(String key, Object value) {
            safeAddProperty(key, value);
            return this;
        }

        public Builder addProperty(String key, String value) {
            safeAddProperty(key, value);
            return this;
        }

        public Builder addProperty(String key, boolean value) {
            safeAddProperty(key, value);
            return this;
        }

        public Builder addProperty(String key, double value) {
            safeAddProperty(key, value);
            return this;
        }

        public Builder addProperty(String key, int value) {
            safeAddProperty(key, value);
            return this;
        }

        public Builder addProperty(String key, long value) {
            safeAddProperty(key, value);
            return this;
        }

        private void safeAddProperty(String key, Object value) {
            try {
                properties.put(key, value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public TTBaseEvent build() {
            TTBaseEvent event = new TTBaseEvent(eventName, properties, eventId);
            return event;
        }
    }
}
