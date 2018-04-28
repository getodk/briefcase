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

import static java.nio.file.Files.newInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Optional;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.parse.XFormParser;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.JavaRosaParserWrapper;

public class FormDefinition {
  private final Optional<String> id;
  private final String name;
  private final Path formFile;
  private final boolean isEncrypted;
  private final TreeElement submissionElement;
  private final Optional<Path> revisedFormFile;

  private FormDefinition(Path formFile, Optional<Path> revisedFormFile, Optional<String> id, String name, boolean isEncrypted, TreeElement submissionElement) {
    this.id = id;
    this.name = name;
    this.isEncrypted = isEncrypted;
    this.submissionElement = submissionElement;
    this.formFile = formFile;
    this.revisedFormFile = revisedFormFile;
  }

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
          formFile,
          Optional.of(revised),
          Optional.ofNullable(formDef.getTextID()),
          formDef.getName(),
          isEncrypted,
          formDef.getMainInstance().getRoot()
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static FormDefinition from(BriefcaseFormDefinition fd) {
    return new FormDefinition(
        fd.getFormDefinitionFile().toPath(),
        Optional.ofNullable(fd.revisedFormFile).map(File::toPath),
        Optional.ofNullable(fd.getFormId()),
        fd.getFormName(),
        fd.isFileEncryptedForm(),
        fd.getSubmissionElement()
    );
  }

  Path getFormDir() {
    return formFile.getParent();
  }

  String getFormName() {
    return name;
  }

  boolean isFileEncryptedForm() {
    return isEncrypted;
  }

  TreeElement getSubmissionElement() {
    return submissionElement;
  }

  private static String readFile(Path formDefinitionFile) throws BadFormDefinition {
    try (BufferedReader rdr = new BufferedReader(new InputStreamReader(newInputStream(formDefinitionFile), "UTF-8"))) {
      StringBuilder xmlBuilder = new StringBuilder();
      String line = rdr.readLine();
      while (line != null) {
        xmlBuilder.append(line);
        line = rdr.readLine();
      }
      return xmlBuilder.toString();
    } catch (IOException e) {
      throw new BadFormDefinition("Unable to read form");
    }
  }
}
