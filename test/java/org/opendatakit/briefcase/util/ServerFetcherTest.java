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

package org.opendatakit.briefcase.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.util.ServerFetcher.isUrl;

import org.junit.Test;

public class ServerFetcherTest {

  @Test
  public void knows_if_a_string_contains_a_valid_url() {
    assertThat(isUrl(""), is(false));
    assertThat(isUrl("foo.bar"), is(false));
    assertThat(isUrl("some text"), is(false));
    assertThat(isUrl("http://foo.bar"), is(true));
    assertThat(isUrl("http://foo.bar:1234"), is(true));
    assertThat(isUrl("http://foo.bar/baz"), is(true));
    assertThat(isUrl("http://foo.bar/baz?query=string"), is(true));
  }
}
