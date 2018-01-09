package org.opendatakit.briefcase.model;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class BriefcaseAnalyticsTest {
    @Test
    public void canCreateBriefcaseAnalytics() {
        BriefcaseAnalytics ba = new BriefcaseAnalytics();
        assertNotNull(ba);
    }
}
