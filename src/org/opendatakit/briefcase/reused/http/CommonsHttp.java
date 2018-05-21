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
import java.net.URI;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

public class CommonsHttp implements Http {
  @Override
  public <T> T execute(Query<T> query) {
    // Always instantiate a new Executor to avoid side-effects between executions
    Executor executor = Executor.newInstance();
    // Apply auth settings if credentials are received
    query.ifCredentials((URI uri, Credentials credentials) -> executor.auth(
        HttpHost.create(uri.getHost()),
        credentials.getUsername(),
        credentials.getPassword()
    ));
    // get the response body and let the Query map it
    return query.map(uncheckedExecute(query, executor));
  }

  private <T> String uncheckedExecute(Query<T> query, Executor executor) {
    try {
      Request request = Request.Get(query.getUri())
          .addHeader("X-OpenRosa-Version", "1.0");
      return executor.execute(request)
          .returnContent()
          .asString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
