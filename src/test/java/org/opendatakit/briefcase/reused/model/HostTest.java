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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HostTest {

  @Test
  public void knows_if_it_is_a_Linux_host() {
    // As per http://lopica.sourceforge.net/os.html
    withSystemProperty("os.name", "Linux", () -> {
      assertThat(Host.isLinux(), is(true));
      assertThat(Host.isWindows(), is(false));
      assertThat(Host.isMac(), is(false));
      assertThat(Host.getOsName(), is("linux"));
    });
    withSystemProperty("os.name", "Digital Unix", () -> {
      assertThat(Host.isLinux(), is(true));
      assertThat(Host.isWindows(), is(false));
      assertThat(Host.isMac(), is(false));
      assertThat(Host.getOsName(), is("linux"));
    });
    withSystemProperty("os.name", "AIX", () -> {
      assertThat(Host.isLinux(), is(true));
      assertThat(Host.isWindows(), is(false));
      assertThat(Host.isMac(), is(false));
      assertThat(Host.getOsName(), is("linux"));
    });
  }

  @Test
  public void knows_if_it_is_a_Windows_host() {
    withSystemProperty("os.name", "Windows XP", () -> {
      assertThat(Host.isLinux(), is(false));
      assertThat(Host.isWindows(), is(true));
      assertThat(Host.isMac(), is(false));
      assertThat(Host.getOsName(), is("windows"));
    });
  }

  @Test
  public void knows_if_it_is_a_Mac_host() {
    withSystemProperty("os.name", "Mac OS X", () -> {
      assertThat(Host.isLinux(), is(false));
      assertThat(Host.isWindows(), is(false));
      assertThat(Host.isMac(), is(true));
      assertThat(Host.getOsName(), is("mac"));
    });
  }

  private void withSystemProperty(String key, String value, Runnable block) {
    String backupValue = System.getProperty(key);
    System.setProperty(key, value);
    block.run();
    System.setProperty(key, backupValue);
  }
}
