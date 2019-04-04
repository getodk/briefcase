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

import static org.apache.http.client.config.CookieSpecs.STANDARD;
import static org.apache.http.client.config.RequestConfig.custom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

public class CommonsHttp implements Http {
  private final Executor executor;

  private CommonsHttp(Executor executor) {
    this.executor = executor;
  }

  public static Http nonReusing() {
    return new CommonsHttp(Executor.newInstance(HttpClientBuilder
        .create()
        .setConnectionManager(new BasicHttpClientConnectionManager())
        .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
        .setDefaultRequestConfig(custom().setCookieSpec(STANDARD).build())
        .build()));
  }

  public static Http reusing() {
    return new CommonsHttp(Executor.newInstance(HttpClientBuilder
        .create()
        .setDefaultRequestConfig(custom().setCookieSpec(STANDARD).build())
        .build()));
  }

  @Override
  public <T> Response<T> execute(Request<T> request) {
    // Apply auth settings if credentials are received
    request.ifCredentials((URL url, Credentials credentials) -> executor.auth(
        HttpHost.create(url.getHost()),
        credentials.getUsername(),
        credentials.getPassword()
    ));
    // get the response body and let the Request map it
    return uncheckedExecute(request, executor);
  }

  private <T> Response<T> uncheckedExecute(Request<T> request, Executor executor) {
    org.apache.http.client.fluent.Request commonsRequest = getCommonsRequest(request);
    commonsRequest.connectTimeout(10_000);
    commonsRequest.socketTimeout(10_000);
    commonsRequest.addHeader("X-OpenRosa-Version", "1.0");
    request.headers.forEach(commonsRequest::addHeader);
    try {
      return executor
          .execute(commonsRequest)
          .handleResponse(res -> Response.from(request, res));
    } catch (HttpHostConnectException e) {
      throw new HttpException("Connection refused");
    } catch (SocketTimeoutException | ConnectTimeoutException e) {
      throw new HttpException("The connection has timed out");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static org.apache.http.client.fluent.Request getCommonsRequest(Request<?> request) {
    switch (request.getMethod()) {
      case GET:
        return org.apache.http.client.fluent.Request.Get(request.asUri());
      case HEAD:
        return org.apache.http.client.fluent.Request.Head(request.asUri());
      default:
        throw new HttpException("Method " + request.getMethod() + " is not supported");
    }
  }
}
