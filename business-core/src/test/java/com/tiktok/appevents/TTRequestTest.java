/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TTRequestTest {

    private List<Object> makeList(int length) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            result.add(new Object());
        }
        return result;
    }

    @Test
    public void testAverageAssign() {
        List<Object> list1 = makeList(10);
        List<List<Object>> result = TTRequest.averageAssign(list1, 5);
        assertEquals(5, result.get(0).size());
        assertEquals(5, result.get(1).size());
        assertEquals(2, result.size());

        result = TTRequest.averageAssign(list1, 3);

        assertEquals(3, result.get(0).size());
        assertEquals(3, result.get(1).size());
        assertEquals(3, result.get(2).size());
        assertEquals(1, result.get(3).size());
        assertEquals(4, result.size());


        result = TTRequest.averageAssign(Collections.emptyList(), 3);
        assertEquals(0, result.size());
    }
}
