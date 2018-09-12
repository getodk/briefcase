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

import java.util.Optional;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class HttpException extends BriefcaseException {
  private final Optional<Response<?>> response;

  public HttpException(String message) {
    super(message);
    response = Optional.empty();
  }

  public HttpException(String message, Throwable cause) {
    super(message, cause);
    response = Optional.empty();
  }

  public HttpException(Response<?> response) {
    super("HTTP Response status code " + response.getStatusCode());
    this.response = Optional.of(response);
  }

  public HttpException(Response<?> response, Throwable cause) {
    super("HTTP Response status code " + response.getStatusCode(), cause);
    this.response = Optional.of(response);
  }
}
