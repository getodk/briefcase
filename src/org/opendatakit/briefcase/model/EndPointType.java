/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.model;

public enum EndPointType {

  AGGREGATE_0_9_X_CHOICE, AGGREGATE_1_0_CHOICE, MOUNTED_ODK_COLLECT_DEVICE_CHOICE, CUSTOM_ODK_COLLECT_DIRECTORY;

  public String toString() {
    switch (this) {
    case AGGREGATE_0_9_X_CHOICE:
      return "Aggregate 0.9.x";
    case AGGREGATE_1_0_CHOICE:
      return "Aggregate 1.0";
    case MOUNTED_ODK_COLLECT_DEVICE_CHOICE:
      return "Mounted Android SD Card";
    case CUSTOM_ODK_COLLECT_DIRECTORY:
      return "Custom Path to ODK Directory";
    }
    throw new IllegalStateException("Unhandled EndPointType value");
  }

  public static EndPointType fromString(String toStringValue) {
    EndPointType[] types = EndPointType.values();
    for (EndPointType t : types) {
      if (t.toString().equals(toStringValue)) {
        return t;
      }
    }
    return null;
  }
}
