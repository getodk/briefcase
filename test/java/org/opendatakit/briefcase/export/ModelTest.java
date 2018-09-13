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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javarosa.core.model.instance.TreeElement;
import org.junit.Test;

public class ModelTest {

  @Test
  public void knows_if_it_is_the_meta_audit_field() {
    assertThat(lastDescendatOf(buildModel("data", "some-field")).isMetaAudit(), is(false));
    assertThat(lastDescendatOf(buildModel("data", "audit")).isMetaAudit(), is(false));
    assertThat(lastDescendatOf(buildModel("data", "some-parent", "audit")).isMetaAudit(), is(false));
    assertThat(lastDescendatOf(buildModel("data", "meta", "audit")).isMetaAudit(), is(true));
  }

  private static Model buildModel(String... names) {
    List<TreeElement> elements = Stream.of(names)
        .map(name -> new TreeElement(name, DEFAULT_MULTIPLICITY))
        .collect(Collectors.toList());

    int maxIndex = elements.size() - 1;
    for (int i = 0; i < maxIndex; i++)
      elements.get(i).addChild(elements.get(i + 1));
    for (int i = maxIndex; i > 0; i--)
      elements.get(i).setParent(elements.get(i - 1));

    return new Model(elements.get(0), emptyMap());
  }

  private static Model lastDescendatOf(Model model) {
    if (!model.hasChildren())
      return model;
    Model child = model.children().get(0);
    while (child.hasChildren())
      child = child.children().get(0);
    return child;
  }

}