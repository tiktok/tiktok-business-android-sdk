/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.view.View;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTLogger;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TTAppEvent implements Serializable {

    public static enum TTAppEventType{
        track,
        identify
    }

    private static final long serialVersionUID = 2L;
    private List<String> tiktokAppIds = new ArrayList<>();
    private TTAppEventType type;
    private String eventName;
    private Date timeStamp;
    private String propertiesJson;
    private String eventId;
    private static AtomicLong counter = new AtomicLong(new Date().getTime() + 0L);
    private Long uniqueId;
    private TTUserInfo userInfo;
    private String screenShot;
    private static String TAG = TTAppEventsQueue.class.getCanonicalName();
    private static TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    TTAppEvent(TTAppEventType type, String eventName, String propertiesJson, String eventId, String[] ttAppId) {
        this(type, eventName, new Date(), propertiesJson, eventId, ttAppId);
    }

    TTAppEvent(TTAppEventType type, String eventName, Date timeStamp, String propertiesJson, String eventId, String[] ttAppId) {
        this.type = type;
        this.eventName = eventName;
        this.timeStamp = timeStamp;
        this.propertiesJson = propertiesJson;
        this.eventId = eventId;
        this.uniqueId = TTAppEvent.counter.getAndIncrement();
        this.userInfo = TTUserInfo.sharedInstance.clone();
        if (ttAppId != null && ttAppId.length > 0) {
            for (int i = 0; i < ttAppId.length; i++) {
                tiktokAppIds.add(ttAppId[i]);
            }
        }
    }

    public TTUserInfo getUserInfo() {
        return userInfo;
    }

    public String getEventName() {
        return eventName;
    }

    public String getType(){
        return this.type.name();
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPropertiesJson() {
        return propertiesJson;
    }

    public void setPropertiesJson(String propertiesJson) {
        this.propertiesJson = propertiesJson;
    }

    public Long getUniqueId() {
        return this.uniqueId;
    }

    public List<String> getTiktokAppIds() {
        return tiktokAppIds;
    }

    public void setTiktokAppIds(List<String> tiktokAppIds) {
        this.tiktokAppIds = tiktokAppIds;
    }

    public String getScreenShot() {
        return screenShot;
    }

    public void setScreenShot() {
        try {
            View view = TTLifecycleListener.getActivityRef().get().getWindow().getDecorView();
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 5, outputStream);
            this.screenShot = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        } catch (Throwable e) {
            logger.error(e, "taker screen shot error");
        }
    }

    @Override
    public String toString() {
        return "TTAppEvent{" +
                "eventName='" + eventName + '\'' +
                ", timeStamp=" + timeStamp +
                ", propertiesJson='" + propertiesJson + '\'' +
                ", eventId='" + eventId + '\'' +
                ", uniqueId=" + uniqueId +
                ", tiktokAppIds=" + tiktokAppIds +
                '}';
    }
}
