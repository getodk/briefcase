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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.PrivateKey;

import org.javarosa.core.model.instance.TreeElement;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.JavaRosaParserWrapper;

public class LocalFormDefinition implements IFormDefinition {
  private final File revisedFormFile;
  private final JavaRosaParserWrapper formDefn;
  private PrivateKey privateKey = null;

  private static final String readFile(File formDefinitionFile) throws BadFormDefinition {
    StringBuilder xmlBuilder = new StringBuilder();
    BufferedReader rdr = null;
    try {
       rdr = new BufferedReader(new FileReader(formDefinitionFile));
       String line = rdr.readLine();
       while (line != null) {
          xmlBuilder.append(line);
          line = rdr.readLine();
       }
    } catch (FileNotFoundException e) {
      throw new BadFormDefinition("Form not found");
    } catch (IOException e) {
      throw new BadFormDefinition("Unable to read form");
    } finally {
       if (rdr != null) {
          try {
             rdr.close();
          } catch (IOException e) {
             e.printStackTrace();
          }
       }
    }
    String inputXml = xmlBuilder.toString();
    return inputXml;
  }
  
  public LocalFormDefinition(File formFile) throws BadFormDefinition {
    if (!formFile.exists()) {
      throw new BadFormDefinition("Form directory does not contain form");
    }
    File revised = new File( formFile.getParentFile(), formFile.getName() + ".revised");
    try {
      if ( revised.exists() ) {
      	revisedFormFile = revised;
        formDefn = new JavaRosaParserWrapper(revisedFormFile, readFile(revisedFormFile));
      } else {
      	revisedFormFile = null;
        formDefn = new JavaRosaParserWrapper(formFile, readFile(formFile));
      }
    } catch (ODKIncompleteSubmissionData e) {
      throw new BadFormDefinition(e, e.getReason());
    }
  }

  @Override
  public String toString() {
	  return getFormName();
  }
  /*
   * (non-Javadoc)
   * 
   * @see org.opendatakit.briefcase.model.IFormDefinition#getFormName()
   */
  @Override
  public String getFormName() {
    return formDefn.getFormName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opendatakit.briefcase.model.IFormDefinition#getFormId()
   */
  @Override
  public String getFormId() {
    return formDefn.getSubmissionElementDefn().formId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opendatakit.briefcase.model.IFormDefinition#getModelVersion()
   */
  @Override
  public Integer getModelVersion() {
    Long l = formDefn.getSubmissionElementDefn().modelVersion;
    if (l == null)
      return null;
    return l.intValue();
  }

  public File getFormDefinitionFile() {
	if ( revisedFormFile != null ) {
		return revisedFormFile;
	} else {
		return formDefn.getFormDefinitionFile();
	}
  }

  public boolean isInvalidFormXmlns() {
	  return formDefn.isInvalidFormXmlns();
  }

  public String getSubmissionKey(String uri) {
    return formDefn.getSubmissionKey(uri);
  }

  public boolean isFieldEncryptedForm() {
	  return formDefn.isFieldEncryptedForm();
  }
  
  public boolean isFileEncryptedForm() {
    return formDefn.isFileEncryptedForm();
  }
  
  public TreeElement getSubmissionElement() {
	  return formDefn.getSubmissionElement();
  }
  
  public void setPrivateKey(PrivateKey privateKey) {
	  this.privateKey = privateKey;
  }
  
  public PrivateKey getPrivateKey() {
	  return privateKey;
  }
  
  @Override
  public LocationType getFormLocation() {
    return LocationType.LOCAL;
  }

  @Override
  public boolean equals(Object obj) {
    if ( obj != null && obj instanceof LocalFormDefinition ) {
      LocalFormDefinition lf = (LocalFormDefinition) obj;
      
      String id = getFormId();
      Integer version = getModelVersion();
      
      return ( id.equals(lf.getFormId()) &&
               ((version == null) ? (lf.getModelVersion() == null) : version.equals(lf.getModelVersion())) );
    }
    
    return false;
  }

  @Override
  public int hashCode() {
    String id = getFormId();
    Integer version = getModelVersion();
    
    return id.hashCode() 
        + 3*(version == null ? -123121 : version.hashCode());
  }
}
