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

package org.opendatakit.briefcase.reused.model.transfer;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.model.transfer.AggregateServer.cleanUrl;
import static org.opendatakit.briefcase.reused.model.transfer.AggregateServer.normal;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;

public class AggregateServerTest {
  private AggregateServer server;

  @Before
  public void setUp() throws MalformedURLException {
    server = normal(new URL("https://some.server.com"));
  }

  @Test
  public void knows_how_to_build_instance_id_batch_urls() throws UnsupportedEncodingException {
    assertThat(
        server.getInstanceIdBatchRequest("some-form", 100, Cursor.empty(), true).getUrl().toString(),
        is("https://some.server.com/view/submissionList?formId=some-form&cursor=&numEntries=100&includeIncomplete=true")
    );
    Cursor cursor = Cursor.of(LocalDate.parse("2010-01-01"));
    assertThat(
        server.getInstanceIdBatchRequest("some-form", 100, cursor, false).getUrl().toString(),
        is("https://some.server.com/view/submissionList?formId=some-form&cursor=" + encode(cursor.getValue(), UTF_8.toString()) + "&numEntries=100&includeIncomplete=false")
    );
  }

  @Test
  public void knows_how_to_clean_copied_and_pasted_Aggregate_URLs_from_a_browser() {
    assertThat(
        cleanUrl("https://sandbox.aggregate.opendatakit.org/Aggregate.html#submissions/filter///"),
        is("https://sandbox.aggregate.opendatakit.org")
    );
    assertThat(
        cleanUrl("https://someserver.com/nonRootWebapp/Aggregate.html#submissions/filter///"),
        is("https://someserver.com/nonRootWebapp")
    );
  }
}
