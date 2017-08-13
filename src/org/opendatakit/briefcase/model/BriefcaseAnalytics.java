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

  private static BriefcaseAnalytics instance = null;
  private static GoogleAnalytics googleAnalytics = null;
  private static GoogleAnalyticsConfig config = null;
  private static Boolean briefcaseTrackingConsent = null;
  private static Boolean hasTrackedStartup = false;

  /**
   * Constructor
   *
   * <P>This constructor will instantiate 'brsanthu's {@link GoogleAnalytics} class and
   * check if the user is okay with their behaviour being tracked.</P>
   * <P>The constructor is private so as to adhere to the rules of the Singleton pattern.</P>
   */
  private BriefcaseAnalytics() {
    config = new GoogleAnalyticsConfig();
    checkTrackingConsentState();
    this.googleAnalytics = new GoogleAnalytics(config, GOOGLE_TRACKING_ID);
  }

  /**
   * Singleton creator.
   *
   * <P>This method (along with the private constructor) will ensure
   * that only one instance of this class is instantiated.</P>
   * @return the instantiated Singleton of {@link BriefcaseAnalytics}.
   */
  public static BriefcaseAnalytics getInstance() {
    return instance == null ? new BriefcaseAnalytics() : instance;
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
    checkTrackingConsentState();
    EventHit eventHit = new EventBuilder()
      .trackingID(GOOGLE_TRACKING_ID)
      .clientID(UNIQUE_USER_ID)
      .dataSource(System.getProperty("os.name"))
      .applicationName(BuildConfig.NAME)
      .applicationVersion(BuildConfig.VERSION)
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
    if (hasTrackedStartup == false) {
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
  private void checkTrackingConsentState() {
    briefcaseTrackingConsent = BriefcasePreferences.getBriefcaseTrackingConsentProperty();
    config.setGatherStats(briefcaseTrackingConsent);
  }

  /**
   * A helper class used to streamline the creation Events.
   *
   * <P>An {@link EventHit} object is used to model the information being tracked.</P>
   */
  private static class EventBuilder {

    private String trackingID = null;
    private String clientID = null;
    private String dataSource = null;
    private String applicationName = null;
    private String applicationVersion = null;
    private String eventCategory = null;
    private String eventAction = null;
    private String eventLabel = null;
    private Integer eventValue = null;

    public EventBuilder trackingID(String trackingID) {
      this.trackingID = trackingID;
      return this;
    }

    public EventBuilder clientID(String clientID) {
      this.clientID = clientID;
      return this;
    }

    public EventBuilder dataSource(String dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public EventBuilder applicationName(String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    public EventBuilder applicationVersion(String applicationVersion) {
      this.applicationVersion = applicationVersion;
      return this;
    }

    public EventBuilder eventCategory(String eventCategory) {
      this.eventCategory = eventCategory;
      return this;
    }

    public EventBuilder eventAction(String eventAction) {
      this.eventAction = eventAction;
      return this;
    }

    public EventBuilder eventLabel(String eventLabel) {
      this.eventLabel = eventLabel;
      return this;
    }

    public EventBuilder eventValue(Integer eventValue) {
      this.eventValue = eventValue;
      return this;
    }

    public EventHit build() {
      EventHit eventHit = new EventHit();
      eventHit.trackingId(this.trackingID);
      eventHit.clientId(this.clientID);
      eventHit.dataSource(this.dataSource);
      eventHit.applicationName(this.applicationName);
      eventHit.applicationVersion(this.applicationVersion);
      eventHit.eventCategory(this.eventCategory);
      eventHit.eventAction(this.eventAction);
      eventHit.eventLabel(this.eventLabel);
      eventHit.eventValue(this.eventValue);
      return eventHit;
    }

  }
}
