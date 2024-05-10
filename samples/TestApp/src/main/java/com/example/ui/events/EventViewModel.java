/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.events;

import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENTS;

import android.app.Application;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;

public class EventViewModel extends AndroidViewModel {

    private MutableLiveData<String> eventName;
    private MutableLiveData<JSONObject> properties;

    public EventViewModel(Application application) {
        super(application);
        eventName = new MutableLiveData<>();
        properties = new MutableLiveData<>();
        this.resetProps();
    }

    void resetEvent() {
        eventName.setValue("");
    }

    void setEvent(String name) {
        eventName.setValue(name);
    }

    void resetProps() {
        properties.setValue(new JSONObject());
    }

    void setProps(JSONObject js) {
        properties.setValue(js);
    }

    void addProp(String key, Object value) throws JSONException {
        JSONObject newProp = properties.getValue();
        assert newProp != null;
        newProp.put(key, value);
        properties.setValue(newProp);
    }

    void addContents(Object value) throws JSONException {
        JSONObject newProp = properties.getValue();
        if(newProp.optJSONArray(EVENT_PROPERTY_CONTENTS) != null){
            newProp.getJSONArray(EVENT_PROPERTY_CONTENTS).put(value);
        } else {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(value);
            newProp.put(EVENT_PROPERTY_CONTENTS, jsonArray);
        }
        properties.setValue(newProp);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    String getProp(String key) throws JSONException {
        return String.valueOf(Objects.requireNonNull(properties.getValue()).get(key));
    }

    LiveData<JSONObject> getLiveProperties() {
        return properties;
    }

    MutableLiveData<String> getLiveEventName() {
        return eventName;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Iterator<String> getPropIterator() {
        return Objects.requireNonNull(properties.getValue()).keys();
    }

}