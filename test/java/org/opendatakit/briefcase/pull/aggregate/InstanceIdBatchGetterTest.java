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

package org.opendatakit.briefcase.pull.aggregate;

import static com.github.dreamhead.moco.HttpMethod.GET;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.log;
import static com.github.dreamhead.moco.Moco.method;
import static com.github.dreamhead.moco.Moco.seq;
import static com.github.dreamhead.moco.Runner.running;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import com.github.dreamhead.moco.HttpServer;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.transfer.RemoteServer;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.http.Http;

public class InstanceIdBatchGetterTest {
  private static final URL BASE_URL = url("http://localhost:12306");
  private static final RemoteServer REMOTE_SERVER = RemoteServer.normal(BASE_URL);
  private HttpServer server;

  private static String escape(String xml) {
    return xml
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\n", "");

  }

  private static List<InstanceIdBatch> getAllBatches(Http http) {
    InstanceIdBatchGetter batcher = new InstanceIdBatchGetter(InstanceIdBatchGetterTest.REMOTE_SERVER, http, "fomdId", true, Cursor.empty());
    Iterable<InstanceIdBatch> iterable = () -> batcher;
    return StreamSupport.stream(iterable.spliterator(), false).collect(toList());
  }

  @Before
  public void setUp() {
    server = httpServer(12306, log());
  }

  @Test
  public void retrieves_batches_until_the_last_empty_one() throws Exception {
    List<String> pages = generatePages(250, 100);
    server.request(by(method(GET)))
        .response(seq(pages.get(0), pages.get(1), pages.get(2), pages.get(3)));

    running(server, () -> {
      List<InstanceIdBatch> idBatches = getAllBatches(CommonsHttp.of(1));
      int total = idBatches.stream().map(InstanceIdBatch::count).reduce(0, Integer::sum);
      assertThat(idBatches, Matchers.hasSize(3));
      assertThat(total, is(250));
    });
  }

  private List<String> generatePages(int totalIds, int idsPerPage) {
    OffsetDateTime startingDateTime = OffsetDateTime.parse("2010-01-01T00:00:00.000Z");
    Cursor lastCursor = Cursor.empty();
    List<String> pages = new ArrayList<>();
    for (int page : IntStream.range(0, (totalIds / idsPerPage) + 1).boxed().collect(Collectors.toList())) {
      int from = page * idsPerPage;
      int to = min(totalIds, (page + 1) * idsPerPage);

      List<String> ids = IntStream.range(from, to).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
      Cursor cursor = Cursor.of(startingDateTime.plusDays(to - 1), ids.get(ids.size() - 1));

      pages.add("" +
          "<idChunk xmlns=\"http://opendatakit.org/submissions\">" +
          "<idList>" + ids.stream().map(id -> "<id>" + id + "</id>").collect(Collectors.joining("")) + "</idList>" +
          "<resumptionCursor>" + escape(cursor.get()) + "</resumptionCursor>" +
          "</idChunk>" +
          "");

      lastCursor = cursor;
    }
    pages.add("" +
        "<idChunk xmlns=\"http://opendatakit.org/submissions\">" +
        "<idList></idList>" +
        "<resumptionCursor>" + escape(lastCursor.get()) + "</resumptionCursor>" +
        "</idChunk>" +
        "");

    return pages;
  }
}
