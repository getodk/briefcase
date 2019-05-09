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
import static org.opendatakit.briefcase.reused.http.RequestMethod.POST;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URL;
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
import org.opendatakit.briefcase.reused.http.response.Response;

public class CommonsHttp implements Http {
  private Executor executor;
  private final int maxConnections;

  private CommonsHttp(Executor executor, int maxConnections) {
    this.executor = executor;
    this.maxConnections = maxConnections;
  }

  public static Http of(int maxConnections, HttpHost httpProxy) {
    return new CommonsHttp(Executor.newInstance(HttpClientBuilder
        .create()
        .setMaxConnPerRoute(maxConnections)
        .setMaxConnTotal(maxConnections)
        .setProxy(httpProxy)
        .setDefaultRequestConfig(custom().setCookieSpec(STANDARD).build())
        .build()), maxConnections);
  }

  public static Http of(int maxConnections) {
    return new CommonsHttp(Executor.newInstance(HttpClientBuilder
        .create()
        .setMaxConnPerRoute(maxConnections)
        .setMaxConnTotal(maxConnections)
        .setDefaultRequestConfig(custom().setCookieSpec(STANDARD).build())
        .build()), maxConnections);
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
    executor = Executor.newInstance(HttpClientBuilder
        .create()
        .setMaxConnPerRoute(maxConnections)
        .setMaxConnTotal(maxConnections)
        .setProxy(proxy)
        .setDefaultRequestConfig(custom().setCookieSpec(STANDARD).build())
        .build());
  }

  @Override
  public void unsetProxy() {
    executor = Executor.newInstance(HttpClientBuilder
        .create()
        .setMaxConnPerRoute(maxConnections)
        .setMaxConnTotal(maxConnections)
        .setDefaultRequestConfig(custom().setCookieSpec(STANDARD).build())
        .build());
  }

  private <T> Response<T> uncheckedExecute(Request<T> request, Executor executor) {
    // Get an Apache Commons HTTPClient request and set some reasonable timeouts
    org.apache.http.client.fluent.Request commonsRequest = getCommonsRequest(request);
    commonsRequest.connectTimeout(10_000);
    commonsRequest.socketTimeout(10_000);

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
      case POST:
        return org.apache.http.client.fluent.Request.Post(request.asUri());
      case HEAD:
        return org.apache.http.client.fluent.Request.Head(request.asUri());
      default:
        throw new HttpException("Method " + request.getMethod() + " is not supported");
    }
  }
}
