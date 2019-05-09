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

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * Stores information to execute an HTTP request and provides an API to
 * transform and obtain the output of the related HTTP response in a
 * type-safe way.
 * <p>
 * Build instances of Request with {@link RequestBuilder} class
 */
public class Request<T> {
  private final RequestMethod method;
  private final URL url;
  private final Optional<Credentials> credentials;
  private final Function<InputStream, T> responseMapper;
  final Map<String, String> headers;
  private final Optional<InputStream> body;
  final List<MultipartMessage> multipartMessages;

  Request(RequestMethod method, URL url, Optional<Credentials> credentials, Function<InputStream, T> responseMapper, Map<String, String> headers, Optional<InputStream> body, List<MultipartMessage> multipartMessages) {
    this.method = method;
    this.url = url;
    this.credentials = credentials;
    this.responseMapper = responseMapper;
    this.headers = headers;
    this.body = body;
    this.multipartMessages = multipartMessages;
  }

  public T map(InputStream responseBody) {
    return responseMapper.apply(responseBody);
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

  // TODO v2.0 Move this to RequestBuilder, with uri() and isUri()
  URI asUri() {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new BriefcaseException(e);
    }
  }

  /**
   * Returns a RequestBuilder that would produce this instance when built.
   */
  public RequestBuilder<T> builder() {
    return new RequestBuilder<>(method, url, responseMapper, credentials, headers, body, multipartMessages);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Request<?> request = (Request<?>) o;
    return method == request.method &&
        url.equals(request.url) &&
        credentials.equals(request.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, url, credentials);
  }

  @Override
  public String toString() {
    return method + " " + url + " " + credentials.map(Credentials::toString).orElse("(no credentials)");
  }

  boolean isMultipart() {
    return !multipartMessages.isEmpty();
  }

  public InputStream getBody() {
    return body.orElseThrow(BriefcaseException::new);
  }
}
