/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.events;

import static com.example.testdata.TestEvents.TTBaseEvents;
import static com.example.ui.events.PropEditActivity.SOURCE;
import static com.example.ui.events.PropEditActivity.SOURCE_CONTENT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENTS;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.model.EventLog;
import com.example.testdata.TestEvents;
import com.example.ui.eventlog.EventLogViewModel;
import com.tiktok.TikTokBusinessSdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class EventFragment extends Fragment {

    private EventViewModel eventViewModel;
    private EventLogViewModel eventLogViewModel;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        eventLogViewModel = new ViewModelProvider(this).get(EventLogViewModel.class);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String eventType = bundle.getString("event_type");
            String eventProps = bundle.getString("event_props");
            assert eventType != null;
            assert eventProps != null;
            eventViewModel.setEvent(eventType);
            try {
                JSONObject object = new JSONObject(eventProps);
                Iterator iterator = object.keys();
                while (iterator.hasNext()) {
                    String prop = (String) iterator.next();
                    eventViewModel.addProp(prop, object.getString(prop));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        View root = inflater.inflate(R.layout.fragment_events, container, false);

        final TextView propsTV = root.findViewById(R.id.propsPrettyViewer);
        final Button propertyBtn = root.findViewById(R.id.addContents);
        final Button crash = root.findViewById(R.id.crash);
        final Button trackEvent = root.findViewById(R.id.track_event);
        final EditText editText = root.findViewById(R.id.number_of_events);
        final Button sendRandomEvents = root.findViewById(R.id.send_random_event);
        propertyBtn.setVisibility(View.GONE);
        sendRandomEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int number = 100;
                try{
                    number = Integer.parseInt(String.valueOf(editText.getText()));
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }
                Set<String>set = TTBaseEvents.keySet();
                String[]key = set.toArray(new String[0]);
                for(int i =0;i<number;i++){
                    Random rand = new Random();
                    int index = rand.nextInt(key.length);
                    TikTokBusinessSdk.trackTTEvent(TTBaseEvents.get(key[index]));
                }
            }
        });
        crash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TikTokBusinessSdk.crashSDK();
            }
        });
        eventViewModel.getLiveProperties().observe(getViewLifecycleOwner(), s -> {
            try {
                assert s != null;
                propsTV.setText(s.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        final TextView eventTV = root.findViewById(R.id.eventName);
        eventViewModel.getLiveEventName().observe(getViewLifecycleOwner(), eventTV::setText);

        Button resetBtn = root.findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(view -> reset());

        propsTV.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), PropEditActivity.class);
            Bundle bundlePros = new Bundle();
            Iterator<String> keys = eventViewModel.getPropIterator();
            while (keys.hasNext()) {
                String key = keys.next();
                if(EVENT_PROPERTY_CONTENTS.equals(key)){
                    continue;
                }
                try {
                    bundlePros.putString(key, eventViewModel.getProp(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            intent.putExtras(bundlePros);
            startActivityForResult(intent, 2);
        });
        propertyBtn.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), PropEditActivity.class);
            intent.putExtra(SOURCE, SOURCE_CONTENT);
            startActivityForResult(intent, 2);
        });

        ImageButton savedEventsBtn = root.findViewById(R.id.savedEventsBtn);
        savedEventsBtn.setOnClickListener(v -> {
            final String[] events = TestEvents.getAllEvents();
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Select an event");
            builder.setItems(events, (dialog, selected) -> {
                eventViewModel.resetProps();
                eventViewModel.setEvent(events[selected]);
                if(TTBaseEvents.containsKey(events[selected])){
                    propsTV.setVisibility(View.GONE);
                    propertyBtn.setVisibility(View.GONE);
                } else {
                    for (String property : Objects.requireNonNull(TestEvents.TTEventProperties.get(events[selected]))) {
                        try {
                            eventViewModel.addProp(property, "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if(TestEvents.TTContentsEvent.contains(events[selected])){
                        propsTV.setVisibility(View.VISIBLE);
                        propertyBtn.setVisibility(View.VISIBLE);
                    } else {
                        propsTV.setVisibility(View.VISIBLE);
                        propertyBtn.setVisibility(View.GONE);
                    }
                }
            });
            builder.show();
        });

        Button sendBtn = root.findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(view -> {
            String eventName = eventTV.getText().toString();
            eventViewModel.setEvent(eventName);
            if (!eventName.equals("")) {
                eventLogViewModel.save(new EventLog(
                        eventName,
                        Objects.requireNonNull(eventViewModel.getLiveProperties().getValue()).toString()
                ), false);
                Toast.makeText(requireContext(), eventName + " event tracked, plz check log", Toast.LENGTH_SHORT).show();
            }
        });

        trackEvent.setOnClickListener(view -> {
            String eventName = eventTV.getText().toString();
            eventViewModel.setEvent(eventName);
            if (!eventName.equals("")) {
                eventLogViewModel.save(new EventLog(
                        eventName,
                        Objects.requireNonNull(eventViewModel.getLiveProperties().getValue()).toString()
                ), true);
                Toast.makeText(requireContext(), eventName + " event tracked, plz check log", Toast.LENGTH_SHORT).show();
            }
        });
        Button flushBtn = root.findViewById(R.id.flush);
        flushBtn.setOnClickListener(v -> {
            TikTokBusinessSdk.flush();
        });

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null || data.getExtras() == null){
            return;
        }
        Bundle bundle = data.getExtras();
        if (resultCode == 2) {
            try {
                if (!TextUtils.isEmpty(bundle.getString("event_prop"))) {
                    eventViewModel.setProps(new JSONObject(bundle.getString("event_prop")));
                } else {
                    eventViewModel.resetProps();
                    for (String key : bundle.keySet()) {
                        if (bundle.get(key) != null) {
                            try {
                                eventViewModel.addProp(key, bundle.get(key));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }
        } else if (resultCode == 3) {
            JSONObject jsonObject = new JSONObject();
            for (String key : bundle.keySet()) {
                if (bundle.get(key) != null) {
                    try {
                        jsonObject.put(key, bundle.get(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                eventViewModel.addContents(jsonObject);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void reset() {
        eventViewModel.resetEvent();
        eventViewModel.resetProps();
    }

}