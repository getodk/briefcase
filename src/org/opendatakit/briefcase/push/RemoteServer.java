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

package org.opendatakit.briefcase.push;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Query;

/**
 * This class represents a remote Aggregate server and it has some methods
 * to query its state.
 */
public class RemoteServer {
  private final URI baseUri;
  private final Optional<Credentials> credentials;

  private RemoteServer(URI baseUri, Optional<Credentials> credentials) {
    this.baseUri = baseUri;
    this.credentials = credentials;
  }

  /**
   * Factory that creates a new {@link RemoteServer} to be used when it requires
   * authentication.
   */
  public static RemoteServer authenticated(String baseUri, String username, String password) {
    return new RemoteServer(
        URI.create(baseUri),
        Optional.of(new Credentials(username, password))
    );
  }

  /**
   * Factory that creates a new {@link RemoteServer} to be used when it doesn't
   * require authentication.
   */
  public static RemoteServer normal(String baseUri) {
    return new RemoteServer(URI.create(baseUri), Optional.empty());
  }

  /**
   * Returns a new {@link Query} to check if the given form ID is present in the
   * remote server or not.
   *
   * @return true if the form is present on the remote server, false otherwise
   */
  public Query<Boolean> containsFormQuery(String formId) {
    return buildBaseQuery()
        .resolve("/formList")
        .map((String body) -> Stream.of(body.split("\n"))
            .anyMatch(s -> s.contains("<formID>" + formId + "</formID>")));
  }

  private Query<String> buildBaseQuery() {
    return new Query<>(baseUri, credentials, Function.identity());
  }
}
