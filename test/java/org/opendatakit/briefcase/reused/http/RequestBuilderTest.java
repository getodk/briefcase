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
  public void should_reject_base_URLs_containing_paths() {
    RequestBuilder.get("http://foo.bar/baz");
  }

  @Test(expected = BriefcaseException.class)
  public void should_reject_base_URLs_containing_query_strings() {
    RequestBuilder.get("http://foo.bar?key=value");
  }

  @Test(expected = BriefcaseException.class)
  public void should_reject_calling_withPath_twice() {
    RequestBuilder.get("http://foo.bar")
        .withPath("/foo")
        .withPath("/bar");
  }

  @Test(expected = BriefcaseException.class)
  public void should_reject_calling_withQuery_twice() {
    RequestBuilder.get("http://foo.bar")
        .withQuery(Pair.of("foo", "bar"))
        .withQuery(Pair.of("bar", "baz"));
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
