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

import java.time.OffsetDateTime;
import java.util.List;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class InstanceIdBatch {
  private final List<String> instanceIds;
  private final Cursor cursor;

  private InstanceIdBatch(List<String> instanceIds, Cursor cursor) {
    this.instanceIds = instanceIds;
    this.cursor = cursor;
  }

  public static InstanceIdBatch from(List<String> instanceIds, String cursor) {
    OffsetDateTime lastUpdate = XmlElement.from(cursor)
        .findElement("attributeValue")
        .flatMap(XmlElement::maybeValue)
        // Incoming values like 2018-12-10T09:36:25.474+0000 are not ISO8601 compliant
        .map(value -> String.format("%s:%s", value.substring(0, 26), value.substring(26)))
        .map(OffsetDateTime::parse)
        .orElseThrow(BriefcaseException::new);

    return new InstanceIdBatch(
        instanceIds,
        new Cursor(cursor, lastUpdate)
    );
  }

  Cursor getCursor() {
    return cursor;
  }

  List<String> getInstanceIds() {
    return instanceIds;
  }

  int count() {
    return instanceIds.size();
  }

}
