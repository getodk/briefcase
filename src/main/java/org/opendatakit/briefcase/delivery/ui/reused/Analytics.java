/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.delivery.ui.reused;

import static org.opendatakit.briefcase.reused.model.Host.isLinux;
import static org.opendatakit.briefcase.reused.model.Host.isWindows;

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.request.EventHit;
import com.brsanthu.googleanalytics.request.ScreenViewHit;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.JComponent;

public class Analytics {
  private static final String LINUX_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.157 Safari/537.36";
  private static final String WINDOWS_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";
  private static final String MAC_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/603.3.8 (KHTML, like Gecko)";
  private static final String USER_AGENT = isWindows() ? WINDOWS_USER_AGENT : isLinux() ? LINUX_USER_AGENT : MAC_USER_AGENT;

  private final GoogleAnalytics ga;
  private final String version;
  private final String userId;
  private final String screenSize;
  private final Supplier<Dimension> windowSizeGetter;

  Analytics(GoogleAnalytics ga, String version, String userId, String screenSize, Supplier<Dimension> windowSizeGetter) {
    this.ga = ga;
    this.version = version;
    this.userId = userId;
    this.screenSize = screenSize;
    this.windowSizeGetter = windowSizeGetter;
  }

  public static Analytics from(String trackingId, String version, String userId, Dimension screenSize, Supplier<Dimension> windowSizeGetter) {
    GoogleAnalytics ga = GoogleAnalytics.builder()
        .withTrackingId(trackingId)
        .build();
    return new Analytics(ga, version, userId, buildResolutionString(screenSize), windowSizeGetter);
  }

  public void enter(String screenName) {
    buildScreenViewHit()
        .documentTitle(screenName)
        .documentPath(screenName.toLowerCase())
        .screenName(screenName)
        .sessionControl("start")
        .sendAsync();
  }

  public void leave(String screenName) {
    buildScreenViewHit()
        .documentTitle(screenName)
        .documentPath(screenName.toLowerCase())
        .screenName(screenName)
        .sessionControl("end")
        .sendAsync();
  }

  public void event(String category, String action, String label, Integer value) {
    event(category, action, label, value, true);
  }

  public void event(String category, String action, String label, Integer value, boolean async) {
    EventHit request = buildEventHit()
        .eventCategory(category)
        .eventAction(action);
    Optional.ofNullable(label).ifPresent(request::eventLabel);
    Optional.ofNullable(value).ifPresent(request::eventValue);
    if (async)
      request.sendAsync();
    else
      request.send();
  }

  public ComponentListener buildComponentListener(String screenName) {
    return new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        enter(screenName);
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        leave(screenName);
      }
    };
  }

  public void enableTracking(boolean enabled) {
    enableTracking(enabled, true);
  }

  public void enableTracking(boolean enabled, boolean silently) {
    if (!silently) {
      ga.getConfig().setEnabled(true);
      // We synchonously send this event now because otherwise, disable events won't be send
      event("Settings", "Track consent", enabled ? "Enable" : "Disable", null, false);
    }
    ga.getConfig().setEnabled(enabled);
  }

  public void register(JComponent component) {
    component.addComponentListener(buildComponentListener("Export"));

  }

  private EventHit buildEventHit() {
    return ga.event()
        .applicationVersion(version)
        .applicationName("ODK Briefcase")
        .userId(userId)
        .screenResolution(screenSize)
        .anonymizeIp(true)
        .userAgent(USER_AGENT)
        .dataSource("app")
        .viewportSize(buildResolutionString(windowSizeGetter.get()));
  }

  private ScreenViewHit buildScreenViewHit() {
    return ga.screenView()
        .applicationVersion(version)
        .applicationName("ODK Briefcase")
        .userId(userId)
        .screenResolution(screenSize)
        .anonymizeIp(true)
        .userAgent(USER_AGENT)
        .dataSource("app")
        .viewportSize(buildResolutionString(windowSizeGetter.get()));
  }

  private static String buildResolutionString(Dimension dimension) {
    return String.format("%dx%d", dimension.width, dimension.height);
  }
}
