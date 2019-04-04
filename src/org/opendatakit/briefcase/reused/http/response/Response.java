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

package org.opendatakit.briefcase.reused.http.response;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.opendatakit.briefcase.reused.http.HttpException;
import org.opendatakit.briefcase.reused.http.Request;

public interface Response<T> {

  static <U> Response<U> from(Request<U> request, HttpResponse response) {
    int statusCode = response.getStatusLine().getStatusCode();
    String statusPhrase = response.getStatusLine().getReasonPhrase();
    if (statusCode >= 500)
      return new ServerError<>(statusCode, statusPhrase);
    if (statusCode >= 400)
      return new ClientError<>(statusCode, statusPhrase);
    if (statusCode >= 300)
      return new Redirection<>(statusCode, statusPhrase);
    return new Success<>(
        statusCode,
        statusPhrase,
        request.map(Optional.ofNullable(response.getEntity())
            .map(Success::uncheckedGetContent)
            .orElse(new ByteArrayInputStream("".getBytes(UTF_8))))
    );
  }

  T get();

  int getStatusCode();

  <V> Response<V> map(Function<T, V> outputMapper);

  T orElse(T defaultValue);

  T orElseThrow(Supplier<? extends RuntimeException> supplier);

  boolean isSuccess();

  boolean isFailure();

  boolean isUnauthorized();

  boolean isNotFound();

  boolean isRedirection();

  class Success<T> implements Response<T> {
    private final int statusCode;
    private final String statusPhrase;
    private final T output;

    Success(int statusCode, String statusPhrase, T output) {
      this.statusCode = statusCode;
      this.statusPhrase = statusPhrase;
      this.output = output;
    }

    private static InputStream uncheckedGetContent(HttpEntity entity) {
      try {
        return entity.getContent();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public T get() {
      return output;
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public <U> Response<U> map(Function<T, U> outputMapper) {
      return new Success<>(statusCode, statusPhrase, outputMapper.apply(output));
    }

    @Override
    public T orElse(T defaultValue) {
      return output;
    }

    @Override
    public T orElseThrow(Supplier<? extends RuntimeException> supplier) {
      return output;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public boolean isUnauthorized() {
      return false;
    }

    @Override
    public boolean isNotFound() {
      return false;
    }

    @Override
    public boolean isRedirection() {
      return false;
    }
  }

  class Redirection<T> implements Response<T> {
    private final int statusCode;
    private final String statusPhrase;

    Redirection(int statusCode, String statusPhrase) {
      this.statusCode = statusCode;
      this.statusPhrase = statusPhrase;
    }

    @Override
    public T get() {
      throw new HttpException("No output to get");
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public <U> Response<U> map(Function<T, U> outputMapper) {
      return new Redirection<>(statusCode, statusPhrase);
    }

    @Override
    public T orElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T orElseThrow(Supplier<? extends RuntimeException> supplier) {
      throw supplier.get();
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public boolean isUnauthorized() {
      return false;
    }

    @Override
    public boolean isNotFound() {
      return false;
    }

    @Override
    public boolean isRedirection() {
      return true;
    }
  }

  class ClientError<T> implements Response<T> {
    private final int statusCode;
    private final String statusPhrase;

    ClientError(int statusCode, String statusPhrase) {
      this.statusCode = statusCode;
      this.statusPhrase = statusPhrase;
    }

    @Override
    public T get() {
      throw new HttpException("No output to get");
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public <U> Response<U> map(Function<T, U> outputMapper) {
      return new ClientError<>(statusCode, statusPhrase);
    }

    @Override
    public T orElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T orElseThrow(Supplier<? extends RuntimeException> supplier) {
      throw supplier.get();
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public boolean isUnauthorized() {
      return statusCode == 401;
    }

    @Override
    public boolean isNotFound() {
      return statusCode == 404;
    }

    @Override
    public boolean isRedirection() {
      return false;
    }
  }

  class ServerError<T> implements Response<T> {
    private final int statusCode;
    private final String statusPhrase;

    ServerError(int statusCode, String statusPhrase) {
      this.statusCode = statusCode;
      this.statusPhrase = statusPhrase;
    }

    @Override
    public T get() {
      throw new HttpException("No output to get");
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public <U> Response<U> map(Function<T, U> outputMapper) {
      return new ServerError<>(statusCode, statusPhrase);
    }

    @Override
    public T orElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T orElseThrow(Supplier<? extends RuntimeException> supplier) {
      throw supplier.get();
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public boolean isUnauthorized() {
      return false;
    }

    @Override
    public boolean isNotFound() {
      return false;
    }

    @Override
    public boolean isRedirection() {
      return false;
    }
  }
}
