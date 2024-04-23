/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.internalmonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tiktok.appevents.TTAppEvent;
import com.tiktok.appevents.TTAppEventLogger;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AppEventCycleAdapter extends RecyclerView.Adapter<AppEventCycleAdapter.ViewHolder> {

    Context context;
    private LayoutInflater inflater;

    public AppEventCycleAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName;
        TextView eventTimestamp;
        TextView eventDetails;

        public TextView getEventName() {
            return eventName;
        }

        public TextView getEventTimestamp() {
            return eventTimestamp;
        }

        public TextView getEventDetails() {
            return eventDetails;
        }

        public ViewHolder(View view) {
            super(view);
            eventName = view.findViewById(R.id.event_name);
            eventTimestamp = view.findViewById(R.id.event_timestamp);
            eventDetails = view.findViewById(R.id.event_details);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.app_event_details, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int lastIndex = TTAppEventLogger.getSuccessfulEvents().size() - 1;
        TTAppEvent event = TTAppEventLogger.getSuccessfulEvents().get(lastIndex - position);
        holder.getEventName().setText(event.getEventName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        holder.getEventTimestamp().setText(dateFormat.format(event.getTimeStamp()));
        holder.getEventDetails().setText(event.getPropertiesJson());
    }

    @Override
    public int getItemCount() {
        return TTAppEventLogger.getSuccessfulEvents().size();
    }
}
