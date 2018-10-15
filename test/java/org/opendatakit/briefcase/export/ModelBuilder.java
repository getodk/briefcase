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

import static org.javarosa.core.model.instance.TreeReference.DEFAULT_MULTIPLICITY;

import java.util.HashMap;
import java.util.Map;
import org.javarosa.core.model.DataType;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.instance.TreeElement;

class ModelBuilder {
  private TreeElement current = new TreeElement(null, DEFAULT_MULTIPLICITY);
  private Map<String, QuestionDef> controls = new HashMap<>();

  ModelBuilder addGroup(String name) {
    TreeElement child = new TreeElement(name, DEFAULT_MULTIPLICITY);
    child.setDataType(DataType.NULL.value);
    child.setRepeatable(false);
    child.setParent(current);
    current.addChild(child);
    current = child;
    return this;
  }

  ModelBuilder addRepeatGroup(String name) {
    TreeElement child = new TreeElement(name, DEFAULT_MULTIPLICITY);
    child.setDataType(DataType.NULL.value);
    child.setRepeatable(true);
    child.setParent(current);
    current.addChild(child);
    current = child;
    return this;
  }

  ModelBuilder addField(String name, DataType dataType) {
    return addField(name, dataType, null);
  }

  ModelBuilder addField(String name, DataType dataType, QuestionDef control) {
    TreeElement child = new TreeElement(name, DEFAULT_MULTIPLICITY);
    child.setDataType(dataType.value);
    child.setParent(current);
    current.addChild(child);
    current = child;
    if (control != null)
      controls.put(Model.fqn(current, 0), control);
    return this;
  }

  Model build() {
    return new Model(current, controls);
  }
}
