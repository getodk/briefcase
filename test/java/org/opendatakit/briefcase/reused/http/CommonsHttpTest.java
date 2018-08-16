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

package org.opendatakit.briefcase.reused.http;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import org.apache.http.client.utils.DateUtils;
import org.junit.Test;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class CommonsHttpTest {
  @Test
  public void can_execute_a_GET_request() throws MalformedURLException {
    Http http = new CommonsHttp();
    Response<String> output = http.execute(Request.get(new URL("https://docs.opendatakit.org/")));
    assertThat(output.orElseThrow(BriefcaseException::new), containsString("Open Data Kit"));
  }

  @Test
  public void can_execute_a_HEAD_request() throws MalformedURLException {
    Http http = new CommonsHttp();
    Response<String> output = http.execute(Request.head(new URL("https://docs.opendatakit.org/")));
    assertThat(output, instanceOf(Response.Success.class));
  }

  @Test
  public void name() {
    System.out.println(DateUtils.formatDate(new Date(), org.apache.http.client.utils.DateUtils.PATTERN_RFC1036));

  }
}