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
import org.opendatakit.briefcase.reused.http.response.Response;

public class SpyFakeHttpHandler<T> implements FakeHttpHandler<T> {
  private final Request<T> expectedRequest;
  private final Response<InputStream> responseStub;
  private final RequestSpy<T> requestSpy;

  public SpyFakeHttpHandler(Request<T> expectedRequest, Response<InputStream> responseStub) {
    this.expectedRequest = expectedRequest;
    this.responseStub = responseStub;
    this.requestSpy = new RequestSpy<>();
  }

  @Override
  public boolean matches(Request<?> request) {
    return this.expectedRequest.equals(request);
  }

  @Override
  public Response<T> handle(Request<T> request) {
    requestSpy.track(request);
    return responseStub.map(request::map);
  }

  public RequestSpy<T> spy() {
    return requestSpy;
  }
}
