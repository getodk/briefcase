package org.opendatakit.briefcase.reused.model.preferences;

import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCategory.PULL;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCategory.PUSH;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.HTTP_PROXY_HOST;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.HTTP_PROXY_PORT;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.REMEMBER_PASSWORDS;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.START_PULL_FROM_LAST;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.TRACKING_CONSENT;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Local.currentServerType;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Local.currentServerValue;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.delivery.ui.transfer.sourcetarget.source.PullSource;

public class PreferenceCommands {
  public static Consumer<PreferencePort> setTrackingConsent(boolean value) {
    return port -> port.persist(Preference.of(TRACKING_CONSENT, value));
  }

  public static Consumer<PreferencePort> setHttpProxy(HttpHost proxy) {
    return port -> port.persist(Stream.of(
        Preference.of(HTTP_PROXY_HOST, proxy.getHostName()),
        Preference.of(HTTP_PROXY_PORT, proxy.getPort())
    ));
  }

  public static Consumer<PreferencePort> unsetHttpProxy() {
    return port -> port.remove(Stream.of(HTTP_PROXY_HOST, HTTP_PROXY_PORT));
  }

  public static Consumer<PreferencePort> setMaxHttpConnections(int value) {
    return port -> port.persist(Preference.of(MAX_HTTP_CONNECTIONS, value));
  }

  public static Consumer<PreferencePort> setStartPullFromLast(boolean value) {
    return port -> port.persist(Preference.of(START_PULL_FROM_LAST, value));
  }

  public static Consumer<PreferencePort> setRememberPasswords(boolean value) {
    return port -> port.persist(Preference.of(REMEMBER_PASSWORDS, value));
  }

  public static Consumer<PreferencePort> removeSavedServers() {
    return port -> {
      // Remove currently being used servers in pull and push panels
      port.remove(Stream.of(
          currentServerType(PULL), currentServerType(PUSH),
          currentServerValue(PULL), currentServerValue(PUSH)
      ));
    };
  }

  public static Consumer<PreferencePort> setCurrentServer(PreferenceCategory category, PullSource source) {
    return null;
  }
}
