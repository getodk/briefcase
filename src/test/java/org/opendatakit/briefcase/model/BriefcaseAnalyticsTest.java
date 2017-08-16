package org.opendatakit.briefcase.model;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class BriefcaseAnalyticsTest {
    @Test
    public void canGetBriefcaseAnalyticsInstance() {
        BriefcaseAnalytics ba = new BriefcaseAnalytics();
        assertNotNull(ba);
    }
}
