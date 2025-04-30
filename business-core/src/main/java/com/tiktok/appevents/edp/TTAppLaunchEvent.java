/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents.edp;

import org.json.JSONObject;

public class TTAppLaunchEvent {
    private JSONObject prop;
    private long ts;

    public TTAppLaunchEvent(JSONObject prop, long ts) {
        this.prop = prop;
        this.ts = ts;
    }

    public JSONObject getProp() {
        return prop;
    }

    public void setProp(JSONObject prop) {
        this.prop = prop;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }
}
