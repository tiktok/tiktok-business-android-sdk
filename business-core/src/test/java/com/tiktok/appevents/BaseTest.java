/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import com.tiktok.util.TTUtil;

import org.junit.Before;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.ArgumentMatchers.anyString;

public class BaseTest {
    @Before
    public void setup() throws Exception {
        PowerMockito.mockStatic(TTUtil.class);
        PowerMockito.doNothing().when(TTUtil.class, "checkThread", anyString());
    }
}
