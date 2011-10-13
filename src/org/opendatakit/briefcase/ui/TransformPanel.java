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

import java.awt.Component;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

public class TransformPanel extends JPanel {

  /**
	 * 
	 */
  private static final long serialVersionUID = 7169316129011796197L;

  private final JLabel lblOutputType = new JLabel("Output Type:");
  private JTextField txtOutputDirectory;

  private JComboBox comboBoxOutputType;

  private JComboBox comboBoxForm;

  private JButton btnChooseOutputDirectory;

  private JButton btnOutput;

  private JTextArea textAreaTransformStatus;

  /**
   * Create the panel.
   */
  public TransformPanel() {
    super();
    
    comboBoxOutputType = new JComboBox();

    JLabel lblForm = new JLabel("Form:");

    comboBoxForm = new JComboBox();

    JLabel lblOutputDirectory = new JLabel("Output Directory:");

    txtOutputDirectory = new JTextField();
    txtOutputDirectory.setColumns(10);

    btnChooseOutputDirectory = new JButton("Choose...");

    btnOutput = new JButton("Output");

    textAreaTransformStatus = new JTextArea();
    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(groupLayout
        .createSequentialGroup()
        .addContainerGap()
        .addGroup(
            groupLayout
                .createParallelGroup(Alignment.LEADING)
                .addComponent(textAreaTransformStatus, GroupLayout.DEFAULT_SIZE, 612,
                    Short.MAX_VALUE)
                .addGroup(
                    groupLayout
                        .createSequentialGroup()
                        .addGroup(
                            groupLayout.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(lblForm).addComponent(lblOutputType)
                                .addComponent(lblOutputDirectory))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                            groupLayout
                                .createParallelGroup(Alignment.LEADING, true)
                                .addComponent(comboBoxForm, Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE)
                                .addComponent(comboBoxOutputType, Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE)
                                .addGroup(
                                    Alignment.TRAILING,
                                    groupLayout
                                        .createSequentialGroup()
                                        .addComponent(txtOutputDirectory,
                                            GroupLayout.PREFERRED_SIZE, 338, Short.MAX_VALUE)
                                        .addPreferredGap(ComponentPlacement.RELATED)
                                        .addComponent(btnChooseOutputDirectory))))
                .addComponent(btnOutput, Alignment.TRAILING)).addContainerGap());
    groupLayout.setVerticalGroup(groupLayout
        .createSequentialGroup()
        .addContainerGap()
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(comboBoxForm)
                .addComponent(lblForm))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(comboBoxOutputType)
                .addComponent(lblOutputType))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(
            groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblOutputDirectory)
                .addComponent(btnChooseOutputDirectory).addComponent(txtOutputDirectory))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(textAreaTransformStatus, GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
        .addPreferredGap(ComponentPlacement.RELATED).addComponent(btnOutput).addContainerGap());
    setLayout(groupLayout);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    Component[] com = this.getComponents();
    for (int a = 0; a < com.length; a++) {
      com[a].setEnabled(enabled);
    }
  }
}
