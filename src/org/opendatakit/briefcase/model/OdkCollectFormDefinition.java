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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.JavaRosaParserWrapper;

/**
 * Holds the parsed form definition for a form in an ODK Collect directory
 *
 * @author mitchellsundt@gmail.com
 */
public class OdkCollectFormDefinition implements IFormDefinition {

  private JavaRosaParserWrapper formDefn;

  private static String readFile(File formDefinitionFile) throws BadFormDefinition {
    StringBuilder xmlBuilder = new StringBuilder();
    try (BufferedReader rdr = new BufferedReader(new InputStreamReader(
        new FileInputStream(formDefinitionFile), UTF_8))) {
      String line = rdr.readLine();
      while (line != null) {
        xmlBuilder.append(line);
        line = rdr.readLine();
      }
    } catch (FileNotFoundException e) {
      throw new BadFormDefinition("Form not found");
    } catch (IOException e) {
      throw new BadFormDefinition("Unable to read form");
    }
    return xmlBuilder.toString();
  }

  public OdkCollectFormDefinition(File formFile) throws BadFormDefinition {
    if (!formFile.exists()) {
      throw new BadFormDefinition("Form directory does not contain form");
    }
    try {
      formDefn = new JavaRosaParserWrapper(formFile, readFile(formFile));
    } catch (ODKIncompleteSubmissionData e) {
      throw new BadFormDefinition(e);
    }
  }

  @Override
  public String toString() {
    return getFormName();
  }

  @Override
  public String getFormName() {
    return formDefn.getFormName();
  }

  @Override
  public String getFormId() {
    return formDefn.getSubmissionElementDefn().formId;
  }

  @Override
  public String getVersionString() {
    return formDefn.getSubmissionElementDefn().modelVersion;
  }

  public File getFormDefinitionFile() {
    return formDefn.getFormDefinitionFile();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OdkCollectFormDefinition) {
      OdkCollectFormDefinition lf = (OdkCollectFormDefinition) obj;

      String id = getFormId();
      String versionString = getVersionString();

      return (id.equals(lf.getFormId()) && ((versionString == null)
          ? (lf.getVersionString() == null)
          : versionString.equals(lf.getVersionString())));
    }

    return false;
  }

  @Override
  public int hashCode() {
    String id = getFormId();
    String versionString = getVersionString();

    return id.hashCode() + 3 * (versionString == null ? -123121 : versionString.hashCode());
  }
}
