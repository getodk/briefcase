package org.opendatakit.briefcase.model;

import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import org.opendatakit.briefcase.buildconfig.BuildConfig;

/**
 *  This class is used to send usage information to ODK's Google Analytics account.
 *
 * <P>A library built by 'brsanthu' is utilized to streamline the
 * process of contacting Google's Measurement Protocol API.</P>
 * <P>This class also implements the Singleton pattern.</P>
 *
 * @author Joao Paulo Knox
 * @see <a href="https://github.com/brsanthu/google-analytics-java">google-analytics-java</a>
 */
public class BriefcaseAnalytics {

    private static final String GOOGLE_TRACKING_ID = BriefcasePreferences.GOOGLE_TRACKING_ID;
    private static final String UNIQUE_USER_ID = BriefcasePreferences.getUniqueUserID();
    private static final String EVENT_CATEGORY = "ODK Briefcase";
    private static final String APPLICATION_NAME = BuildConfig.NAME;
    private static final String APPLICATION_VERSION = BuildConfig.VERSION;
    private final GoogleAnalytics googleAnalytics = new GoogleAnalytics(GOOGLE_TRACKING_ID);

    public BriefcaseAnalytics() {
    }

    /**
     *  Sends usage information to Google Analytics, only if the user has consented,
     *  or if they are enabling or disabling tracking. The information is modeled using an {@link EventHit} object.
     *
     * @param action (required) name of the event itself.
     * @param label (optional) an open-ended label.
     * @param value (optional) an open-ended value. Must be an integer.
     * @param always whether to track the event even when the tracking is turned off
     *               (such as when we record that they have changed the tracking preference). Usually false.
     */
    public void trackEvent(String action, String label, Integer value, boolean always) {
        if (always || BriefcasePreferences.getBriefcaseTrackingConsentProperty()) {
            EventHit eventHit = new EventHit();
            eventHit.trackingId(GOOGLE_TRACKING_ID);
            eventHit.clientId(UNIQUE_USER_ID);
            eventHit.dataSource(System.getProperty("os.name"));
            eventHit.applicationName(APPLICATION_NAME);
            eventHit.applicationVersion(APPLICATION_VERSION);
            eventHit.eventCategory(EVENT_CATEGORY);
            eventHit.eventAction(action);
            eventHit.eventLabel(label);
            eventHit.eventValue(value);

            googleAnalytics.postAsync(eventHit);
        }
    }

    /**
     * Track this application's startup event.
     */
    public void trackStartup() {
        trackEvent("Application-Startup", null, null, false);
    }

    /**
     * Tracks the user giving/withdrawing their consent for usage tracking, and saves the new preference.
     */
    public void trackConsentDecision(boolean givesConsent) {
        BriefcasePreferences.setBriefcaseTrackingConsentProperty(givesConsent);
        trackEvent(givesConsent ? "Tracking-Enabled" : "Tracking-Disabled", null, null, true);
    }
}
