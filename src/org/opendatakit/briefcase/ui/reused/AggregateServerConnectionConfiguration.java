package org.opendatakit.briefcase.ui.reused;

import org.opendatakit.briefcase.reused.BriefcaseException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class AggregateServerConnectionConfiguration {
  private URL url;
  private String username;
  private String password;

  public AggregateServerConnectionConfiguration(String url, String username, String password) {
    try {
      this.url = new URL(url);
    } catch (MalformedURLException e) {
      throw new BriefcaseException("Invalid URL", e);
    }

    this.username = username;
    this.password = password;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(String url) {
    try {
      this.url = new URL(url);
    } catch (MalformedURLException e) {
      throw new BriefcaseException("Invalid URL", e);
    }
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AggregateServerConnectionConfiguration that = (AggregateServerConnectionConfiguration) o;
    return Objects.equals(url, that.url) &&
        Objects.equals(username, that.username) &&
        Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, username, password);
  }

}
