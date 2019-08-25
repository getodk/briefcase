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

package org.opendatakit.briefcase.operations.export;

import java.nio.file.Path;

@FunctionalInterface
public interface SubmissionExportErrorCallback {
  void accept(Path path, String message);

  /**
   * Returns a new callback, result of composing this callback with the given callback
   *
   * @see #compose(SubmissionExportErrorCallback, SubmissionExportErrorCallback)
   */
  default SubmissionExportErrorCallback andThen(SubmissionExportErrorCallback other) {
    return compose(this, other);
  }

  /**
   * Returns a new callback that will run the given callbacks in order
   */
  static SubmissionExportErrorCallback compose(SubmissionExportErrorCallback a, SubmissionExportErrorCallback b) {
    return (path, message) -> {
      a.accept(path, message);
      b.accept(path, message);
    };
  }
}
