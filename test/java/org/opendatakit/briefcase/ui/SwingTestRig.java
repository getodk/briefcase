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
package org.opendatakit.briefcase.ui;

import static org.opendatakit.briefcase.util.FileSystemUtils.getMd5Hash;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.InMemoryFormCache;

public class SwingTestRig {

  public static void installFormsFrom(Path path) {
    uncheckedWalk(path).forEach(sourcePath -> {
      BriefcaseFormDefinition briefcaseFormDefinition = readForm(path, sourcePath);
      FileSystemUtils.formCache.putFormFileMd5Hash(sourcePath.toString(), getMd5Hash(sourcePath.toFile()));
      FileSystemUtils.formCache.putFormFileFormDefinition(sourcePath.toString(), briefcaseFormDefinition);
    });
  }

  private static BriefcaseFormDefinition readForm(Path path, Path sourcePath) {
    try {
      return new BriefcaseFormDefinition(path.toFile(), sourcePath.toFile());
    } catch (BadFormDefinition badFormDefinition) {
      throw new RuntimeException(badFormDefinition);
    }
  }

  public static Path classPath(String location) {
    return Paths.get(uncheckedURLtoURI(SwingTestRig.class.getResource(location)));
  }

  public static void createInMemoryCache() {
    FileSystemUtils.formCache = new InMemoryFormCache();
  }

  public static void uncheckedSleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI uncheckedURLtoURI(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static Stream<Path> uncheckedWalk(Path path) {
    try {
      return Files.walk(path).filter(p -> Files.isRegularFile(p));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void uncheckedCopy(Path source, Path destination, CopyOption... copyOptions) {
    try {
      Files.copy(source, destination, copyOptions);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
