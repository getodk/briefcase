package org.opendatakit.briefcase.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServerConnectionInfoTest {

    @Test
    public void testEquals_False() {
        char[] testPassword = {'a'};
        ServerConnectionInfo testInfo = new ServerConnectionInfo("testURL", "testName", testPassword);
        Object o = new Object();

        assertFalse(testInfo.equals(o));
    }

    @Test
    public void testEquals_True() {
        char[] testPassword = {'a'};
        ServerConnectionInfo testInfo = new ServerConnectionInfo("testURL", "testName", testPassword);
        ServerConnectionInfo testInfo2 = new ServerConnectionInfo("testURL", "testName", testPassword);

        assertTrue(testInfo.equals(testInfo2));
    }

    @Test
    public void testEquals_True_Itself() {
        char[] testPassword = {'a'};
        ServerConnectionInfo testInfo = new ServerConnectionInfo("testURL", "testName", testPassword);
        assertTrue(testInfo.equals(testInfo));
    }

}