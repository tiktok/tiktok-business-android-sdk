/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "event_logs")
public class EventLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "event_type")
    public String eventType;

    @NonNull
    @ColumnInfo(name = "properties")
    public String properties;

    @NonNull
    @ColumnInfo(name = "created_at")
    public String createdAt;

    public EventLog(@NonNull String eventType, @NonNull String properties) {
        this.eventType = eventType;
        this.properties = properties;
        this.createdAt = new Date().toString();
    }
}
