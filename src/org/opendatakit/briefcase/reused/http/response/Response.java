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

import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.http.HttpResponse;
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
    return Success.from(request, response);
  }

  T get();

  int getStatusCode();

  <V> Response<V> map(Function<T, V> outputMapper);

  T orElse(T defaultValue);

  T orElseThrow(Supplier<? extends RuntimeException> supplier);

  boolean isSuccess();

  boolean isUnauthorized();

  boolean isNotFound();

  boolean isRedirection();

}
