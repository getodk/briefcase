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

import static java.util.Collections.emptyMap;
import static org.opendatakit.briefcase.reused.http.RequestMethod.HEAD;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This Value Object class represents an HTTP request to some {@link URL}, maybe using
 * some {@link Credentials} for authentication.
 * <p>
 * It also gives type hints about the result calling sites would be able to expect
 * when executed.
 */
public class Request<T> {
  private final RequestMethod method;
  private final URL url;
  private final Optional<Credentials> credentials;
  private final Function<String, T> bodyMapper;
  final Map<String, String> headers;

  Request(RequestMethod method, URL url, Optional<Credentials> credentials, Function<String, T> bodyMapper, Map<String, String> headers) {
    this.method = method;
    this.url = url;
    this.credentials = credentials;
    this.bodyMapper = bodyMapper;
    this.headers = headers;
  }

  public static Request<String> head(URL url) {
    return new Request<>(HEAD, url, Optional.empty(), Function.identity(), emptyMap());
  }

  public static Request<String> head(URL url, Optional<Credentials> credentials) {
    return new Request<>(HEAD, url, credentials, Function.identity(), emptyMap());
  }

  public Request<T> resolve(String path) {
    // Normalize slashes to ensure that the resulting url
    // has exactly one slash before the input path
    String newUrl = url.toString()
        + (!url.toString().endsWith("/") ? "/" : "")
        + (path.startsWith("/") ? path.substring(1) : path);
    return new Request<>(method, url(newUrl), credentials, bodyMapper, headers);
  }

  private static URL url(String baseUrl) {
    try {
      return new URL(baseUrl);
    } catch (MalformedURLException e) {
      throw new BriefcaseException(e);
    }
  }

  public T map(String body) {
    return bodyMapper.apply(body);
  }

  public <U> Request<U> withMapper(Function<T, U> newBodyMapper) {
    return new Request<>(method, url, credentials, bodyMapper.andThen(newBodyMapper), headers);
  }

  void ifCredentials(BiConsumer<URL, Credentials> consumer) {
    credentials.ifPresent(c -> consumer.accept(url, c));
  }

  public URL getUrl() {
    return url;
  }

  public RequestMethod getMethod() {
    return method;
  }

  URI asUri() {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new BriefcaseException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Request<?> request = (Request<?>) o;
    return Objects.equals(url, request.url) &&
        Objects.equals(credentials, request.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, credentials);
  }

  @Override
  public String toString() {
    return method + " " + url + " " + credentials.map(Credentials::toString).orElse("(no credentials)");
  }

}
