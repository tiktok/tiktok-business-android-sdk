package com.tiktok.appevents.edp.proxy;

import android.view.MotionEvent;
import android.view.View;

public interface ITouchListener {
    boolean onTouch(View v, MotionEvent event);
}
