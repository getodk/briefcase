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

package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static com.github.dreamhead.moco.HttpMethod.GET;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.log;
import static com.github.dreamhead.moco.Moco.method;
import static com.github.dreamhead.moco.Moco.seq;
import static com.github.dreamhead.moco.Runner.running;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.generatePages;

import com.github.dreamhead.moco.HttpServer;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.ContainerHelper;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.http.CommonsHttp;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;

public class InstanceIdBatchGetterTest {
  private static final URL BASE_URL = url("http://localhost:12306");
  private static final AggregateServer REMOTE_SERVER = AggregateServer.normal(BASE_URL);
  private HttpServer server;
  private Container container;

  @Before
  public void setUp() {
    server = httpServer(12306, log());
    container = ContainerHelper.inMemory(CommonsHttp.of(1, Optional.empty()));
  }

  @Test
  public void retrieves_batches_until_the_last_empty_one() throws Exception {
    List<Pair<String, Cursor>> pages = generatePages(250, 100);
    server.request(by(method(GET)))
        .response(seq(
            pages.get(0).getLeft(),
            pages.get(1).getLeft(),
            pages.get(2).getLeft(),
            pages.get(3).getLeft()
        ));

    running(server, () -> {
      List<InstanceIdBatch> idBatches = getAllBatches();
      int total = idBatches.stream().map(InstanceIdBatch::count).reduce(0, Integer::sum);
      assertThat(idBatches, hasSize(3));
      assertThat(total, is(250));
    });
  }


  private List<InstanceIdBatch> getAllBatches() {
    InstanceIdBatchGetter batcher = new InstanceIdBatchGetter(container.http, InstanceIdBatchGetterTest.REMOTE_SERVER, "fomdId", true, Cursor.empty());
    Iterable<InstanceIdBatch> iterable = () -> batcher;
    return StreamSupport.stream(iterable.spliterator(), false).collect(toList());
  }
}
