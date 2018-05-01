/*
 * Copyright (C) 2012 University of Washington.
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

/*
 * Copyright (C) 2010 University of Washington.
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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.opendatakit.briefcase.util.FindDirectoryStructure;

public class MountedSDCardChooserDialog extends JDialog implements ActionListener {

  private static final long serialVersionUID = 6753077036860161654L;

  // GUI components
  ButtonGroup _driveButtons;

  private boolean outcome = false;
  private File odkDevice;

  private ButtonGroup radioGroup = new ButtonGroup();
  private JPanel radioPanel;
  private JButton btnRefresh;
  private JButton btnOK;
  private JButton btnCancel;

  public MountedSDCardChooserDialog(Window parentWindow, String candidatePath) {
    super(parentWindow, "Mounted SD Card", ModalityType.DOCUMENT_MODAL);

    if (candidatePath != null && candidatePath.trim().length() != 0) {
      odkDevice = new File(candidatePath);
    } else {
      odkDevice = null;
    }

    JLabel lblChooseLocation = new JLabel("Select the mounted SD card:");
    radioPanel = new JPanel();

    btnOK = new JButton("OK");
    btnOK.addActionListener(__ -> {
      outcome = true;
      setVisible(false);
    });

    btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(__ -> {
      outcome = false;
      setVisible(false);
    });

    btnRefresh = new JButton("Refresh List");
    btnRefresh.addActionListener(__ -> {
      try {
        btnOK.setEnabled(false);
        btnCancel.setEnabled(false);
        rebuildPanel();
      } finally {
        btnOK.setEnabled(true);
        btnCancel.setEnabled(true);
      }
    });

    GroupLayout groupLayout = new GroupLayout(getContentPane());
    groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(lblChooseLocation)
            .addComponent(radioPanel)
            .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                .addComponent(btnRefresh)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(btnOK)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(btnCancel)))
        .addContainerGap()
    );
    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addComponent(lblChooseLocation)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(radioPanel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            .addComponent(btnRefresh)
            .addComponent(btnOK)
            .addComponent(btnCancel))
        .addContainerGap());
    getContentPane().setLayout(groupLayout);
    rebuildPanel();
  }

  private void rebuildPanel() {
    GroupLayout groupLayout = new GroupLayout(radioPanel);

    // Get mounted drives that have the needed directory structures...
    List<File> mounts = FindDirectoryStructure.searchMountedDrives();

    // remove everything from the panel
    radioPanel.removeAll();
    while (radioGroup.getButtonCount() != 0) {
      AbstractButton rb = radioGroup.getElements().nextElement();
      radioGroup.remove(rb);
    }

    // construct the panel
    ParallelGroup leftAlignedHorizontals = groupLayout.createParallelGroup(Alignment.LEADING);
    SequentialGroup verticalGroup = groupLayout.createSequentialGroup();

    int len = 0;
    File oldDevice = odkDevice;
    JRadioButton activeButton = null;
    odkDevice = null;
    boolean first = true;
    for (File m : mounts) {
      String mountName = m.getAbsolutePath();
      JRadioButton mountButton = new JRadioButton(mountName, first);
      mountButton.setActionCommand(mountName);
      radioGroup.add(mountButton);
      mountButton.addActionListener(this);
      if (first) {
        verticalGroup.addContainerGap();
        odkDevice = new File(mountName);
        activeButton = mountButton;
      } else {
        verticalGroup.addPreferredGap(ComponentPlacement.RELATED);
      }
      // try to preserve original mount point selection
      if (oldDevice != null) {
        if (oldDevice.getAbsolutePath().startsWith(mountName)) {
          if (len < mountName.length()) {
            len = mountName.length();
            odkDevice = new File(mountName);
            activeButton = mountButton;
          }
        }
      }
      leftAlignedHorizontals.addComponent(mountButton);
      verticalGroup.addComponent(mountButton);
      first = false;
    }
    if (first != true) {
      verticalGroup.addContainerGap();
    }
    // TODO: set the chosen device as selected.
    if (activeButton != null) {
      activeButton.setSelected(true);
    }
    groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(leftAlignedHorizontals)
        .addContainerGap()
    );
    groupLayout.setVerticalGroup(verticalGroup);
    radioPanel.setLayout(groupLayout);
    pack();
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    odkDevice = new File(event.getActionCommand());
  }

  public int showDialog() {
    this.setVisible(true);
    if (outcome) {
      return JFileChooser.APPROVE_OPTION;
    } else {
      return JFileChooser.CANCEL_OPTION;
    }
  }

  public File getSelectedFile() {
    return odkDevice;
  }
}
