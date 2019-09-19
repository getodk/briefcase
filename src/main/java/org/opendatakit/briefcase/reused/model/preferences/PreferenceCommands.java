package org.opendatakit.briefcase.reused.model.preferences;

import static org.opendatakit.briefcase.reused.api.Json.getMapper;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCategory.PULL;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCategory.PUSH;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.HTTP_PROXY_HOST;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.HTTP_PROXY_PORT;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.REMEMBER_PASSWORDS;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.START_PULL_FROM_LAST;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.TRACKING_CONSENT;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.WELCOME_MESSAGE_SHOWED;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Local.currentSourceOrTarget;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;

public class PreferenceCommands {
  public static Consumer<PreferencePort> setTrackingConsent(boolean value) {
    return port -> port.persist(Preference.of(TRACKING_CONSENT, value));
  }

  public static Consumer<PreferencePort> setWelcomeMessageShowed(boolean value) {
    return port -> port.persist(Preference.of(WELCOME_MESSAGE_SHOWED, value));
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

  public static Consumer<PreferencePort> setCurrentSource(SourceOrTarget source) {
    return setCurrentSourceOrTarget(PULL, source);
  }

  public static Consumer<PreferencePort> setCurrentTarget(SourceOrTarget target) {
    return setCurrentSourceOrTarget(PUSH, target);
  }

  public static Consumer<PreferencePort> removeCurrentSource() {
    return removeCurrentSourceOrTarget(PULL);
  }

  public static Consumer<PreferencePort> removeCurrentTarget() {
    return removeCurrentSourceOrTarget(PUSH);
  }

  private static Consumer<PreferencePort> removeCurrentSourceOrTarget(PreferenceCategory category) {
    return port -> port.remove(currentSourceOrTarget(category));
  }

  private static Consumer<PreferencePort> setCurrentSourceOrTarget(PreferenceCategory category, SourceOrTarget sourceOrTarget) {
    return port -> port.persist(Preference.of(currentSourceOrTarget(category), sourceOrTarget.asJson(getMapper())));
  }
}
