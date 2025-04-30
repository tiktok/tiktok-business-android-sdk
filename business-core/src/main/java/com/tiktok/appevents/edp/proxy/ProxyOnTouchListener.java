package com.tiktok.appevents.edp.proxy;

import android.view.MotionEvent;
import android.view.View;

public class ProxyOnTouchListener implements View.OnTouchListener {

    ITouchListener listener;
    View.OnTouchListener originOnTouchListener;
    public ProxyOnTouchListener(ITouchListener listener, View.OnTouchListener originOnTouchListener) {
        this.listener = listener;
        this.originOnTouchListener = originOnTouchListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean consumed = false;
        if (originOnTouchListener != null){
            consumed = originOnTouchListener.onTouch(v, event);
        }
        if (listener != null){
            listener.onTouch(v, event);
        }
        return consumed;
    }
}
