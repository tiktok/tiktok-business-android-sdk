/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.persistence;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.model.EventLog;
import com.tiktok.TikTokBusinessSdk;

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

    public void save(final EventLog eventLog) {
        try {
            JSONObject props = new JSONObject(eventLog.properties);
            Iterator iterator = props.keys();
            JSONObject obj = new JSONObject();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                obj.put(key, props.get(key));
            }
            TikTokBusinessSdk.trackEvent(eventLog.eventType, obj);
            PersistenceManager.databaseWriteExecutor.execute(() -> eventLogDao.save(eventLog));
        } catch (Exception ignored) {}
    }

    public void clear() {
        PersistenceManager.databaseWriteExecutor.execute(eventLogDao::clear);
    }
}
