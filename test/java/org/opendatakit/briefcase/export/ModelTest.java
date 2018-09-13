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

package org.opendatakit.briefcase.export;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.javarosa.core.model.instance.TreeReference.DEFAULT_MULTIPLICITY;
import static org.junit.Assert.assertThat;

import org.javarosa.core.model.instance.TreeElement;
import org.junit.Test;

public class ModelTest {

  @Test
  public void knows_if_it_is_the_meta_audit_field() {
    assertThat(buildModel("some-field").isMetaAudit(), is(false));
    assertThat(buildModel("audit").isMetaAudit(), is(false));
    assertThat(buildModel("audit", "some-parent").isMetaAudit(), is(false));
    assertThat(buildModel("audit", "meta").isMetaAudit(), is(true));
  }

  private Model buildModel(String name) {
    return buildModel(name, null);
  }

  private Model buildModel(String name, String parentName) {
    TreeElement element = new TreeElement(name, DEFAULT_MULTIPLICITY);
    if (parentName != null) {
      TreeElement parent = new TreeElement(parentName, DEFAULT_MULTIPLICITY);
      element.setParent(parent);
    }
    return new Model(element, emptyMap());
  }
}