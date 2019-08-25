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

package org.opendatakit.briefcase.reused.model;

import static org.opendatakit.briefcase.reused.model.Host.OperatingSystem.LINUX;
import static org.opendatakit.briefcase.reused.model.Host.OperatingSystem.MAC;
import static org.opendatakit.briefcase.reused.model.Host.OperatingSystem.WIN;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class Host {
  public static boolean isMac() {
    return OperatingSystem.get() == MAC;
  }

  public static boolean isLinux() {
    return OperatingSystem.get() == LINUX;
  }

  public static boolean isWindows() {
    return OperatingSystem.get() == WIN;
  }

  public static String getOsName() {
    return OperatingSystem.get().name;
  }

  enum OperatingSystem {
    WIN("windows", "windows"),
    LINUX("linux", "nix", "nux", "aix"),
    MAC("mac", "mac");

    private final String name;
    private final List<String> tests;

    OperatingSystem(String name, String... tests) {
      this.name = name;
      this.tests = Arrays.asList(tests);
    }

    static OperatingSystem get() {
      String rawOs = System.getProperty("os.name").toLowerCase();
      return Stream.of(WIN, LINUX, MAC)
          .filter(operatingSystem -> operatingSystem.test(rawOs))
          .findFirst()
          .orElseThrow(() -> new BriefcaseException("Unrecognized operating system " + rawOs));
    }

    private boolean test(String os) {
      return tests.stream().anyMatch(os::contains);
    }
  }
}
