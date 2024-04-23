/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test() {
        List<Integer> aaa = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

        System.out.println(aaa);
    }

    @Before
    public void setup() {
        System.out.println("setup");
        MockitoAnnotations.initMocks(ExampleUnitTest.class);
    }


    class Person {
        int aa() {
            System.out.println("aa");
            return 120;
        }
    }

    @Mock
    List t;


    @Test
    public void mockito() {
        List mockList = new ArrayList();
        List spyList = spy(mockList);
        spyList.add("123");
        spyList.add("123");
        System.out.println(spyList.size());

        Person p = spy(new Person());
        when(p.aa()).thenReturn(456);
//        doReturn(456).when(p).aa();

        System.out.println(p.aa());

    }

}