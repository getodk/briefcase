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
import java.io.InputStream;

public class ResponseHelpers {
  public static Response<InputStream> ok(String body) {
    return new Success<>(200, "OK", new ByteArrayInputStream(body.getBytes(UTF_8)));
  }

  public static Response<Boolean> found() {
    return new Redirection<>(302, "Found");
  }

  public static Response<Boolean> unauthorized() {
    return new ClientError<>(401, "Unauthorized", "");
  }

  public static Response<Boolean> notFound() {
    return new ClientError<>(404, "Not Found", "");
  }

  public static Response<InputStream> notFoundInputStream() {
    return new ClientError<>(404, "Not Found", "");
  }
}
