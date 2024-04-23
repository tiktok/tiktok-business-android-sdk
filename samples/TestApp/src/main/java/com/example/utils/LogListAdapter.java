/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.R;
import com.example.model.EventLog;

import java.util.ArrayList;

public class LogListAdapter extends BaseAdapter {
    private ArrayList<EventLog> eventLogs;
    private LayoutInflater mLayoutInflater;

    public LogListAdapter(Context context, ArrayList<EventLog> arrayList) {
        eventLogs = arrayList;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return eventLogs.size();
    }

    @Override
    public Object getItem(int position) {
        return eventLogs.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = mLayoutInflater.inflate(R.layout.list_eventlog, null);
            holder.logID = view.findViewById(R.id.logID);
            holder.log = view.findViewById(R.id.log);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        EventLog eventLog = eventLogs.get(position);
        if (eventLog != null) {
            if (holder.logID != null) {
                holder.logID.setText(""+eventLog.id);
            }
            if (holder.log != null) {
                holder.log.setText(eventLog.eventType + " @ " +eventLog.createdAt);
            }
        }
        return view;
    }

    private static class ViewHolder {
        protected TextView logID;
        protected TextView log;
    }
}