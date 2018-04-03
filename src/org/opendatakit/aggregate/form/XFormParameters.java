/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.aggregate.form;

import java.io.Serializable;
import java.util.Objects;

/**
 * Helper class holding the details of a
 * specific version of a form.
 *
 * @author mitchellsundt@gmail.com
 * @author wbrunette@gmail.com
 */
public final class XFormParameters implements Comparable<XFormParameters>, Serializable {

  public final String formId;
  public final String versionString;
  public final String modelVersion;

  public XFormParameters(String formId, String versionString) {
    if (formId == null) {
      throw new IllegalArgumentException("formId cannot be null");
    }
    this.formId = formId;
    this.versionString = (versionString == null || versionString.length() == 0) ? null : versionString;
    this.modelVersion = this.versionString;
  }

  @Override
  public String toString() {
    return "XFormParameters{" +
        "formId='" + formId + '\'' +
        ", versionString='" + versionString + '\'' +
        ", modelVersion='" + modelVersion + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    XFormParameters that = (XFormParameters) o;
    return Objects.equals(formId, that.formId) &&
        Objects.equals(versionString, that.versionString) &&
        Objects.equals(modelVersion, that.modelVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formId, versionString, modelVersion);
  }

  @Override
  public int compareTo(XFormParameters p) {
    int cmp = formId.compareTo(p.formId);
    if (cmp != 0)
      return cmp;
    if (((modelVersion == null) ? (p.modelVersion == null) :
        (p.modelVersion != null && modelVersion.equals(p.modelVersion)))) {
      // uiVersion is ignored during comparisons and equality tests
      return 0;
    } else if (modelVersion == null) {
      return 1;
    } else if (p.modelVersion == null) {
      return -1;
    } else {
      return modelVersion.compareTo(p.modelVersion);
    }
  }
}