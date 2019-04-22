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

package org.opendatakit.briefcase.reused.http;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import org.junit.Test;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Pair;

public class RequestBuilderTest {

  @Test(expected = BriefcaseException.class)
  public void should_reject_calling_withPath_if_baseUrl_already_has_a_path() {
    RequestBuilder.get("http://foo.bar/baz")
        .withPath("/bing");
  }

  @Test(expected = BriefcaseException.class)
  public void should_reject_calling_withQuery_if_baseUrl_already_has_a_query() {
    RequestBuilder.get("http://foo.bar/baz?bing=bong")
        .withQuery(Pair.of("beep", "bop"));
  }

  @Test
  public void can_resolve_paths() {
    // No slashes on base and the path
    assertThat(
        RequestBuilder.get("http://foo.com").withPath("bar/baz").build().getUrl(),
        is(url("http://foo.com/bar/baz"))
    );

    // Ending slash on base only
    assertThat(
        RequestBuilder.get("http://foo.com/").withPath("bar/baz").build().getUrl(),
        is(url("http://foo.com/bar/baz"))
    );

    // Ending slash on base and starting slash on path
    assertThat(
        RequestBuilder.get("http://foo.com/").withPath("/bar/baz").build().getUrl(),
        is(url("http://foo.com/bar/baz"))
    );

    // Starting slash on path only
    assertThat(
        RequestBuilder.get("http://foo.com").withPath("/bar/baz").build().getUrl(),
        is(url("http://foo.com/bar/baz"))
    );
  }
}
