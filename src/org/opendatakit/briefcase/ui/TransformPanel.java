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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.OutputType;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransformFailedEvent;
import org.opendatakit.briefcase.model.TransformProgressEvent;
import org.opendatakit.briefcase.model.TransformSucceededEvent;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransformAction;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;

public class TransformPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7169316129011796197L;

	private JTextField txtOutputDirectory;

	private JComboBox comboBoxOutputType;

	private JComboBox comboBoxForm;

	private JButton btnChooseOutputDirectory;

	private JButton btnOutput;

	private TerminationFuture terminationFuture;
	private JScrollPane scrollPane;

	private JEditorPane transformStatusList;

	class FolderActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			// briefcase...
			NonBriefcaseFolderChooser fc = new NonBriefcaseFolderChooser(
					TransformPanel.this, true);
			int retVal = fc.showDialog(TransformPanel.this, null);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				if (fc.getSelectedFile() != null) {
					String nonBriefcasePath = fc.getSelectedFile()
							.getAbsolutePath();
					txtOutputDirectory.setText(nonBriefcasePath);
					btnOutput.setEnabled(true);
					return;
				}
			}
			// otherwise Output button is disabled.
			btnOutput.setEnabled(false);
		}
	}

	/**
	 * Handle click-action for the "Transfer" button. Extracts the settings from
	 * the UI and invokes the relevant TransferAction to actually do the work.
	 */
	class TransformActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			if (!NonBriefcaseFolderChooser
					.isValidNonBriefcaseFolder(txtOutputDirectory.getText())) {
				return;
			}
			if (comboBoxOutputType.getSelectedIndex() == -1
					|| comboBoxForm.getSelectedIndex() == -1) {
				return;
			}

			File outputDir = new File(txtOutputDirectory.getText());
			OutputType outputType = (OutputType) comboBoxOutputType
					.getSelectedItem();
			LocalFormDefinition lfd = (LocalFormDefinition) comboBoxForm
					.getSelectedItem();

			// OK -- launch background task to do the transformation

			try {
				btnOutput.setEnabled(false);
				transformStatusList.setText("Starting Transformation...");
				terminationFuture.reset();
				TransformAction.transform(outputDir, outputType, lfd,
						terminationFuture);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(TransformPanel.this,
						"Briefcase action failed: " + ex.getMessage(),
						"Briefcase Action Failed", JOptionPane.ERROR_MESSAGE);
			} finally {
				btnOutput.setEnabled(true);
			}
		}
	}

	/**
	 * Create the panel.
	 */
	public TransformPanel(TerminationFuture terminationFuture) {
		super();
		AnnotationProcessor.process(this);// if not using AOP
		this.terminationFuture = terminationFuture;

		JLabel lblForm = new JLabel("Form:");
		comboBoxForm = new JComboBox();

		List<LocalFormDefinition> forms = FileSystemUtils
				.getBriefcaseFormList(BriefcasePreferences
						.getBriefcaseDirectoryProperty());
		DefaultComboBoxModel formChoices = new DefaultComboBoxModel(
				forms.toArray());
		comboBoxForm.setModel(formChoices);

		JLabel lblOutputType = new JLabel("Output Type:");
		comboBoxOutputType = new JComboBox(OutputType.values());

		JLabel lblOutputDirectory = new JLabel("Output Directory:");

		txtOutputDirectory = new JTextField();
		txtOutputDirectory.setFocusable(false);
		txtOutputDirectory.setEditable(false);
		txtOutputDirectory.setColumns(10);

		btnChooseOutputDirectory = new JButton("Choose...");
		btnChooseOutputDirectory.addActionListener(new FolderActionListener());

		btnOutput = new JButton("Output");
		btnOutput.addActionListener(new TransformActionListener());
		btnOutput.setEnabled(false);

		scrollPane = new JScrollPane();

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout
				.setHorizontalGroup(groupLayout
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								Alignment.TRAILING,
								groupLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.TRAILING)
														.addComponent(
																scrollPane,
																Alignment.LEADING,
																GroupLayout.DEFAULT_SIZE,
																650,
																Short.MAX_VALUE)
														.addGroup(
																groupLayout
																		.createSequentialGroup()
																		.addGroup(
																				groupLayout
																						.createParallelGroup(
																								Alignment.LEADING,
																								false)
																						.addComponent(
																								lblForm)
																						.addComponent(
																								lblOutputType)
																						.addComponent(
																								lblOutputDirectory))
																		.addPreferredGap(
																				ComponentPlacement.RELATED)
																		.addGroup(
																				groupLayout
																						.createParallelGroup(
																								Alignment.TRAILING)
																						.addComponent(
																								btnOutput)
																						.addComponent(
																								comboBoxForm,
																								0,
																								520,
																								Short.MAX_VALUE)
																						.addComponent(
																								comboBoxOutputType,
																								0,
																								520,
																								Short.MAX_VALUE)
																						.addGroup(
																								groupLayout
																										.createSequentialGroup()
																										.addComponent(
																												txtOutputDirectory,
																												GroupLayout.PREFERRED_SIZE,
																												412,
																												Short.MAX_VALUE)
																										.addPreferredGap(
																												ComponentPlacement.RELATED)
																										.addComponent(
																												btnChooseOutputDirectory)))))
										.addContainerGap()));
		
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
				groupLayout.createSequentialGroup()
							.addContainerGap()
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
												.addComponent(
														comboBoxForm,
														GroupLayout.PREFERRED_SIZE,
														GroupLayout.DEFAULT_SIZE,
														GroupLayout.PREFERRED_SIZE)
												.addComponent(lblForm))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
												.addComponent(
														comboBoxOutputType,
														GroupLayout.PREFERRED_SIZE,
														GroupLayout.DEFAULT_SIZE,
														GroupLayout.PREFERRED_SIZE)
												.addComponent(
														lblOutputType))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
												.addComponent(
														lblOutputDirectory)
												.addComponent(
														btnChooseOutputDirectory)
												.addComponent(
														txtOutputDirectory,
														GroupLayout.PREFERRED_SIZE,
														GroupLayout.DEFAULT_SIZE,
														GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(scrollPane,
												GroupLayout.PREFERRED_SIZE,
												131, Short.MAX_VALUE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnOutput)
							.addContainerGap()));

		transformStatusList = new JEditorPane("text/plain", "");
		transformStatusList.setEditable(false);
		scrollPane.setViewportView(transformStatusList);
		setLayout(groupLayout);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		// update the list of forms...
		List<LocalFormDefinition> forms = FileSystemUtils
				.getBriefcaseFormList(BriefcasePreferences
						.getBriefcaseDirectoryProperty());
		DefaultComboBoxModel formChoices = new DefaultComboBoxModel(
				forms.toArray());
		comboBoxForm.setModel(formChoices);

		// enable/disable the components...
		Component[] com = this.getComponents();
		for (int a = 0; a < com.length; a++) {
			com[a].setEnabled(enabled);
		}

		// touch-up with real state...
		if (!NonBriefcaseFolderChooser
				.isValidNonBriefcaseFolder(txtOutputDirectory.getText())) {
			btnOutput.setEnabled(false);
		}
	}

	@EventSubscriber(eventClass = TransformProgressEvent.class)
	public void progress(TransformProgressEvent event) {
		String text = transformStatusList.getText();
		transformStatusList.setText(text + "\n" + event.getText());
	}

	@EventSubscriber(eventClass = TransformFailedEvent.class)
	public void failedCompletion(TransformFailedEvent event) {
		String text = transformStatusList.getText();
		transformStatusList.setText(text + "\n" + "FAILED!");
	}

	@EventSubscriber(eventClass = TransformSucceededEvent.class)
	public void successfulCompletion(TransformSucceededEvent event) {
		String text = transformStatusList.getText();
		transformStatusList.setText(text + "\n" + "SUCCEEDED!");
	}
}
