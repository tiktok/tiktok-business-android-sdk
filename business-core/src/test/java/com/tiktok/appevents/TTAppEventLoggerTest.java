/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.app.Application;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        TTUtil.class, TikTokBusinessSdk.class,
        TTAutoEventsManager.class,
        TTAppEventStorage.class, TTRequest.class
})
public class TTAppEventLoggerTest extends BaseTest {

    @Test
    public void skipFlush() throws Exception {
        PowerMockito.mockStatic(TikTokBusinessSdk.class);

        Application context = mock(Application.class);
        when(TikTokBusinessSdk.getApplicationContext()).thenReturn(context);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);

        TTLogger logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).flush(any());

        appEventLogger.logger = logger;
        appEventLogger.flush(TTAppEventLogger.FlushReason.FORCE_FLUSH);
        verify(logger).info(TTAppEventLogger.SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED);
    }

    @Test
    public void globalConfigFetchedButTurnedOff() {
        PowerMockito.mockStatic(TikTokBusinessSdk.class);
        when(TikTokBusinessSdk.isGlobalConfigFetched()).thenReturn(true);
        when(TikTokBusinessSdk.getSdkGlobalSwitch()).thenReturn(false);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);

        TTLogger logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).flush(any());

        appEventLogger.logger = logger;
        appEventLogger.flush(TTAppEventLogger.FlushReason.FORCE_FLUSH);
        verify(logger).info(TTAppEventLogger.SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF);
    }

    @Test
    public void globalSwitchOnButNetworkTurnedOff() {
        PowerMockito.mockStatic(TikTokBusinessSdk.class);
        PowerMockito.mockStatic(TTAppEventStorage.class);

        when(TikTokBusinessSdk.isGlobalConfigFetched()).thenReturn(true);
        when(TikTokBusinessSdk.isSystemActivated()).thenReturn(true);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);

        TTLogger logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).flush(any());

        appEventLogger.logger = logger;
        appEventLogger.flush(TTAppEventLogger.FlushReason.FORCE_FLUSH);
        verify(logger).info(TTAppEventLogger.NETWORK_IS_TURNED_OFF);
    }

    @Test
    public void globalSwitchOnButNetworkOnButAccessTokenNull() {
        PowerMockito.mockStatic(TikTokBusinessSdk.class);
        PowerMockito.mockStatic(TTAppEventStorage.class);

        when(TikTokBusinessSdk.isGlobalConfigFetched()).thenReturn(true);
        when(TikTokBusinessSdk.isSystemActivated()).thenReturn(true);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);

        TTLogger logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).flush(any());

        appEventLogger.logger = logger;
        appEventLogger.flush(TTAppEventLogger.FlushReason.FORCE_FLUSH);
    }

    TTAppEvent fromDisk1 = new TTAppEvent(TTAppEvent.TTAppEventType.track, "InternalTest", "{}", null, null);
    TTAppEvent fromDisk2 = new TTAppEvent(TTAppEvent.TTAppEventType.track,"InternalTest", "{}", null, null);
    TTAppEvent fromMemory3 = new TTAppEvent(TTAppEvent.TTAppEventType.track,"InternalTest", "{}", null, null);

    private TTAppEventLogger flushCommon() {
        PowerMockito.mockStatic(TikTokBusinessSdk.class);
        PowerMockito.mockStatic(TTAppEventStorage.class);
        PowerMockito.mockStatic(TTRequest.class);

        when(TikTokBusinessSdk.isGlobalConfigFetched()).thenReturn(true);
        when(TikTokBusinessSdk.isSystemActivated()).thenReturn(true);
        when(TikTokBusinessSdk.getNetworkSwitch()).thenReturn(true);

        TTAppEventLogger appEventLogger = mock(TTAppEventLogger.class);

        TTLogger logger = mock(TTLogger.class);
        doCallRealMethod().when(appEventLogger).flush(any());

        appEventLogger.logger = logger;

        TTAppEventPersist persist = new TTAppEventPersist();
        LinkedList<TTAppEvent> eventList = new LinkedList<>();
        eventList.add(fromDisk1);
        eventList.add(fromDisk2);
        persist.setAppEvents(eventList);

        TTAppEventsQueue.addEvent(fromMemory3);
        when(TTAppEventStorage.readFromDisk()).thenReturn(persist);
        return appEventLogger;

    }

    @Test
    public void flushNormally() {
        TTAppEventLogger appEventLogger = flushCommon();
        appEventLogger.flush(TTAppEventLogger.FlushReason.FORCE_FLUSH);
        ArgumentCaptor<List<TTAppEvent>> captor = ArgumentCaptor.forClass(List.class);
        PowerMockito.verifyStatic(TTRequest.class, VerificationModeFactory.times(1));
        TTRequest.reportAppEvent(any(), captor.capture());
        List<TTAppEvent> values = captor.getValue();
        assertEquals(3, values.size());
        assertEquals(fromDisk1, values.get(0));
        assertEquals(fromDisk2, values.get(1));
        assertEquals(fromMemory3, values.get(2));

        // TTAppEventStorage.readFromDisk is called first, but persist method is not called
        PowerMockito.verifyStatic(TTAppEventStorage.class, VerificationModeFactory.times(1));
        TTAppEventStorage.readFromDisk();
        PowerMockito.verifyStatic(TTAppEventStorage.class, VerificationModeFactory.noMoreInteractions());
        TTAppEventStorage.persist(any());
    }

    @Test
    public void flushFailed() {
        TTAppEventLogger appEventLogger = flushCommon();
        List<TTAppEvent> failed = new LinkedList<>();
        failed.add(new TTAppEvent(TTAppEvent.TTAppEventType.track,"InternalTest", "{}", null, null));

        // when failed to flush, persist to disk
        when(TTRequest.reportAppEvent(any(), any())).thenReturn(failed);

        appEventLogger.flush(TTAppEventLogger.FlushReason.FORCE_FLUSH);

        PowerMockito.verifyStatic(TTAppEventStorage.class, VerificationModeFactory.times(1));
        TTAppEventStorage.readFromDisk();
        PowerMockito.verifyStatic(TTAppEventStorage.class, VerificationModeFactory.times(1));
        TTAppEventStorage.persist(failed);
    }

}
