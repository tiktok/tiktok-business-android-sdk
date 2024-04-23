/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple container which makes serialization easier
 */
class TTAppEventPersist implements Serializable {

    public static final long serialVersionUID = 1L;

    private List<TTAppEvent> appEvents = new ArrayList<>();

    public void addEvents(List<TTAppEvent> appEventList) {
        if (appEventList == null || appEventList.isEmpty()) {
            return;
        }

        appEvents.addAll(appEventList);
    }

    public List<TTAppEvent> getAppEvents() {
        return appEvents;
    }

    public void setAppEvents(List<TTAppEvent> appEvents) {
        this.appEvents = appEvents;
    }

    public boolean isEmpty() {
        return appEvents.isEmpty();
    }

    @Override
    public String toString() {
        return "TTAppEventPersist{" +
                "appEvents=" + appEvents +
                '}';
    }
}
