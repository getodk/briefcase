package org.opendatakit.briefcase.ui.export;

import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.util.function.Consumer;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import org.opendatakit.briefcase.ui.export.components.ConfigurationPanel;
import org.opendatakit.briefcase.ui.export.components.ExportPanelBottomPanel;
import org.opendatakit.briefcase.ui.export.components.FormsTable;

public class ExportPanelView extends JPanel {
  private final ConfigurationPanel configurationPanel;
  private final FormsTable formsTable;
  private final ExportPanelBottomPanel bottomPanel;
  private final ExportForms forms;
  private boolean exporting;

  ExportPanelView(ExportForms forms, ExportConfiguration initialDefaultConfiguration) {
    super();
    this.forms = forms;
    configurationPanel = ConfigurationPanel.from(initialDefaultConfiguration);
    formsTable = new FormsTable(forms);
    bottomPanel = new ExportPanelBottomPanel();

    configurationPanel.onChange(this::updateBottomPanel);
    formsTable.onChange(this::updateBottomPanel);
    bottomPanel.onSelectAll(formsTable::selectAll);
    bottomPanel.onClearAll(formsTable::clearAll);

    exporting = false;

    JLabel lblFormsToTransfer = new JLabel("Forms to export:");

    JScrollPane scrollPane = new JScrollPane(formsTable.getView());
    JSeparator separatorFormsList = new JSeparator();


    GroupLayout panelLayout = new GroupLayout(this);

    panelLayout.setHorizontalGroup(panelLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(panelLayout.createParallelGroup(LEADING)
            .addComponent(this.configurationPanel.getView())
            .addComponent(separatorFormsList)
            .addComponent(lblFormsToTransfer)
            .addComponent(scrollPane)
            .addComponent(bottomPanel)
        )
        .addContainerGap());

    panelLayout.setVerticalGroup(panelLayout.createParallelGroup(LEADING)
        .addGroup(panelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(this.configurationPanel.getView())
            .addPreferredGap(RELATED)
            .addComponent(separatorFormsList)
            .addPreferredGap(RELATED)
            .addComponent(lblFormsToTransfer)
            .addPreferredGap(RELATED)
            .addComponent(scrollPane)
            .addPreferredGap(RELATED)
            .addComponent(bottomPanel)
            .addContainerGap()
        ));

    setLayout(panelLayout);
  }

  public void onExport(Consumer<ExportConfiguration> callback) {
    bottomPanel.onExport(() -> callback.accept(configurationPanel.getConfiguration()));
  }

  public void refresh() {
    formsTable.refresh();
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (enabled) {
      enableUI();
    } else {
      disableUI();
    }
    setExporting(exporting);
  }

  private void setExporting(boolean active) {
    if (active)
      disableUI();
    else
      enableUI();
    exporting = active;
  }

  private void updateBottomPanel() {
    if (forms.someSelected() && (configurationPanel.isValid() || forms.allSelectedFormsHaveConfiguration()))
      bottomPanel.enableExport();
    else
      bottomPanel.disableExport();

    if (forms.allSelected())
      bottomPanel.toggleClearAll();
    else
      bottomPanel.toggleSelectAll();
  }

  void disableUI() {
    configurationPanel.disable();
    formsTable.disable();
    bottomPanel.setEnabled(false);
  }

  void enableUI() {
    configurationPanel.enable();
    formsTable.enable();
    bottomPanel.setEnabled(true);
    // and then update the widgets based upon the transfer state
    formsTable.refresh();
  }
}
