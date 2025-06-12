/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents.edp;

import static com.tiktok.appevents.edp.EDPConfig.button_black_list;
import static com.tiktok.appevents.edp.EDPConfig.enable_click_track;
import static com.tiktok.appevents.edp.EDPConfig.enable_sync_get_touch_info;
import static com.tiktok.appevents.edp.EDPConfig.enable_webview_request_track;
import static com.tiktok.appevents.edp.EDPConfig.page_detail_upload_deep_count;
import static com.tiktok.appevents.edp.EDPConfig.sensig_filtering_regex_list;
import static com.tiktok.appevents.edp.EDPConfig.time_diff_frequency_control;
import static com.tiktok.appevents.edp.TTEDPEventTrack.LAST_CLICK_TS;
import static com.tiktok.appevents.edp.TTEDPEventTrack.isSending;
import static com.tiktok.util.RegexUtil.replaceAllToHash;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.edp.proxy.ITouchListener;
import com.tiktok.appevents.edp.proxy.TouchProxyHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class TTHierarchyHelper {
    public static Handler mHandler;
    public static JSONObject getViewHierarchy(WeakReference<View> rootView, int hierarchy) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (hierarchy <= 0) {
                return jsonObject;
            }
            jsonObject.put("class_name", rootView.get().getClass().getCanonicalName());
            if (rootView.get() instanceof TextView) {
                String text = "";
                if (((TextView) rootView.get()).getText() != null) {
                    text = ((TextView) rootView.get()).getText().toString();
                }
                if (!TextUtils.isEmpty(text)) {
                    text = replaceAllToHash(sensig_filtering_regex_list, text);
                }
                jsonObject.put("text", text);
                jsonObject.put("font_size", ((TextView) rootView.get()).getTextSize());
            }
            int[] location = new int[2];
            rootView.get().getLocationOnScreen(location);
            jsonObject.put("left", location[0]);
            jsonObject.put("top", location[1]);
            jsonObject.put("width", rootView.get().getMeasuredWidth());
            jsonObject.put("height", rootView.get().getMeasuredHeight());
            jsonObject.put("scroll_x", rootView.get().getScrollX());
            jsonObject.put("scroll_y", rootView.get().getScrollY());
            if (rootView.get() instanceof ViewGroup) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < ((ViewGroup) rootView.get()).getChildCount(); i++) {
                    JSONObject jsonItem = getViewHierarchy((new WeakReference<>(((ViewGroup) (rootView.get())).getChildAt(i))), hierarchy-1);
                    jsonArray.put(jsonItem);
                }
                try {
                    jsonObject.put("child_views", jsonArray);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }catch (Throwable e){

        }
        return jsonObject;
    }

    public static void proxyOnTouch(WeakReference<View> rootView, WeakReference<Activity> activity){
        if(!enable_click_track) {
            return;
        }
        TouchProxyHelper.proxy(rootView, new ITouchListener() {
            long touchDown = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            touchDown = System.currentTimeMillis();
                            break;
                        case MotionEvent.ACTION_UP:
                            if (activity == null || activity.get() == null) {
                                return false;
                            }
                            if (button_black_list.contains(rootView.get().getClass().getCanonicalName())) {
                                return false;
                            }
                            if(!TTEDPEventTrack.checkUpload() || isSending){
                                return false;
                            }
                            if(System.currentTimeMillis() - LAST_CLICK_TS <= time_diff_frequency_control * 1000){
                                return false;
                            }
                            isSending = true;
                            String className = v.getClass().getCanonicalName();
                            if (enable_sync_get_touch_info) {
                                float rawX = event.getRawX();
                                float rawY = event.getRawY();
                                TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            TTEDPEventTrack.trackClick(className, rawX, rawY, rootView.get().getMeasuredWidth(),
                                                    rootView.get().getMeasuredHeight(), rootView.get() instanceof TextView ? ((TextView) (rootView.get())).getText().toString() : "",
                                                    activity.get().getClass().getSimpleName(), TTHierarchyHelper.getViewHierarchy(new WeakReference<>(activity.get().getWindow().getDecorView()), page_detail_upload_deep_count),
                                                    getViewHierarchyCount(new WeakReference<>(activity.get().getWindow().getDecorView())),
                                                    System.currentTimeMillis() - touchDown);
                                        } catch (Throwable e) {

                                        }
                                    }
                                });
                            } else {
                                TikTokBusinessSdk.getAppEventLogger().addToQ(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            TTEDPEventTrack.trackClick(className, event.getRawX(), event.getRawY(), rootView.get().getMeasuredWidth(),
                                                    rootView.get().getMeasuredHeight(), rootView.get() instanceof TextView ? ((TextView) (rootView.get())).getText().toString() : "",
                                                    activity.get().getClass().getSimpleName(), TTHierarchyHelper.getViewHierarchy(new WeakReference<>(activity.get().getWindow().getDecorView()), page_detail_upload_deep_count),
                                                    getViewHierarchyCount(new WeakReference<>(activity.get().getWindow().getDecorView())),
                                                    System.currentTimeMillis() - touchDown);
                                        } catch (Throwable e) {

                                        }
                                    }
                                });
                            }
                            break;
                    }
                }catch (Throwable e){

                }
                return false;
            }
        });
    }

    public static int getViewHierarchyCount(WeakReference<View> rootView){
        try {
            if (rootView.get() instanceof ViewGroup) {
                int viewHierarchyCount = 1;
                for (int i = 0; i < ((ViewGroup) (rootView.get())).getChildCount(); i++) {
                    viewHierarchyCount = Math.max(getViewHierarchyCount(new WeakReference<>(((ViewGroup) (rootView.get())).getChildAt(i))) + 1, viewHierarchyCount);
                }
                return viewHierarchyCount;
            } else {
                return 1;
            }
        }catch (Throwable e){
            return 0;
        }
    }

    public static Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    public static int getViewHierarchyCountAndRegisterOnTouch(WeakReference<View> rootView, WeakReference<Activity> activity) {
        try {
            if (rootView.get() instanceof WebView) {
                if (enable_webview_request_track) {
                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String url = ((WebView) rootView.get()).getOriginalUrl();
                                if (!TextUtils.isEmpty(url)) {
                                    TTEDPEventTrack.trackWebviewRequest(url);
                                }
                            } catch (Throwable e) {

                            }
                        }
                    });
                }
            }
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    proxyOnTouch(rootView, activity);
                }
            });
            if (rootView.get() instanceof ViewGroup) {
                int viewHierarchyCount = 1;
                for (int i = 0; i < ((ViewGroup) (rootView.get())).getChildCount(); i++) {
                    viewHierarchyCount = Math.max(getViewHierarchyCountAndRegisterOnTouch(new WeakReference<>(((ViewGroup) (rootView.get())).getChildAt(i)), activity) + 1, viewHierarchyCount);
                }
                ((ViewGroup) (rootView.get())).setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        getViewHierarchyCountAndRegisterOnTouch(new WeakReference<>(child), activity);
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {

                    }
                });
                return viewHierarchyCount;
            } else {
                return 1;
            }
        }catch (Throwable e){
            return 0;
        }
    }
}
