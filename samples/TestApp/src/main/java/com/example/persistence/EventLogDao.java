/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.model.EventLog;

import java.util.List;

@Dao
public interface EventLogDao {
    @Query("SELECT * FROM event_logs ORDER BY id DESC")
    LiveData<List<EventLog>> getAll();

    @Query("SELECT * FROM event_logs")
    List<EventLog> getLogs();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void save(EventLog log);

    @Query("DELETE FROM event_logs")
    void clear();
}
