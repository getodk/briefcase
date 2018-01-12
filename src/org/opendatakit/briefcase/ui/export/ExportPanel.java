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

package org.opendatakit.briefcase.ui.export;

import static java.lang.Short.MAX_VALUE;
import static java.time.ZoneId.systemDefault;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.opendatakit.briefcase.model.ExportType.CSV;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
import org.opendatakit.briefcase.ui.export.components.FormsTable;
import org.opendatakit.briefcase.ui.reused.MouseListenerBuilder;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;

public class ExportPanel extends JPanel {

  private static final long serialVersionUID = 7169316129011796197L;

  public static final String TAB_NAME = "Export";

  private final JButton btnSelectAll;
  private final JButton btnClearAll;

  private final JButton btnExport;

  private boolean exportStateActive = false;

  private final TerminationFuture terminationFuture;
  private final ConfigurationPanel confPanel;
  private final ExportForms forms;
  private final FormsTable formsTable;

  public ExportPanel(TerminationFuture terminationFuture) {
    super();
    AnnotationProcessor.process(this);// if not using AOP

    this.terminationFuture = terminationFuture;

    confPanel = ConfigurationPanel.from(ExportConfiguration.empty());
    confPanel.onChange(this::updateExportButton);

    forms = new ExportForms();
    formsTable = FormsTable.from(forms);

    formsTable.onChange(this::updateExportButton);
    formsTable.onChange(this::updateSelectAllButton);
    formsTable.onChange(this::updateClearAllButton);

    btnSelectAll = new JButton("Select all");
    btnSelectAll.addMouseListener(new MouseListenerBuilder().onClick(__ -> forms.selectAll()).build());

    btnClearAll = new JButton("Clear all");
    btnClearAll.setVisible(false);
    btnClearAll.addMouseListener(new MouseListenerBuilder().onClick(__ -> forms.clearAll()).build());

    JLabel lblFormsToTransfer = new JLabel("Forms to export:");

    JScrollPane scrollPane = new JScrollPane(formsTable.getView());
    JSeparator separatorFormsList = new JSeparator();

    btnExport = new JButton("Export");
    btnExport.setEnabled(false);
    btnExport.addActionListener(__ -> {
      List<String> configurationErrors = getErrors();
      showErrors(
          configurationErrors,
          "We can't export the forms until these errors are corrected:",
          "Export configuration error"
      );
      if (configurationErrors.isEmpty()) {
        new Thread(() -> {
          btnExport.setEnabled(false);
          List<String> errors = export();
          showErrors(
              errors,
              "We have found some errors while performing the requested export actions:",
              "Export error report"
          );
          btnExport.setEnabled(true);
        }).start();
      }
    });

    GroupLayout groupLayout = new GroupLayout(this);

    GroupLayout.SequentialGroup leftActions = groupLayout.createSequentialGroup()
        .addComponent(btnSelectAll)
        .addComponent(btnClearAll);

    GroupLayout.SequentialGroup rightActions = groupLayout.createSequentialGroup()
        .addComponent(btnExport);

    GroupLayout.SequentialGroup horizontalGroup = groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(LEADING)
            .addComponent(confPanel.getForm().container)
            .addComponent(separatorFormsList, DEFAULT_SIZE, PREFERRED_SIZE, MAX_VALUE)
            .addComponent(lblFormsToTransfer)
            .addComponent(scrollPane, PREFERRED_SIZE, PREFERRED_SIZE, MAX_VALUE)
            .addGroup(LEADING, leftActions)
            .addGroup(TRAILING, rightActions)
        )
        .addContainerGap();


    GroupLayout.ParallelGroup verticalGroup = groupLayout
        .createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(confPanel.getForm().container)
            .addPreferredGap(ComponentPlacement.UNRELATED, 10, MAX_VALUE)
            .addComponent(separatorFormsList, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(lblFormsToTransfer)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(scrollPane, 200, PREFERRED_SIZE, MAX_VALUE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(groupLayout.createParallelGroup(TRAILING)
                .addComponent(btnSelectAll)
                .addComponent(btnClearAll)
                .addComponent(btnExport))
            .addContainerGap());

    groupLayout.setHorizontalGroup(horizontalGroup);
    groupLayout.setVerticalGroup(verticalGroup);
    setLayout(groupLayout);
    updateForms();
    setActiveExportState(exportStateActive);
  }

  private List<String> getErrors() {
    List<String> errors = new ArrayList<>();

    errors.addAll(confPanel.getConfiguration().getErrors());

    if (forms.noneSelected())
      errors.add("No form has been selected");

    return errors;
  }

  private void updateExportButton() {
    if (forms.someSelected() && (!confPanel.getConfiguration().isEmpty() || forms.allSelectedFormsHaveConfiguration()))
      btnExport.setEnabled(true);
    else
      btnExport.setEnabled(false);
  }

  private void updateClearAllButton() {
    btnClearAll.setVisible(forms.allSelected());
  }

  private void updateSelectAllButton() {
    btnSelectAll.setVisible(!forms.allSelected());
  }

  public void updateForms() {
    forms.merge(FileSystemUtils.getBriefcaseFormList().stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList()));
    formsTable.refresh();
  }

  private void showErrors(List<String> errors, String headerText, String title) {
    if (!errors.isEmpty()) {
      String message = String.format("%s\n\n%s", headerText, errors.stream().map(e -> "- " + e).collect(joining("\n")));
      showErrorDialog(this, message, title);
    }
  }

  /**
   * The DatePicker default text box and calendar button don't match with the rest of the UI.
   * This tweaks those elements to be consistent with the rest of the application.
   */
  private DatePicker createDatePicker() {
    DatePicker datePicker = new DatePicker();
    JTextField model = new JTextField();

    datePicker.getComponentToggleCalendarButton().setText("Choose...");
    datePicker.getComponentDateTextField().setBorder(model.getBorder());
    datePicker.getComponentDateTextField().setMargin(model.getMargin());

    return datePicker;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    updateForms();

    for (Component c : this.getComponents()) {
      c.setEnabled(enabled);
    }
    if (enabled) {
      // and then update the widgets based upon the transfer state
      setActiveExportState(exportStateActive);
    }
  }

  private void setActiveExportState(boolean active) {
    if (active) {
      // don't allow normal actions when we are transferring...
      confPanel.disable();
      btnExport.setEnabled(false);
      // reset the termination future so we can cancel activity
      terminationFuture.reset();
    } else {
      confPanel.enable();
      btnExport.setEnabled(true);
      // touch-up with real state...
      updateExportButton();
      // retain progress text (to display last export outcome)
    }
    // remember state...
    exportStateActive = active;
  }

  void resetExport() {
  }

  private List<String> export() {
    return forms.getSelectedForms().parallelStream()
        .peek(FormStatus::clearStatusHistory)
        .map(formStatus -> (BriefcaseFormDefinition) formStatus.getFormDefinition())
        .flatMap(formDefinition -> this.export(formDefinition).stream())
        .collect(toList());
  }

  private List<String> export(BriefcaseFormDefinition formDefinition) {
    List<String> errors = new ArrayList<>();
    ExportConfiguration configuration = forms.getConfiguration(formDefinition).orElse(confPanel.getConfiguration());
    Optional<File> pemFile = configuration.mapPemFile(Path::toFile).filter(File::exists);
    if ((formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) && !pemFile.isPresent())
      errors.add(formDefinition.getFormName() + " form requires is encrypted and you haven't defined a valid private key file location");
    else
      try {
        ExportAction.export(
            configuration.mapExportDir(Path::toFile).orElseThrow(() -> new RuntimeException("No export dir has been set")),
            CSV,
            formDefinition,
            pemFile.orElse(null),
            terminationFuture,
            configuration.mapStartDate(ld -> Date.from(ld.atStartOfDay(systemDefault()).toInstant())).orElse(null),
            configuration.mapEndDate(ld -> Date.from(ld.atStartOfDay(systemDefault()).toInstant())).orElse(null)
        );
      } catch (IOException ex) {
        errors.add("Export of form " + formDefinition.getFormName() + " has failed: " + ex.getMessage());
      }
    return errors;
  }

  @EventSubscriber(eventClass = TransferSucceededEvent.class)
  public void onTransferSucceededEvent(TransferSucceededEvent event) {
    updateForms();
  }
}
