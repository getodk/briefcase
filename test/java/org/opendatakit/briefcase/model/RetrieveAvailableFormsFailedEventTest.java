package org.opendatakit.briefcase.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class RetrieveAvailableFormsFailedEventTest {

    @Test
    public void getReason_unknown() {
        RetrieveAvailableFormsFailedEvent testEvent = new RetrieveAvailableFormsFailedEvent(null);
        assertEquals("unknown", testEvent.getReason());
    }

    @Test
    public void getReason_exception() {
        Exception e = new Exception("fail");
        RetrieveAvailableFormsFailedEvent testEvent = new RetrieveAvailableFormsFailedEvent(e);
        assertEquals("Exception: fail", testEvent.getReason());
    }
}