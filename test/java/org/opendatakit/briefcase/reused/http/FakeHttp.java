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

import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.reused.http.response.Response;

public class FakeHttp implements Http {
  private final List<FakeHttpHandler<?>> handlers = new ArrayList<>();

  public void stub(Request<?> request, Response<InputStream> stub) {
    handlers.add(SimpleFakeHttpHandler.of(request, stub));
  }

  public <T> void stub(Request<T> request, Function<Request<T>, Response<InputStream>> stubSupplier) {
    handlers.add(SimpleFakeHttpHandler.of(request, stubSupplier));
  }

  @SuppressWarnings("unchecked")
  public <T> Response<T> execute(Request<T> request) {
    FakeHttpHandler<T> handler = (FakeHttpHandler<T>) handlers.stream()
        .filter(h -> h.matches(request))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No stub defined for Query " + request.toString()));
    return handler.handle(request);
  }

  @Override
  public void setProxy(HttpHost proxy) {

  }

  @Override
  public void unsetProxy() {

  }

  public <T> RequestSpy<T> spyOn(Request<T> request) {
    SpyFakeHttpHandler<T> spy = new SpyFakeHttpHandler<>(request, ok("stub response"));
    handlers.add(spy);
    return spy.spy();
  }

  public <T> RequestSpy<T> spyOn(Request<T> request, Response<InputStream> responseStub) {
    SpyFakeHttpHandler<T> spy = new SpyFakeHttpHandler<>(request, responseStub);
    handlers.add(spy);
    return spy.spy();
  }

}
