package org.opendatakit.briefcase.model;

import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsConfig;
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

  private static BriefcaseAnalytics instance;
  private static final GoogleAnalyticsConfig config = new GoogleAnalyticsConfig();
  private static final GoogleAnalytics googleAnalytics = new GoogleAnalytics(config, GOOGLE_TRACKING_ID);
  private static boolean hasTrackedStartup;

  /**
   * Constructor
   *
   * <P>The constructor is private so as to adhere to the rules of the Singleton pattern.</P>
   */
  private BriefcaseAnalytics() {
  }

  /**
   * Singleton creator.
   *
   * <P>This method (along with the private constructor) will ensure
   * that only one instance of this class is instantiated.</P>
   * @return the instantiated Singleton of {@link BriefcaseAnalytics}.
   */
  public static BriefcaseAnalytics getInstance() {
    return instance == null ? (instance = new BriefcaseAnalytics()) : instance;
  }

  /**
   *  Sends usage information to Google Analytics, only if the user has consented.
   *  The information is modeled using an {@link EventHit} object.
   *
   * @param action (required) name of the event itself.
   * @param label (optional) an open-ended label.
   * @param value (optional) an open-ended value. Must be an integer.
   * @return the {@link EventHit} object.
   */
  public EventHit trackEvent(String action, String label, Integer value) {
    checkStateTrackingConsent();
    EventHit eventHit = new EventHitBuilder()
      .trackingID(GOOGLE_TRACKING_ID)
      .clientID(UNIQUE_USER_ID)
      .dataSource(System.getProperty("os.name"))
      .applicationName(APPLICATION_NAME)
      .applicationVersion(APPLICATION_VERSION)
      .eventCategory(EVENT_CATEGORY)
      .eventAction(action)
      .eventLabel(label)
      .eventValue(value)
      .build();
    googleAnalytics.postAsync(eventHit);
    return eventHit;
  }

  /**
   * Track this application's startup event.
   *
   * <P>This should only be run once per application execution.</P>
   * <P>Note that the method will check to see if it has already tracked this session's startup.</P>
   */
  public void trackStartup() {
    if (!hasTrackedStartup) {
      trackEvent("Application-Startup", null, null);
      hasTrackedStartup = true;
    }
  }

  /**
   * Check if the user has set or updated their consent
   * to ODK tracking their application usage.
   *
   * <P>Note that this method should be executed before any event is tracked,
   * in case the user has unticked the consent checkbox within the session.</P>
   */
  private void checkStateTrackingConsent() {
    config.setGatherStats(BriefcasePreferences.getBriefcaseTrackingConsentProperty());
  }

  /**
   * A helper class used to streamline the creation of EventHits.
   *
   * <P>An {@link EventHit} object is used to model the information being tracked.</P>
   */
  private static class EventHitBuilder {

    private String trackingID;
    private String clientID;
    private String dataSource;
    private String applicationName;
    private String applicationVersion;
    private String eventCategory;
    private String eventAction;
    private String eventLabel;
    private Integer eventValue;

    public EventHitBuilder trackingID(String trackingID) {
      this.trackingID = trackingID;
      return this;
    }

    public EventHitBuilder clientID(String clientID) {
      this.clientID = clientID;
      return this;
    }

    public EventHitBuilder dataSource(String dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public EventHitBuilder applicationName(String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    public EventHitBuilder applicationVersion(String applicationVersion) {
      this.applicationVersion = applicationVersion;
      return this;
    }

    public EventHitBuilder eventCategory(String eventCategory) {
      this.eventCategory = eventCategory;
      return this;
    }

    public EventHitBuilder eventAction(String eventAction) {
      this.eventAction = eventAction;
      return this;
    }

    public EventHitBuilder eventLabel(String eventLabel) {
      this.eventLabel = eventLabel;
      return this;
    }

    public EventHitBuilder eventValue(Integer eventValue) {
      this.eventValue = eventValue;
      return this;
    }

    public EventHit build() {
      EventHit eventHit = new EventHit();
      eventHit.trackingId(trackingID);
      eventHit.clientId(clientID);
      eventHit.dataSource(dataSource);
      eventHit.applicationName(applicationName);
      eventHit.applicationVersion(applicationVersion);
      eventHit.eventCategory(eventCategory);
      eventHit.eventAction(eventAction);
      eventHit.eventLabel(eventLabel);
      eventHit.eventValue(eventValue);
      return eventHit;
    }

  }
}
