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

package org.opendatakit.briefcase.reused;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

public class UncheckedFiles {
  public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) {
    try {
      return Files.createTempFile(prefix, suffix, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path write(Path path, byte[] bytes, OpenOption... options) {
    try {
      return Files.write(path, bytes, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path createDirectories(Path dir, FileAttribute<?>... attrs) {
    try {
      return Files.createDirectories(dir, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path copy(Path source, Path target, CopyOption... options) {
    try {
      return Files.copy(source, target, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
