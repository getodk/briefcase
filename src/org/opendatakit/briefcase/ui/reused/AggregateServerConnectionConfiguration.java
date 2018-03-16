package org.opendatakit.briefcase.ui.reused;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.opendatakit.briefcase.reused.BriefcaseException;

public class AggregateServerConnectionConfiguration {
  final private URL url;
  final private Optional<Credentials> credentials;

  private AggregateServerConnectionConfiguration(URL url, Optional<Credentials> credentials) {
    this.url = url;
    this.credentials = credentials;
  }

  public static AggregateServerConnectionConfiguration from(String urlText, String username, char[] password) {
    URL url;
    try {
      url = new URL(urlText);
    } catch (MalformedURLException e) {
      throw new BriefcaseException("Invalid URL " + urlText, e);
    }

    Optional<String> optUsername = Optional.ofNullable(username);
    Optional<String> optPassword = Optional.of(password).map(Arrays::toString).filter(String::isEmpty);
    Optional<Credentials> from = optUsername.isPresent() && optPassword.isPresent()
        ? Optional.of(Credentials.from(optUsername.get(), optPassword.get()))
        : Optional.empty();

    return new AggregateServerConnectionConfiguration(url, from);
  }

  public URL getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AggregateServerConnectionConfiguration that = (AggregateServerConnectionConfiguration) o;
    return Objects.equals(url, that.url) &&
        Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, credentials);
  }

  @Override
  public String toString() {
    return "Aggregate Server Configuration{" + "" +
        "URL = " + url +
        ", Credentials = " + credentials;
  }
}
