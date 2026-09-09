/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.app.Application;

import androidx.lifecycle.Lifecycle;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TTUtil.class, TTRequest.class})
public class ConsentWithdrawalTest extends BaseTest {

    private static TTAppEvent event() {
        return new TTAppEvent(TTAppEvent.TTAppEventType.track, "InternalTest", "{}", null, null);
    }

    /**
     * the sdk event loop is single threaded, so a task queued afterwards completes last
     */
    private static void waitForEventLoop() throws Exception {
        TTAppEventLogger.eventLoop.submit(() -> {
        }).get(5, TimeUnit.SECONDS);
    }

    private static void useConfig(boolean discardEventsWhileDisabled) {
        Application application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(application);
        TikTokBusinessSdk.TTConfig config = new TikTokBusinessSdk.TTConfig(application);
        if (discardEventsWhileDisabled) {
            config.discardEventsWhileDisabled();
        }
        Whitebox.setInternalState(TikTokBusinessSdk.class, "config", config);
    }

    @After
    public void reset() {
        Whitebox.setInternalState(TikTokBusinessSdk.class, "config", (Object) null);
        TikTokBusinessSdk.setSdkGlobalSwitch(true);
        TikTokBusinessSdk.setRemoteSdkSwitch(true);
        TTAppEventsQueue.clearAll();
        TTEdpAppEventsQueue.clearAll();
        TTCrashHandler.clearAll(null);
    }

    @Test
    public void appSwitchAndRemoteSwitchAreStoredApart() {
        TikTokBusinessSdk.setSdkGlobalSwitch(false);
        // what the global config callback does
        TikTokBusinessSdk.setRemoteSdkSwitch(true);
        assertFalse(TikTokBusinessSdk.getSdkGlobalSwitch());
        assertFalse(TikTokBusinessSdk.isSdkEnabled());

        TikTokBusinessSdk.setSdkGlobalSwitch(true);
        TikTokBusinessSdk.setRemoteSdkSwitch(false);
        assertTrue(TikTokBusinessSdk.getSdkGlobalSwitch());
        assertFalse(TikTokBusinessSdk.isSdkEnabled());

        TikTokBusinessSdk.setRemoteSdkSwitch(true);
        assertTrue(TikTokBusinessSdk.isSdkEnabled());
    }

    @Test
    public void noConfigIsFetchedWhileTheAppSwitchIsOff() throws Exception {
        PowerMockito.mockStatic(TTRequest.class);
        when(TTRequest.getBusinessSDKConfig())
                .thenReturn(new JSONObject("{\"business_sdk_config\":{\"enable_sdk\":true}}"));

        TikTokBusinessSdk.setSdkGlobalSwitch(false);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);
        appEventLogger.logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).fetchGlobalConfig(anyInt());

        appEventLogger.fetchGlobalConfig(0);
        waitForEventLoop();

        PowerMockito.verifyStatic(TTRequest.class, never());
        TTRequest.getBusinessSDKConfig();
        verify(appEventLogger.logger).info(TTAppEventLogger.SKIP_CONFIG_BECAUSE_APP_SWITCH_IS_TURNED_OFF);
        assertFalse(TikTokBusinessSdk.getSdkGlobalSwitch());
    }

    @Test
    public void remoteConfigDoesNotOverwriteTheAppSwitch() throws Exception {
        PowerMockito.mockStatic(TTRequest.class);
        when(TTRequest.getBusinessSDKConfig())
                .thenReturn(new JSONObject("{\"business_sdk_config\":{\"enable_sdk\":false}}"));

        TikTokBusinessSdk.setSdkGlobalSwitch(true);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);
        appEventLogger.logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).fetchGlobalConfig(anyInt());

        appEventLogger.fetchGlobalConfig(0);
        waitForEventLoop();

        assertTrue(TikTokBusinessSdk.getSdkGlobalSwitch());
        assertFalse(TikTokBusinessSdk.getRemoteSdkSwitch());
        assertFalse(TikTokBusinessSdk.isSdkEnabled());
    }

    @Test
    public void destroyUnregistersLifecycleCallbacksAndStopsQueueing() throws Exception {
        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);
        Lifecycle lifecycle = mock(Lifecycle.class);
        appEventLogger.lifecycle = lifecycle;
        TTActivityLifecycleCallbacksListener listener = new TTActivityLifecycleCallbacksListener(appEventLogger);
        appEventLogger.activityLifecycleCallbacks = listener;
        doCallRealMethod().when(appEventLogger).destroy();
        doCallRealMethod().when(appEventLogger).unregisterLifecycleCallbacks();
        doCallRealMethod().when(appEventLogger).addToQ(any());

        appEventLogger.destroy();

        assertTrue(appEventLogger.destroyed);
        assertNull(appEventLogger.activityLifecycleCallbacks);
        verify(lifecycle).removeObserver(listener);

        // a later foreground callback can no longer queue any work
        AtomicBoolean ran = new AtomicBoolean(false);
        appEventLogger.addToQ(() -> ran.set(true));
        waitForEventLoop();
        assertFalse(ran.get());
    }

    @Test
    public void eventsAreDiscardedWhileDisabled() throws Exception {
        useConfig(true);
        TikTokBusinessSdk.setSdkGlobalSwitch(true);

        TTAppEventsQueue.addEvent(event());
        assertEquals(1, TTAppEventsQueue.size());

        // turning the app switch off purges what was already stored
        TikTokBusinessSdk.setSdkGlobalSwitch(false);
        waitForEventLoop();
        assertEquals(0, TTAppEventsQueue.size());

        // and every kind of event created afterwards is dropped
        TTAppEventsQueue.addEvent(event());
        TTEdpAppEventsQueue.addEvent(event());
        TTCrashHandler.retryLater(new JSONObject("{\"monitor\":\"metric\"}"));
        assertEquals(0, TTAppEventsQueue.size());
        assertEquals(0, TTEdpAppEventsQueue.size());
        assertTrue(TTCrashHandler.crashReport.reports.isEmpty());
    }

    @Test
    public void eventsAreKeptWhileDisabledByDefault() {
        useConfig(false);
        TikTokBusinessSdk.setSdkGlobalSwitch(false);

        TTAppEventsQueue.addEvent(event());
        TTEdpAppEventsQueue.addEvent(event());
        TTCrashHandler.retryLater(new JSONObject());

        assertEquals(1, TTAppEventsQueue.size());
        assertEquals(1, TTEdpAppEventsQueue.size());
        assertFalse(TTCrashHandler.crashReport.reports.isEmpty());
    }

    @Test
    public void clearPersistedEventsPurgesQueuesAndMonitorReports() throws Exception {
        TTAppEventsQueue.addEvent(event());
        TTEdpAppEventsQueue.addEvent(event());
        TTCrashHandler.retryLater(new JSONObject("{\"monitor\":\"metric\"}"));
        assertEquals(1, TTAppEventsQueue.size());
        assertEquals(1, TTEdpAppEventsQueue.size());
        assertFalse(TTCrashHandler.crashReport.reports.isEmpty());

        TikTokBusinessSdk.clearPersistedEvents();
        waitForEventLoop();

        assertEquals(0, TTAppEventsQueue.size());
        assertEquals(0, TTEdpAppEventsQueue.size());
        assertTrue(TTCrashHandler.crashReport.reports.isEmpty());
    }

    @Test
    public void noRequestLeavesTheDeviceWhileTheAppSwitchIsOff() {
        TikTokBusinessSdk.setSdkGlobalSwitch(false);
        HttpRequestUtil.HttpResponse response =
                HttpRequestUtil.doPost("https://example.com/anything", new HashMap<>(), "{}");
        assertFalse(response.isOK());
        assertEquals(-1, response.httpCode);
    }
}
