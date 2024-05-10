/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.eventlog;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.model.EventLog;
import com.example.persistence.EventLogRepo;

import java.util.List;

public class EventLogViewModel extends AndroidViewModel {

    private EventLogRepo eventLogRepo;

    public EventLogViewModel(Application application) {
        super(application);
        eventLogRepo = new EventLogRepo(application);
    }

    public LiveData<List<EventLog>> getAllEventLogs() { return eventLogRepo.getAllEventLogs(); }

    public void save(EventLog eventLog, boolean oldApi) {
        eventLogRepo.save(eventLog, oldApi);
    }
}