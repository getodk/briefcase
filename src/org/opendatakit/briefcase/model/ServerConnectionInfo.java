/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.model;

import java.util.Arrays;
import java.util.Objects;

public class ServerConnectionInfo {

  private String url;
  private final String username;
  private final char[] password;

  public ServerConnectionInfo(String url, String username, char[] cs) {
    this.url = url;
    this.username = username;
    this.password = cs;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

  public boolean hasCredentials() {
    return getUsername() != null && getUsername().length() != 0;
  }

  @Override
  public String toString() {
    return "ServerConnectionInfo{" +
        "url='" + url + '\'' +
        ", username='" + username + '\'' +
        ", password=" + String.valueOf(password).replaceAll(".", "*") +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ServerConnectionInfo that = (ServerConnectionInfo) o;
    return Objects.equals(url, that.url) &&
        Objects.equals(username, that.username) &&
        Arrays.equals(password, that.password);
  }

  @Override
  public int hashCode() {

    int result = Objects.hash(url, username);
    result = 31 * result + Arrays.hashCode(password);
    return result;
  }
}
