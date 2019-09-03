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

import static org.apache.http.client.config.CookieSpecs.IGNORE_COOKIES;
import static org.apache.http.client.config.RequestConfig.custom;
import static org.opendatakit.briefcase.reused.http.RequestMethod.POST;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.http.response.Response;

public class CommonsHttp implements Http {
  private Executor executor;
  private int maxHttpConnections;
  private Optional<HttpHost> proxy;

  private CommonsHttp(Executor executor, int maxHttpConnections, Optional<HttpHost> proxy) {
    this.executor = executor;
    this.maxHttpConnections = maxHttpConnections;
    this.proxy = proxy;
  }

  public static Http create() {
    return new CommonsHttp(Executor.newInstance(
        getBaseBuilder(DEFAULT_HTTP_CONNECTIONS).build()
    ), DEFAULT_HTTP_CONNECTIONS, Optional.empty());
  }

  public static Http of(int maxConnections, Optional<HttpHost> proxy) {
    if (Http.isNotValidHttpConnectionsValue(maxConnections))
      throw new BriefcaseException("Invalid maximum simultaneous HTTP connections " + maxConnections + ". Try a value between " + MIN_HTTP_CONNECTIONS + " and " + MAX_HTTP_CONNECTIONS);
    return new CommonsHttp(
        proxy.map(host -> Executor.newInstance(getBaseBuilder(maxConnections).setProxy(host).build()))
            .orElseGet(() -> Executor.newInstance(getBaseBuilder(maxConnections).build())),
        maxConnections,
        proxy
    );
  }

  private static HttpClientBuilder getBaseBuilder(int maxConnections) {
    return HttpClientBuilder
        .create()
        .setMaxConnPerRoute(maxConnections)
        .setMaxConnTotal(maxConnections)
        .setDefaultRequestConfig(custom()
            .setConnectionRequestTimeout(0)
            .setSocketTimeout(0)
            .setConnectTimeout(0)
            .setCookieSpec(IGNORE_COOKIES)
            .build());
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

  @Override
  public void setProxy(HttpHost proxy) {
    this.proxy = Optional.of(proxy);
    rebuildExecutor();
  }

  @Override
  public void unsetProxy() {
    proxy = Optional.empty();
    rebuildExecutor();
  }

  @Override
  public void setMaxHttpConnections(int maxHttpConnections) {
    this.maxHttpConnections = maxHttpConnections;
    rebuildExecutor();
  }

  private void rebuildExecutor() {
    executor = proxy
        .map(httpHost -> Executor.newInstance(getBaseBuilder(maxHttpConnections).setProxy(httpHost).build()))
        .orElseGet(() -> Executor.newInstance(getBaseBuilder(maxHttpConnections).build()));
  }

  private <T> Response<T> uncheckedExecute(Request<T> request, Executor executor) {
    // Get an Apache Commons HTTPClient request and set some reasonable timeouts
    org.apache.http.client.fluent.Request commonsRequest = getCommonsRequest(request);

    // Add the declared headers
    // TODO v2.0 remove this header, since this is not a concern of this class
    commonsRequest.addHeader("X-OpenRosa-Version", "1.0");
    request.headers.forEach(commonsRequest::addHeader);

    // Set the request's body if it's a POST request
    if (request.getMethod() == POST) {
      HttpEntity body;
      if (request.isMultipart()) {
        MultipartEntityBuilder bodyBuilder = MultipartEntityBuilder.create();
        for (MultipartMessage part : request.multipartMessages)
          bodyBuilder = bodyBuilder.addPart(
              part.name,
              new InputStreamBody(part.body, ContentType.create(part.contentType), part.attachmentName)
          );
        body = bodyBuilder.build();
      } else {
        body = new BasicHttpEntity();
        ((BasicHttpEntity) body).setContent(request.getBody());
      }
      commonsRequest.body(body);
    }
    try {
      // Send the request and handle the response
      return executor
          .execute(commonsRequest)
          .handleResponse(res -> Response.from(request, res));
    } catch (HttpHostConnectException e) {
      throw new HttpException("Connection refused", e);
    } catch (SocketTimeoutException | ConnectTimeoutException e) {
      throw new HttpException("The connection has timed out", e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static org.apache.http.client.fluent.Request getCommonsRequest(Request<?> request) {
    switch (request.getMethod()) {
      case GET:
        return org.apache.http.client.fluent.Request.Get(request.asUri());
      case POST:
        return org.apache.http.client.fluent.Request.Post(request.asUri());
      case HEAD:
        return org.apache.http.client.fluent.Request.Head(request.asUri());
      default:
        throw new HttpException("Method " + request.getMethod() + " is not supported");
    }
  }
}
