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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.IDataReference;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xform.parse.XFormParser;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class holds all the relevant information about the form being exported.
 */
public class FormDefinition {
  private static final IFunctionHandler DUMMY_PULLDATA_HANDLER = new IFunctionHandler() {
    @Override
    public String getName() {
      return "pulldata";
    }

    @Override
    public List<Class[]> getPrototypes() {
      return Collections.emptyList();
    }

    @Override
    public boolean rawArgs() {
      return true;
    }

    @Override
    public boolean realTime() {
      return false;
    }

    @Override
    public Object eval(Object[] args, EvaluationContext ec) {
      return "";
    }
  };
  private final String id;
  private final String name;
  private final Path formFile;
  private final boolean isEncrypted;
  private final Model model;
  private final List<Model> repeatFields;

  public FormDefinition(String id, Path formFile, String name, boolean isEncrypted, Model model, List<Model> repeatableFields) {
    this.id = id;
    this.name = name;
    this.formFile = formFile;
    this.isEncrypted = isEncrypted;
    this.model = model;
    this.repeatFields = repeatableFields;
  }

  /**
   * Factory that takes the {@link Path} to a form's definition XML file, parses it, and
   * returns a new {@link FormDefinition}.
   *
   * @throws BriefcaseException if the given {@link Path} doesn't exist
   * @throws ParsingException   if there is any problem while parsing the file
   */
  public static FormDefinition from(Path formFile) {
    if (!Files.exists(formFile))
      throw new BriefcaseException("No form file found");

    Path revised = formFile.getParent().resolve(formFile.getFileName() + ".revised");

    try (InputStream in = Files.newInputStream(Files.exists(revised) ? revised : formFile);
         InputStreamReader isr = new InputStreamReader(in, UTF_8);
         BufferedReader br = new BufferedReader(isr)) {
      FormDef formDef = new XFormParser(XFormParser.getXMLDocument(br)).parse();
      boolean isEncrypted = Optional.ofNullable(formDef.getSubmissionProfile())
          .flatMap(sp -> Optional.ofNullable(sp.getAttribute("base64RsaPublicKey")))
          .filter(s -> !s.isEmpty())
          .isPresent();
      final Model model1 = new Model(formDef.getMainInstance().getRoot(), getFormControls(formDef));
      return new FormDefinition(
          parseFormId(formDef.getMainInstance().getRoot()),
          formFile,
          formDef.getName(),
          isEncrypted,
          model1, model1.getRepeatableFields()
      );
    } catch (IOException e) {
      throw new ParsingException(e);
    }
  }

  private static Map<String, QuestionDef> getFormControls(FormDef formDef) {
    formDef.getEvaluationContext().addFunctionHandler(DUMMY_PULLDATA_HANDLER);
    formDef.initialize(false, new InstanceInitializationFactory());
    return formDef.getChildren()
        .stream()
        .flatMap(FormDefinition::flatten)
        .filter(e -> e instanceof QuestionDef)
        .map(e -> (QuestionDef) e)
        .peek(control -> {
          // Select controls with an itemset pointing to an internal secondary
          // instance *and* using a predicate are effectively dynamic. When this
          // happens, we need to populate the choices when this happens to support
          // the split select multiples feature.
          ItemsetBinding itemsetBinding = control.getDynamicChoices();
          if (itemsetBinding != null) {
            String instanceName = itemsetBinding.nodesetRef.getInstanceName();
            DataInstance secondaryInstance = formDef.getNonMainInstance(instanceName);
            // Populate choices of any control using a secondary
            // instance that is not external
            if (secondaryInstance != null && !(secondaryInstance instanceof ExternalDataInstance))
              try {
                formDef.populateDynamicChoices(itemsetBinding, (TreeReference) control.getBind().getReference());
              } catch (NullPointerException e) {
                // Ignore (see https://github.com/opendatakit/briefcase/issues/789)
              }
          }
        })
        .collect(toMap(FormDefinition::controlFqn, e -> e));
  }

  private static String controlFqn(QuestionDef e) {
    IDataReference bind = e.getBind();
    TreeReference reference = (TreeReference) bind.getReference();

    List<String> names = new ArrayList<>();
    for (int i = 0; i < reference.size(); i++) {
      names.add(reference.getName(i));
    }
    return names
        .subList(1, names.size())
        .stream()
        .collect(joining("-"));
  }

  private static Stream<IFormElement> flatten(IFormElement e) {
    Stream<IFormElement> e1 = Stream.of(e);
    List<IFormElement> children = childrenOf(e);
    Stream<IFormElement> b = children.stream()
        .flatMap(e2 -> childrenOf(e2).size() == 0 ? Stream.of(e2) : flatten(e2));
    return Stream.concat(e1, b);
  }

  private static List<IFormElement> childrenOf(IFormElement e) {
    return Optional.ofNullable(e.getChildren()).orElse(Collections.emptyList());
  }

  private static String parseFormId(TreeElement root) {
    for (int attrIndex = 0; attrIndex < root.getAttributeCount(); attrIndex++) {
      String name = root.getAttributeName(attrIndex);
      if (name.equals("id"))
        return root.getAttributeValue(attrIndex);
    }
    throw new BriefcaseException("No form ID found");
  }

  /**
   * Returns the {@link Path} to the directory where the form's definition XMl
   * file is located.
   */
  Path getFormDir() {
    return formFile.getParent();
  }

  /**
   * Returns the form's name
   */
  String getFormName() {
    return name;
  }

  /**
   * Returns true if the form is encrypted, false otherwise.
   */
  public boolean isFileEncryptedForm() {
    return isEncrypted;
  }

  /**
   * Returns the form's model
   */
  Model getModel() {
    return model;
  }

  /**
   * Returns the form's ID
   */
  public String getFormId() {
    return id;
  }

  /**
   * Returns the list of repeat group fields
   */
  List<Model> getRepeatableFields() {
    return repeatFields;
  }

  /**
   * Returns true if the form definition includes repeat groups, false otherwise.
   */
  boolean hasRepeatableFields() {
    return !repeatFields.isEmpty();
  }
}
