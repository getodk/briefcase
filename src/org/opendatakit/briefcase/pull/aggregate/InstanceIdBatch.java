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

import java.util.List;

public class InstanceIdBatch {
  private final List<String> instanceIds;
  private final Cursor cursor;

  private InstanceIdBatch(List<String> instanceIds, Cursor cursor) {
    this.instanceIds = instanceIds;
    this.cursor = cursor;
  }

  public static InstanceIdBatch from(List<String> instanceIds, Cursor cursor) {
    return new InstanceIdBatch(instanceIds, cursor);
  }

  Cursor getCursor() {
    return cursor;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  int count() {
    return instanceIds.size();
  }

}
