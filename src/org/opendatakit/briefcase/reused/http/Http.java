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

import org.opendatakit.briefcase.reused.http.response.Response;

/**
 * This interface has Briefcase's HTTP API to interact with external services
 */
public interface Http {
  /**
   * Runs a {@link Request} and returns some output value.
   *
   * @param request the {@link Request} to be executed
   * @param <T>   type of the output {@link Response}
   * @return an output value of type T
   */
  <T> Response<T> execute(Request<T> request);
}
