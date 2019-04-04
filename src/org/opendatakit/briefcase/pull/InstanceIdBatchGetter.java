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

package org.opendatakit.briefcase.pull;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;

public class InstanceIdBatchGetter implements Iterator<InstanceIdBatch> {
  private final RemoteServer server;
  private final Http http;
  private final String formId;
  private final boolean includeIncomplete;
  private Optional<String> nextCursor = Optional.empty();
  private List<String> nextUids;

  private InstanceIdBatchGetter(RemoteServer server, Http http, String formId, boolean includeIncomplete) {
    this.server = server;
    this.http = http;
    this.formId = formId;
    this.includeIncomplete = includeIncomplete;
    fetchNext();
  }

  static List<InstanceIdBatch> getInstanceIdBatches(RemoteServer server, Http http, String formId, boolean includeIncomplete) {
    InstanceIdBatchGetter batcher = new InstanceIdBatchGetter(server, http, formId, includeIncomplete);
    Iterable<InstanceIdBatch> iterable = () -> batcher;
    return StreamSupport.stream(iterable.spliterator(), false).collect(toList());
  }

  private void fetchNext() {
    Pair<String, List<String>> batch = http.execute(server.getInstanceIdBatchRequest(
        formId,
        100,
        nextCursor.orElse(""),
        includeIncomplete
    )).map(this::parseBatch).orElseThrow(BriefcaseException::new);
    nextUids = batch.getRight();
    nextCursor = Optional.of(batch.getLeft());
  }

  private Pair<String, List<String>> parseBatch(XmlElement xmlElement) {
    String nextCursor = xmlElement.findElement("resumptionCursor")
        .orElseThrow(BriefcaseException::new)
        .getValue();
    List<String> uids = xmlElement
        .findElement("idList")
        .map(e -> e.findElements("id"))
        .orElse(Collections.emptyList())
        .stream()
        .map(XmlElement::getValue)
        .collect(toList());
    return Pair.of(nextCursor, uids);
  }

  @Override
  public boolean hasNext() {
    return !nextUids.isEmpty();
  }

  @Override
  public InstanceIdBatch next() {
    InstanceIdBatch batch = InstanceIdBatch.from(nextUids, nextCursor.orElseThrow(BriefcaseException::new));
    fetchNext();
    return batch;
  }

}
