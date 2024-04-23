/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.model.EventLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {EventLog.class}, version = 1)
public abstract class PersistenceManager extends RoomDatabase {

    abstract EventLogDao eventLogDao();

    private static volatile PersistenceManager INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            // Executors.newSingleThreadExecutor();

    static PersistenceManager getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PersistenceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PersistenceManager.class, "sample-app-store")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
