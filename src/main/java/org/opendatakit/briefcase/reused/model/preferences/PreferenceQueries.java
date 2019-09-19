package org.opendatakit.briefcase.reused.model.preferences;

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

import java.util.Optional;
import java.util.function.Function;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.reused.api.OptionalProduct;

public class PreferenceQueries {
  public static Function<PreferencePort, Boolean> getTrackingConsent() {
    return port -> port.fetch(TRACKING_CONSENT).getValue();
  }

  public static Function<PreferencePort, Boolean> getWelcomeMessageShowed() {
    return port -> port.fetchOptional(WELCOME_MESSAGE_SHOWED).map(Preference::getValue).orElse(false);
  }

  public static Function<PreferencePort, Optional<HttpHost>> getHttpProxy() {
    return port -> OptionalProduct.all(
        port.fetchOptional(HTTP_PROXY_HOST),
        port.fetchOptional(HTTP_PROXY_PORT)
    ).map((host, portNumber) -> new HttpHost(host.getValue(), portNumber.getValue()));
  }

  public static Function<PreferencePort, Optional<Integer>> getMaxHttpConnections() {
    return port -> port.fetchOptional(MAX_HTTP_CONNECTIONS).map(Preference::getValue);
  }

  public static Function<PreferencePort, Boolean> getStartPullFromLast() {
    return port -> port.fetchOptional(START_PULL_FROM_LAST).map(Preference::getValue).orElse(false);
  }

  public static Function<PreferencePort, Boolean> getRememberPasswords() {
    return port -> port.fetchOptional(REMEMBER_PASSWORDS).map(Preference::getValue).orElse(false);
  }

  public static Function<PreferencePort, Optional<SourceOrTarget>> GET_CURRENT_SOURCE = getCurrentSourceOrTarget(PULL);

  public static Function<PreferencePort, Optional<SourceOrTarget>> GET_CURRENT_TARGET = getCurrentSourceOrTarget(PUSH);

  private static Function<PreferencePort, Optional<SourceOrTarget>> getCurrentSourceOrTarget(PreferenceCategory category) {
    return port -> port.fetchOptional(currentSourceOrTarget(category))
        .map(Preference::getValue)
        .map(SourceOrTarget::from);
  }
}
