package org.opendatakit.briefcase.reused.model.preferences;

import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.HTTP_PROXY_HOST;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.HTTP_PROXY_PORT;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.REMEMBER_PASSWORDS;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.START_PULL_FROM_LAST;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceKey.Global.TRACKING_CONSENT;

import java.util.Optional;
import java.util.function.Function;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.reused.api.OptionalProduct;

public class PreferenceQueries {
  public static Function<PreferencePort, Boolean> getTrackingConsent() {
    return port -> port.fetch(TRACKING_CONSENT).getValue();
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

  public static Function<PreferencePort, Optional<Boolean>> getStartPullFromLast() {
    return port -> port.fetchOptional(START_PULL_FROM_LAST).map(Preference::getValue);
  }

  public static Function<PreferencePort, Optional<Boolean>> getRememberPasswords() {
    return port -> port.fetchOptional(REMEMBER_PASSWORDS).map(Preference::getValue);
  }
}
