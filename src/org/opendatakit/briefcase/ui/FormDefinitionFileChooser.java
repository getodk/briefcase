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

package org.opendatakit.briefcase.ui;

import java.awt.Container;
import java.io.File;

import javax.swing.JFileChooser;

class FormDefinitionFileChooser extends AbstractFileChooser {

  /**
	 * 
	 */
  private final Container parentWindow;
  /**
	 * 
	 */
  private static final long serialVersionUID = 7687043156045655297L;

  /**
   * 
   * @param f
   * @param parentWindow
   * @return true if file exists and has an xml extension.
   */
  public static final boolean testAndMessageBadFormDefinitionFile(File f, Container parentWindow) {
    if ( !f.exists() ) {
      ODKOptionPane.showErrorDialog(parentWindow,
          "Form definition file does not exist",
          "Invalid Form Definition File");
      return false;
    }
    if ( !f.getName().endsWith(".xml") ) {
      ODKOptionPane.showErrorDialog(parentWindow,
          "Form definition file should be an XML file (ending in .xml).",
          "Invalid Form Definition File");
      return false;
    }
    return true;
  }

  @Override
  public void approveSelection() {
    File f = this.getSelectedFile();
    if (testAndMessageBadFormDefinitionFile(f, parentWindow)) {
      super.approveSelection();
    }
  }

  public FormDefinitionFileChooser(Container parentWindow) {
    super();
    this.parentWindow = parentWindow;
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    setDialogType(JFileChooser.OPEN_DIALOG); // must exist...
    setDialogTitle("Open Form Definition File");
    setApproveButtonText("Open");
  }

  @Override
  public boolean testAndMessageBadFolder(File f, Container parentWindow) {
    return testAndMessageBadFormDefinitionFile(f, parentWindow);
  }
}