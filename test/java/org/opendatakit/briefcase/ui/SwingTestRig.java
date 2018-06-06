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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.InMemoryFormCache;

public class SwingTestRig {

  public static Path classPath(String location) {
    String absoluteLocation = location.startsWith("/") ? location : "/" + location;
    return Paths.get(uncheckedURLtoURI(SwingTestRig.class.getResource(absoluteLocation)));
  }

  public static void createInMemoryCache() {
    FileSystemUtils.setFormCache(new InMemoryFormCache());
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
}
