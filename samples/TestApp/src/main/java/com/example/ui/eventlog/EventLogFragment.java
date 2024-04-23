/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.eventlog;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;
import com.example.model.EventLog;
import com.example.ui.events.EventFragment;
import com.example.utils.LogListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class EventLogFragment extends Fragment {

    private ListView myList;
    private LogListAdapter adapter;

    private EventLogViewModel eventLogViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        eventLogViewModel = new ViewModelProvider(this).get(EventLogViewModel.class);
        View root = inflater.inflate(R.layout.fragment_eventlog, container, false);

        myList = root.findViewById(R.id.logList);
        eventLogViewModel.getAllEventLogs().observe(getViewLifecycleOwner(), eventLogs -> {
            adapter = new LogListAdapter(requireContext(), (ArrayList<EventLog>) eventLogs);
            myList.setAdapter(adapter);
        });

        myList.setOnItemClickListener((adapterView, view, position, id) -> {
            EventLog eventLog = (EventLog) myList.getItemAtPosition(position);

            AlertDialog.Builder showProps = new AlertDialog.Builder(requireContext());
            showProps.setTitle(eventLog.eventType);
            showProps.setMessage(eventLog.id + " - " + eventLog.createdAt);
            LinearLayout layout = new LinearLayout(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(40, 0, 40, 0);
            TextView propTV = new TextView(requireContext());
            try {
                JSONObject props = new JSONObject(eventLog.properties);
                propTV.setText(props.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            propTV.setLayoutParams(lp);
            layout.addView(propTV);
            showProps.setView(layout);
            showProps.setPositiveButton("Edit & Resend",  (dialogInterface, i) -> {
                EventFragment fragment = new EventFragment();
                Bundle bundle = new Bundle();
                bundle.putString("event_type", eventLog.eventType);
                bundle.putString("event_props", eventLog.properties);
                fragment.setArguments(bundle);
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.replace(R.id.nav_host_fragment, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.addToBackStack(null);
                ft.commit();
            });
            showProps.setNegativeButton("Close", null);
            showProps.create().show();
        });

        return root;
    }
}