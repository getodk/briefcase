package org.opendatakit.briefcase.ui.export;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

public class AggregateServerConnectionConfiguration {
  private URL url;
  private String username;
  private char[] password;

  public AggregateServerConnectionConfiguration(String url, String username, char[] password) throws MalformedURLException {
    this.url = new URL(url);
    this.username = username;
    this.password = password;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(String url) throws MalformedURLException {
    this.url = new URL(url);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public char[] getPassword() {
    return password;
  }

  public void setPassword(char[] password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AggregateServerConnectionConfiguration that = (AggregateServerConnectionConfiguration) o;
    return Objects.equals(url, that.url) &&
        Objects.equals(username, that.username) &&
        Arrays.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, username, password);
  }

}
