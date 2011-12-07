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

import javax.swing.JFileChooser;

public class PrivateKeyFileChooser extends JFileChooser {

  /**
	 * 
	 */
  private static final long serialVersionUID = 7687043156045655207L;

  public PrivateKeyFileChooser(Container parentWindow) {
    super();
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    setDialogType(JFileChooser.OPEN_DIALOG); // must exist...
    setApproveButtonText("Open");
  }
}