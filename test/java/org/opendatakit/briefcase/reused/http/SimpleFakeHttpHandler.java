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

package org.opendatakit.briefcase.reused.http;

import java.io.InputStream;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.http.response.Response;

class SimpleFakeHttpHandler<T> implements FakeHttpHandler<T> {
  private final Request<T> request;
  private final Function<Request<T>, Response<InputStream>> responseProvider;

  protected SimpleFakeHttpHandler(Request<T> request, Function<Request<T>, Response<InputStream>> responseProvider) {
    this.request = request;
    this.responseProvider = responseProvider;
  }

  public static <U> SimpleFakeHttpHandler<U> of(Request<U> request, Response<InputStream> stub) {
    return new SimpleFakeHttpHandler<>(request, __ -> stub);
  }

  public static <U> SimpleFakeHttpHandler<U> of(Request<U> request, Function<Request<U>, Response<InputStream>> stubProvider) {
    return new SimpleFakeHttpHandler<>(request, stubProvider);
  }

  @Override
  public boolean matches(Request<?> request) {
    return this.request.equals(request);
  }

  @Override
  public Response<T> handle(Request<T> request) {
    return responseProvider.apply(request).map(request::map);
  }
}
