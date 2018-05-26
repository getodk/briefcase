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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;

public class CommonsHttp implements Http {
  @Override
  public <T> Response<T> execute(Request<T> request) {
    // Always instantiate a new Executor to avoid side-effects between executions
    Executor executor = Executor.newInstance();
    // Apply auth settings if credentials are received
    request.ifCredentials((URL url, Credentials credentials) -> executor.auth(
        HttpHost.create(url.getHost()),
        credentials.getUsername(),
        credentials.getPassword()
    ));
    // get the response body and let the Request map it
    return uncheckedExecute(request, executor).map(request::map);
  }

  private Response<String> uncheckedExecute(Request<?> request, Executor executor) {
    org.apache.http.client.fluent.Request commonsRequest = getCommonsRequest(request);
    commonsRequest.connectTimeout(10_000);
    commonsRequest.socketTimeout(10_000);
    commonsRequest.addHeader("X-OpenRosa-Version", "1.0");
    request.headers.forEach(pair -> commonsRequest.addHeader(pair.getLeft(), pair.getRight()));
    try {
      return executor
          .execute(commonsRequest)
          .handleResponse(res -> {
            if (res.getStatusLine().getStatusCode() >= 500)
              return new Response.ServerError<>(res.getStatusLine().getStatusCode(), res.getStatusLine().getReasonPhrase());
            if (res.getStatusLine().getStatusCode() >= 400)
              return new Response.ClientError<>(res.getStatusLine().getStatusCode(), res.getStatusLine().getReasonPhrase());
            if (res.getStatusLine().getStatusCode() >= 300)
              return new Response.Redirection<>(res.getStatusLine().getStatusCode(), res.getStatusLine().getReasonPhrase());
            return new Response.Success<>(res.getStatusLine().getStatusCode(), readBody(res));
          });
    } catch (HttpHostConnectException e) {
      throw new HttpException("Connection refused");
    } catch (SocketTimeoutException | ConnectTimeoutException e) {
      throw new HttpException("The connection has timed out");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String readBody(HttpResponse res) {
    return Optional.ofNullable(res.getEntity())
        .map(e -> uncheckedHandleEntity(new BasicResponseHandler(), e))
        .orElse("");
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

  private static <T> T uncheckedHandleEntity(AbstractResponseHandler<T> handler, HttpEntity entity) {
    try {
      return handler.handleEntity(entity);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
