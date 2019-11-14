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
package org.opendatakit.briefcase.ui.reused;

import java.util.Comparator;

/**
 * Sort Comparator for columns providing booleans
 */
public class SelectionComparator  implements Comparator<Boolean> {

  public int compare(Boolean o1, Boolean o2) {
    int result;
    if (o1.equals(o2)) result = 0;
    else if (o1.equals(Boolean.TRUE)  && o2.equals(Boolean.FALSE)) result = -1;
    else
      result = 1;
    return result;
  }
}

