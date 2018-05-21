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

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This Value Object class represents a query to some {@link URI}, maybe using
 * some {@link Credentials} for authentication.
 * <p>
 * It also gives type hints about the result calling sites would be able to expect
 * when executed.
 */
public class Query<T> {
  private final URI uri;
  private final Optional<Credentials> credentials;
  private final Function<String, T> bodyMapper;

  /**
   * Main constructor for {@link Query} instances.
   *
   * @param uri         the {@link URI} this query must be sent to
   * @param credentials the authentication {@link Credentials}, wrapped inside
   *                    an {@link Optional}, or an {@link Optional#empty()} if
   *                    no credentials are required
   * @param bodyMapper  a {@link Function} that takes the output body from the
   *                    request and maps it to some other type
   */
  public Query(URI uri, Optional<Credentials> credentials, Function<String, T> bodyMapper) {
    this.uri = uri;
    this.credentials = credentials;
    this.bodyMapper = bodyMapper;
  }

  /**
   * Factory to create new {@link Query} instances for authenticated requests.
   */
  public static Query<String> authenticated(String uri, String username, String password) {
    return new Query<>(
        URI.create(uri),
        Optional.of(new Credentials(username, password)),
        Function.identity()
    );
  }

  /**
   * Factory to create new {@link Query} instances for non authenticated ("normal", lacking of
   * a better name) requests.
   */
  public static Query<String> normal(String uri) {
    return new Query<>(URI.create(uri), Optional.empty(), Function.identity());
  }

  /**
   * Return a new {@link Query} pointing to other {@link URI} obtained by resolving
   * the given path.
   */
  public Query<T> resolve(String path) {
    return new Query<>(uri.resolve(path), credentials, bodyMapper);
  }

  /**
   * Map a body {@link String} using the {@link Query#bodyMapper} and return the result.
   */
  public T map(String body) {
    return bodyMapper.apply(body);
  }

  /**
   * Return a new {@link Query} that will use the given {@link Function} as its {@link Query#bodyMapper}
   *
   * @see Query#map(String)
   */
  public <U> Query<U> map(Function<T, U> newBodyMapper) {
    return new Query<>(uri, credentials, bodyMapper.andThen(newBodyMapper));
  }

  /**
   * Run the given consumer if this {@link Query} has some credentials defined.
   */
  public void ifCredentials(BiConsumer<URI, Credentials> consumer) {
    credentials.ifPresent(c -> consumer.accept(uri, c));
  }

  /**
   * Return this {@link Query} instance's {@link URI} member
   */
  public URI getUri() {
    return uri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Query<?> query = (Query<?>) o;
    return Objects.equals(uri, query.uri) &&
        Objects.equals(credentials, query.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, credentials);
  }

  @Override
  public String toString() {
    return "GET " + uri + " " + credentials.map(Credentials::toString).orElse("no credentials") + ")";
  }
}
