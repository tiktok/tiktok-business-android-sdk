/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.model.EventLog;
import com.example.persistence.EventLogRepo;
import com.example.testdata.TestEvents;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.TikTokBusinessSdk.TTConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class App extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_init,
                R.id.nav_billing, R.id.nav_events, R.id.nav_eventlog, R.id.deepLink)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        EventLogRepo eventLogRepo = new EventLogRepo(getApplication());
        switch (item.getItemId()) {
            case R.id.action_clearlog:
                eventLogRepo.clear();
                break;
            case R.id.action_preload:
                for (String event : TestEvents.getAllEvents()) {
                    JSONObject props = new JSONObject();
                    try {
                        for (String prop : Objects.requireNonNull(TestEvents.TTEventProperties.get(event))) {
                            props.put(prop, "");
                        }
                        eventLogRepo.save(new EventLog(event, props.toString()), false);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.action_loadtest:
                int MAX = 1000;
                int count = 0;
                List<EventLog> logs = null;
                try {
                    logs = eventLogRepo.getLogs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (count < MAX && logs.size() > 0) {
                    for (EventLog log : Objects.requireNonNull(logs)) {
                        eventLogRepo.save(new EventLog(log.eventType, log.properties), false);
                        count++;
                        if (count >= MAX) break;
                    }
                }
                break;
            case R.id.action_crash_sdk:
                TikTokBusinessSdk.crashSDK();
                break;
            case R.id.action_crash_app:
                throw new RuntimeException("force crash from app");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
