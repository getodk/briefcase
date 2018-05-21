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

package org.opendatakit.briefcase.push;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.opendatakit.briefcase.reused.http.FakeHttp;
import org.opendatakit.briefcase.reused.http.Query;

public class RemoteServerTest {
  @Test
  public void knows_if_a_form_is_present_or_not_in_an_authenticated_server() {
    FakeHttp http = new FakeHttp();
    http.stub(
        Query.authenticated(
            "https://some.server.com/formList",
            "administrator",
            "aggregate"
        ),
        "<xforms xmlns=\"http://openrosa.org/xforms/xformsList\">\n" +
            "<xform><formID>some-form</formID>\n" +
            "<name>Some Form</name>\n" +
            "<majorMinorVersion>2012072302</majorMinorVersion>\n" +
            "<version>2012072302</version>\n" +
            "<hash>md5:29a492009d35c1515886ddd307521819</hash>\n" +
            "<downloadUrl>https://some.server.com/formXml?formId=some-form</downloadUrl>\n" +
            "</xform>\n" +
            "</xforms>"
    );
    RemoteServer server = RemoteServer.authenticated("https://some.server.com", "administrator", "aggregate");
    assertThat(http.execute(server.containsFormQuery("some-form")), is(true));
    assertThat(http.execute(server.containsFormQuery("some-other-form")), is(false));
  }

  @Test
  public void knows_if_a_form_is_present_or_not_in_a_non_authenticated_server() {
    FakeHttp http = new FakeHttp();
    http.stub(
        Query.normal("https://some.server.com/formList"),
        "<xforms xmlns=\"http://openrosa.org/xforms/xformsList\">\n" +
            "<xform><formID>some-form</formID>\n" +
            "<name>Some Form</name>\n" +
            "<majorMinorVersion>2012072302</majorMinorVersion>\n" +
            "<version>2012072302</version>\n" +
            "<hash>md5:29a492009d35c1515886ddd307521819</hash>\n" +
            "<downloadUrl>https://some.server.com/formXml?formId=some-form</downloadUrl>\n" +
            "</xform>\n" +
            "</xforms>"
    );
    RemoteServer server = RemoteServer.normal("https://some.server.com");
    assertThat(http.execute(server.containsFormQuery("some-form")), is(true));
    assertThat(http.execute(server.containsFormQuery("some-other-form")), is(false));
  }

}
