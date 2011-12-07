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

import java.io.File;
import java.security.PrivateKey;

import org.javarosa.core.model.instance.TreeElement;
import org.opendatakit.briefcase.util.JavaRosaWrapper;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;

public class LocalFormDefinition implements IFormDefinition {
  private final File revisedFormFile;
  private final JavaRosaWrapper formDefn;
  private PrivateKey privateKey = null;

  public LocalFormDefinition(File formFile) throws BadFormDefinition {
    if (!formFile.exists()) {
      throw new BadFormDefinition("Form directory does not contain form");
    }
    File revised = new File( formFile.getParentFile(), formFile.getName() + ".revised");
    if ( revised.exists() ) {
    	revisedFormFile = revised;
    	formDefn = new JavaRosaWrapper(revisedFormFile);
    } else {
    	revisedFormFile = null;
    	formDefn = new JavaRosaWrapper(formFile);
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

  /*
   * (non-Javadoc)
   * 
   * @see org.opendatakit.briefcase.model.IFormDefinition#getUiVersion()
   */
  @Override
  public Integer getUiVersion() {
    Long l = formDefn.getSubmissionElementDefn().uiVersion;
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
  
  public String getMD5Hash() {
    return formDefn.getMD5Hash();
  }

  public String getSubmissionKey(String uri) {
    return formDefn.getSubmissionKey(uri);
  }

  public boolean isEncryptedForm() {
	  return formDefn.isEncryptedForm();
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
}
