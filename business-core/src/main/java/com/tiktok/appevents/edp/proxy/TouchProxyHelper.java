package com.tiktok.appevents.edp.proxy;

import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TouchProxyHelper {

    public static void proxy(WeakReference<View>v, ITouchListener clickListener) {
        try {
            Method method = View.class.getDeclaredMethod("getListenerInfo");
            method.setAccessible(true);
            Object mListenerInfo = method.invoke(v.get());
            Class<?> clz = Class.forName("android.view.View$ListenerInfo");
            Field field = clz.getDeclaredField("mOnTouchListener");
            field.setAccessible(true);
            View.OnTouchListener onTouchListenerInstance = (View.OnTouchListener) field.get(mListenerInfo);
            View.OnTouchListener proxyTouchListener = new ProxyOnTouchListener(clickListener, onTouchListenerInstance);
            if(onTouchListenerInstance instanceof ProxyOnTouchListener){
                return;
            }
            field.set(mListenerInfo, proxyTouchListener);
        } catch (Throwable e) {

        }
    }
}
