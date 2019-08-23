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

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.pull.aggregate.CursorHelpers.buildCursorXml;

import org.junit.Test;

public class AggregateCursorTest {

  @Test
  @SuppressWarnings("unchecked")
  public void fixes_dates_while_parsing_cursors() {
    assertThat(
        AggregateCursor.from(buildCursorXml("2010-01-01T00:00:00.000+0800")),
        lessThan(AggregateCursor.from(buildCursorXml("2010-01-01T00:00:00.000+03")))
    );
  }
}
