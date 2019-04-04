/*
 * Copyright (C) 2019 Nafundi
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

import static org.opendatakit.briefcase.reused.http.RequestMethod.GET;
import static org.opendatakit.briefcase.reused.http.RequestMethod.HEAD;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RequestBuilder<T> {
  private final RequestMethod method;
  private final URL url;
  private Optional<Credentials> credentials = Optional.empty();
  private Function<String, T> bodyMapper;
  private Map<String, String> headers = new HashMap<>();

  private RequestBuilder(RequestMethod method, URL url) {
    this.method = method;
    this.url = url;
  }

  public static RequestBuilder<String> get(URL url) {
    return new RequestBuilder<>(GET, url);
  }

  public static RequestBuilder<String> head(URL url) {
    return new RequestBuilder<>(HEAD, url);
  }

  public Request<T> build() {
    return new Request<>(method, url, credentials, bodyMapper, headers);
  }

  public RequestBuilder<T> withCredentials(Optional<Credentials> maybeCredentials) {
    this.credentials = maybeCredentials;
    return this;
  }

  public RequestBuilder<T> withCredentials(Credentials credentials) {
    this.credentials = Optional.of(credentials);
    return this;
  }
}
