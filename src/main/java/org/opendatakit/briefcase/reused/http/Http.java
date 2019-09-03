/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.reused.http;

import org.apache.http.HttpHost;
import org.opendatakit.briefcase.reused.http.response.Response;

/**
 * This interface describes how Briefcase interacts with
 * remote systems using the HTTP protocol.
 */
public interface Http {
  int DEFAULT_HTTP_CONNECTIONS = 8;
  int MIN_HTTP_CONNECTIONS = 1;
  int MAX_HTTP_CONNECTIONS = 32;

  static boolean isNotValidHttpConnectionsValue(int value) {
    return value < MIN_HTTP_CONNECTIONS || value > MAX_HTTP_CONNECTIONS;
  }

  <T> Response<T> execute(Request<T> request);

  void setProxy(HttpHost proxy);

  void unsetProxy();

  void setMaxHttpConnections(int maxHttpConnections);
}
