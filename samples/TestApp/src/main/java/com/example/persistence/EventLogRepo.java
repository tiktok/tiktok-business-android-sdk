/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.persistence;

import static com.example.testdata.TestEvents.TTBaseEvents;
import static com.example.testdata.TestEvents.TTContentsEvent;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_ADD_TO_CARD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_ADD_TO_WISHLIST;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_CHECK_OUT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_PURCHASE;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_VIEW_CONTENT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENTS;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENT_ID;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENT_TYPE;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CURRENCY;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_DESCRIPTION;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_VALUE;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.model.EventLog;
import com.example.testdata.TestEvents;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.base.TTBaseEvent;
import com.tiktok.appevents.contents.TTAddToCartEvent;
import com.tiktok.appevents.contents.TTAddToWishlistEvent;
import com.tiktok.appevents.contents.TTCheckoutEvent;
import com.tiktok.appevents.contents.TTContentParams;
import com.tiktok.appevents.contents.TTContentsEvent;
import com.tiktok.appevents.contents.TTPurchaseEvent;
import com.tiktok.appevents.contents.TTViewContentEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventLogRepo {
    private final EventLogDao eventLogDao;
    private final LiveData<List<EventLog>> allEventLogs;

    public EventLogRepo(Application ctx) {
        PersistenceManager db = PersistenceManager.getDatabase(ctx);
        eventLogDao = db.eventLogDao();
        allEventLogs = eventLogDao.getAll();
    }

    public LiveData<List<EventLog>> getAllEventLogs() {
        return allEventLogs;
    }

    private static class getAllAsyncTask extends AsyncTask<Void, Void, List<EventLog>> {
        private final EventLogDao eventLogDao;

        getAllAsyncTask(EventLogDao dao) {
            eventLogDao = dao;
        }

        @Override
        protected List<EventLog> doInBackground(Void... voids) {
            return eventLogDao.getLogs();
        }
    }

    public List<EventLog> getLogs() throws ExecutionException, InterruptedException {
        return new getAllAsyncTask(eventLogDao).execute().get();
    }

    public void save(final EventLog eventLog, boolean oldApi) {
        try {
            if(oldApi){
                JSONObject props = new JSONObject(eventLog.properties);
                Iterator iterator = props.keys();
                JSONObject obj = new JSONObject();
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    obj.put(key, props.get(key));
                }
                TikTokBusinessSdk.trackEvent(eventLog.eventType, obj);
                PersistenceManager.databaseWriteExecutor.execute(() -> eventLogDao.save(eventLog));
                return;
            }
            JSONObject props = new JSONObject(eventLog.properties);
            if(TTBaseEvents.get(eventLog.eventType) != null){
                TikTokBusinessSdk.trackTTEvent(TTBaseEvents.get(eventLog.eventType));
            }else if(TTContentsEvent.contains(eventLog.eventType)){
                TTContentsEvent.Builder info = null;
                switch (eventLog.eventType){
                    case EVENT_NAME_ADD_TO_CARD:
                        info = TTAddToCartEvent.newBuilder();
                        break;
                    case EVENT_NAME_ADD_TO_WISHLIST:
                        info = TTAddToWishlistEvent.newBuilder();
                        break;
                    case EVENT_NAME_CHECK_OUT:
                        info = TTCheckoutEvent.newBuilder();
                        break;
                    case EVENT_NAME_PURCHASE:
                        info = TTPurchaseEvent.newBuilder();
                        break;
                    case EVENT_NAME_VIEW_CONTENT:
                        info = TTViewContentEvent.newBuilder();
                        break;
                }
                if(info != null){
                    TTContentParams[] params = null;
                    if(props.has(EVENT_PROPERTY_CONTENTS)){
                        JSONArray jsonArray = props.optJSONArray(EVENT_PROPERTY_CONTENTS);
                        params = new TTContentParams[jsonArray.length()];
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            params[i] = TTContentParams.newBuilder()
                                    .setContentId(jsonObject.optString("content_id"))
                                    .setContentCategory(jsonObject.optString("content_category"))
                                    .setBrand(jsonObject.optString("brand"))
                                    .setPrice((float) jsonObject.optDouble("price"))
                                    .setQuantity(jsonObject.optInt("quantity"))
                                    .setContentName(jsonObject.optString("content_name")).build();
                        }
                    }
                    info.setDescription(props.optString(EVENT_PROPERTY_DESCRIPTION))
                            .setCurrency(TestEvents.TTCurrency.get(props.optString(EVENT_PROPERTY_CURRENCY)))
                            .setValue(props.optDouble(EVENT_PROPERTY_VALUE))
                            .setContentId(props.optString(EVENT_PROPERTY_CONTENT_ID))
                            .setContentType(props.optString(EVENT_PROPERTY_CONTENT_TYPE));
                    if(params != null){
                        info.setContents(params);
                    }
                    TikTokBusinessSdk.trackTTEvent(info.build());
                }
            }else {
                Iterator iterator = props.keys();
                TTBaseEvent.Builder ttBaseEvent = TTBaseEvent.newBuilder(eventLog.eventType);
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    ttBaseEvent.addProperty(key, props.get(key));
                }
                TikTokBusinessSdk.trackTTEvent(ttBaseEvent.build());
            }
            PersistenceManager.databaseWriteExecutor.execute(() -> eventLogDao.save(eventLog));
        } catch (Exception ignored) {}
    }

    public void clear() {
        PersistenceManager.databaseWriteExecutor.execute(eventLogDao::clear);
    }
}
