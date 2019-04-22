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

package org.opendatakit.briefcase.reused.job;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;

public final class CompletableFutureHelpers {
  private CompletableFutureHelpers() {
  }

  /**
   * Performs the conversion from List&lt;CompletableFuture&lt;T&gt;&gt; to CompletableFuture&lt;List&lt;T&gt;&gt;.
   * <p>
   * It joins each future in a ForkJoinPool thread using {@link CompletableFuture#allOf(CompletableFuture[])}
   */
  static <X, T extends CompletableFuture<X>> Collector<T, ?, CompletableFuture<List<X>>> collectResult() {
    return collectingAndThen(
        toList(),
        ts -> allOf(ts.toArray(new CompletableFuture[0]))
            .thenApply(v -> ts
                .stream()
                .map(CompletableFuture::join)
                .collect(toList()))
    );
  }

}
