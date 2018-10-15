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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.javarosa.core.model.DataType.TEXT;
import static org.javarosa.core.model.instance.TreeReference.DEFAULT_MULTIPLICITY;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.ModelBuilder.field;
import static org.opendatakit.briefcase.export.ModelBuilder.instance;
import static org.opendatakit.briefcase.export.ModelBuilder.selectMultiple;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.instance.TreeElement;
import org.junit.Test;

public class ModelTest {
  @Test
  public void gets_choices_of_a_related_select_control() {
    SelectChoice choice1 = new SelectChoice("some label 1", "some value 1", false);
    SelectChoice choice2 = new SelectChoice("some label 2", "some value 2", false);

    Model model = instance(selectMultiple("select", choice1, choice2))
        .build()
        .getChildByName("select");

    assertThat(model.getChoices(), contains(choice1, choice2));
  }

  @Test
  public void gets_choices_of_a_related_select_control_with_search_appearance() {
    QuestionDef control = new QuestionDef();
    control.setControlType(Model.ControlType.SELECT_MULTI.value);
    // This is the choice we will usually find in a select that uses appearance="search(...)"
    control.addSelectChoice(new SelectChoice("name", "name_key", false));
    control.setAppearanceAttr("search('some_external_instance')");

    Model model = instance(field("select", TEXT, control))
        .build()
        .getChildByName("select");


    assertThat(model.getChoices(), empty());
  }

  @Test
  public void knows_if_it_is_the_meta_audit_field() {
    assertThat(buildModel("data", "some-field").getChildByName("some-field").isMetaAudit(), is(false));
    assertThat(buildModel("data", "audit").getChildByName("audit").isMetaAudit(), is(false));
    assertThat(buildModel("data", "some-parent", "audit").getChildByName("audit").isMetaAudit(), is(false));
    assertThat(buildModel("data", "meta", "audit").getChildByName("audit").isMetaAudit(), is(true));
  }

  @Test
  public void knows_if_it_contains_a_meta_audit_field() {
    assertThat(buildModel("data", "meta", "audit").hasAuditField(), is(true));
    assertThat(buildModel("data", "meta", "instanceID").hasAuditField(), is(false));
    assertThat(buildModel("data", "meta").hasAuditField(), is(false));
    assertThat(buildModel("data", "some-field").hasAuditField(), is(false));
    assertThat(buildModel("data", "some-field", "audit").hasAuditField(), is(false));
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

}