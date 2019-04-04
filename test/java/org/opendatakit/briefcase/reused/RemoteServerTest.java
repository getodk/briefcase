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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.noContent;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.http.FakeHttp;

public class RemoteServerTest {
  private FakeHttp http;
  private RemoteServer server;

  @Before
  public void setUp() throws MalformedURLException {
    http = new FakeHttp();
    server = RemoteServer.normal(new URL("https://some.server.com"));
  }

  @Test
  public void knows_if_it_contains_a_form() {
    http.stub(server.getFormListRequest(), ok("" +
        "<forms>\n" +
        "<form url=\"https://some.server.com/formXml?formId=some-form\">Some form</form>\n" +
        "</forms>\n"));
    assertThat(server.containsForm(http, "some-form"), is(true));
    assertThat(server.containsForm(http, "some-other-form"), is(false));
  }

  @Test
  public void knows_how_to_test_connection_params_for_pulling_forms() {
    http.stub(server.getFormListRequest(), ok("" +
        "<forms>\n" +
        "<form url=\"https://some.server.com/formXml?formId=some-form\">Some form</form>\n" +
        "</forms>\n"));
    assertThat(server.testPull(http).isSuccess(), is(true));
  }

  @Test
  public void knows_how_to_test_connection_params_for_pushing_forms() {
    http.stub(server.getPushFormPreflightRequest(), noContent());
    assertThat(server.testPush(http).isSuccess(), is(true));
  }

  @Test
  public void knows_how_to_build_instance_id_batch_urls() {
    assertThat(
        server.getInstanceIdBatchRequest("some-form", 100, "", true).getUrl().toString(),
        is("https://some.server.com/view/submissionList?formId=some-form&cursor=&numEntries=100&includeIncomplete=true")
    );
    assertThat(
        server.getInstanceIdBatchRequest("some-form", 100, "some-cursor", false).getUrl().toString(),
        is("https://some.server.com/view/submissionList?formId=some-form&cursor=some-cursor&numEntries=100&includeIncomplete=false")
    );
  }
}
