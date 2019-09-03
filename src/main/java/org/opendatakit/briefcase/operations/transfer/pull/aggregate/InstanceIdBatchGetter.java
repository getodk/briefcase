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

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.http.response.Response;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;

/**
 * This class will request batches of submission instanceIDs to Aggregate until
 * all have been retrieved using the <a href="https://docs.opendatakit.org/briefcase-api/#">Briefcase Aggregate API</a>.
 */
public class InstanceIdBatchGetter implements Iterator<InstanceIdBatch> {
  private final AggregateServer server;
  private final Workspace workspace;
  private final String formId;
  private final boolean includeIncomplete;
  private Cursor nextCursor;
  private List<String> nextUids;

  InstanceIdBatchGetter(Workspace workspace, AggregateServer server, String formId, boolean includeIncomplete, Cursor nextCursor) {
    this.server = server;
    this.workspace = workspace;
    this.formId = formId;
    this.includeIncomplete = includeIncomplete;
    this.nextCursor = nextCursor;
    fetchNext();
  }

  private void fetchNext() {
    Response<XmlElement> response = workspace.http.execute(server.getInstanceIdBatchRequest(
        formId,
        100,
        nextCursor,
        includeIncomplete
    ));
    Pair<Cursor, List<String>> batch = response
        .map(this::parseBatch)
        .orElseThrow(() -> new InstanceIdBatchGetterException(response));
    nextUids = batch.getRight();
    nextCursor = batch.getLeft();
  }

  private Pair<Cursor, List<String>> parseBatch(XmlElement xmlElement) {
    Cursor nextCursor = xmlElement.findFirstElement("resumptionCursor")
        .flatMap(XmlElement::maybeValue)
        .map(Cursor::from)
        .orElseGet(Cursor::empty);
    List<String> uids = xmlElement
        .findFirstElement("idList")
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
    InstanceIdBatch batch = InstanceIdBatch.from(nextUids, nextCursor);
    fetchNext();
    return batch;
  }

}
