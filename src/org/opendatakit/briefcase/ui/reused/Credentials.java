package org.opendatakit.briefcase.ui.reused;

import java.util.Objects;
import org.apache.http.util.TextUtils;

public class Credentials {
  final private String username;
  final private String password;

  private Credentials(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password);
  }

  public static Credentials from(String username, String password) {
    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
      throw new IllegalArgumentException("You need to provide non-empty username and password.");

    return new Credentials(username, password);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Credentials that = (Credentials) o;
    return Objects.equals(username, that.username) &&
        Objects.equals(password, that.password);
  }

  @Override
  public String toString() {
    return "Credentials{ " +
        "Username = " + username +
        ", Password = " + password;
  }
}
