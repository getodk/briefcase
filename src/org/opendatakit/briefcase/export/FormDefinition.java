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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.parse.XFormParser;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class holds all the relevant information about the form being exported.
 */
public class FormDefinition {
  private final String id;
  private final String name;
  private final Path formFile;
  private final boolean isEncrypted;
  private final Model model;

  private FormDefinition(String id, Path formFile, String name, boolean isEncrypted, Model model) {
    this.id = id;
    this.name = name;
    this.formFile = formFile;
    this.isEncrypted = isEncrypted;
    this.model = model;
  }

  /**
   * Factory that takes the {@link Path} to a form's definition XML file, parses it, and
   * returns a new {@link FormDefinition}.
   *
   * @throws BriefcaseException if the given {@link Path} doesn't exist
   * @throws ParsingException   if there is any problem while parsing the file
   */
  static FormDefinition from(Path formFile) {
    if (!Files.exists(formFile))
      throw new BriefcaseException("No form file found");

    Path revised = formFile.getParent().resolve(formFile.getFileName() + ".revised");

    try (InputStream in = Files.newInputStream(Files.exists(revised) ? revised : formFile);
         InputStreamReader isr = new InputStreamReader(in, "UTF-8");
         BufferedReader br = new BufferedReader(isr)) {
      FormDef formDef = new XFormParser(XFormParser.getXMLDocument(br)).parse();
      boolean isEncrypted = Optional.ofNullable(formDef.getSubmissionProfile())
          .flatMap(sp -> Optional.ofNullable(sp.getAttribute("base64RsaPublicKey")))
          .filter(s -> !s.isEmpty())
          .isPresent();
      return new FormDefinition(
          parseFormId(formDef.getMainInstance().getRoot()),
          formFile,
          formDef.getName(),
          isEncrypted,
          new Model(formDef.getMainInstance().getRoot())
      );
    } catch (IOException e) {
      throw new ParsingException(e);
    }
  }

  /**
   * Factory that reads all necessary info from a {@link BriefcaseFormDefinition} to create
   * a new {@link FormDefinition} instance.
   */
  public static FormDefinition from(BriefcaseFormDefinition fd) {
    return new FormDefinition(
        fd.getFormId(),
        fd.getFormDefinitionFile().toPath(),
        fd.getFormName(),
        fd.isFileEncryptedForm(),
        new Model(fd.getSubmissionElement())
    );
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
  boolean isFileEncryptedForm() {
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
}
