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

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.opendatakit.briefcase.util.WebUtils;

public class ServerConnectionInfo {
  private String url;
  private final String token; // for legacy ODK Aggregate 0.9.8 access
  private final String username;
  private final char[] password;
  private final boolean isOpenRosaServer;
  private HttpClientContext httpContext = null;

  public ServerConnectionInfo(String url, String username, char[] cs) {
    this.url = url;
    this.username = username;
    this.password = cs;
    this.token = null;
    this.isOpenRosaServer = true;
  }

  public ServerConnectionInfo(String url, String token) {
    this.url = url;
    this.token = token;
    this.username = null;
    this.password = null;
    this.isOpenRosaServer = false;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getToken() {
    return token;
  }
  
  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

  public HttpClient getHttpClient() {
    return WebUtils.createHttpClient();
  }

  public HttpClientContext getHttpContext() {
    return httpContext;
  }

  public void setHttpContext(HttpClientContext httpContext) {
    this.httpContext = httpContext;
  }

  public boolean isOpenRosaServer() {
    return isOpenRosaServer;
  }
}
