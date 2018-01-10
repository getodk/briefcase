package org.opendatakit.briefcase.ui.export;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static org.opendatakit.briefcase.ui.MessageStrings.INVALID_DATE_RANGE_MESSAGE;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.export.FileChooser.directory;
import static org.opendatakit.briefcase.ui.export.FileChooser.file;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import com.github.lgooddatepicker.components.DatePicker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.opendatakit.briefcase.util.StringUtils;

public class ExportConfigurationDialog extends JDialog {
  private final JTextField txtExportDirectory;
  private final JButton btnChooseExportDirectory;

  private final JTextField pemPrivateKeyFilePath;
  private final JButton btnPemFileChooseButton;

  private final DatePicker pickStartDate;
  private final DatePicker pickEndDate;

  private JButton removeConfigButton;
  private JButton applyConfigButton;
  private JButton cancelButton;

  private final ExportConfiguration config;

  public ExportConfigurationDialog(Window app, ExportConfiguration config, Runnable onRemove, Consumer<ExportConfiguration> onApply) {
    super(app, "Form export configuration", ModalityType.DOCUMENT_MODAL);

    this.config = config;

    setBounds(100, 100, 450, 234);
    getContentPane().setLayout(new BorderLayout());

    JLabel lblExportDirectory = new JLabel("Export Directory:");
    txtExportDirectory = new JTextField();
    txtExportDirectory.setFocusable(false);
    txtExportDirectory.setEditable(false);
    txtExportDirectory.setColumns(10);
    config.ifExportDirPresent((Path exportDir) -> txtExportDirectory.setText(exportDir.toString()));

    btnChooseExportDirectory = new JButton("Choose...");
    btnChooseExportDirectory.addActionListener(__ -> buildExportDirFileDialog()
        .choose()
        .ifPresent(file -> {
          txtExportDirectory.setText(file.getAbsolutePath());
          config.setExportDir(Paths.get(file.toURI()));
          updateAcceptButton();
        }));

    JLabel lblPemPrivateKey = new JLabel("PEM Private Key File:");

    pemPrivateKeyFilePath = new JTextField();
    pemPrivateKeyFilePath.setFocusable(false);
    pemPrivateKeyFilePath.setEditable(false);
    pemPrivateKeyFilePath.setColumns(10);
    config.ifPemFilePresent((Path pemFile) -> pemPrivateKeyFilePath.setText(pemFile.toString()));

    btnPemFileChooseButton = new JButton("Choose...");
    btnPemFileChooseButton.addActionListener(__ -> buildPemFileDialog()
        .choose()
        .ifPresent(file -> {
          pemPrivateKeyFilePath.setText(file.getAbsolutePath());
          config.setPemFile(Paths.get(file.toURI()));
          updateAcceptButton();
        }));

    JLabel lblDateFrom = new JLabel("Start Date (inclusive):");
    JLabel lblDateTo = new JLabel("End Date (exclusive):");

    pickStartDate = createDatePicker();
    pickStartDate.addDateChangeListener(__ -> {
      config.setDateRangeStart(pickStartDate.convert().getDateWithDefaultZone());
      if (!config.isDateRangeValid()) {
        showErrorDialog(this, INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        pickStartDate.clear();
      }
      updateAcceptButton();
    });

    pickEndDate = createDatePicker();
    pickEndDate.addDateChangeListener(__ -> {
      config.setDateRangeEnd(pickEndDate.convert().getDateWithDefaultZone());
      if (!config.isDateRangeValid()) {
        showErrorDialog(this, INVALID_DATE_RANGE_MESSAGE, "Export configuration error");
        pickEndDate.clear();
      }
      updateAcceptButton();
    });

    removeConfigButton = new JButton("Remove");
    removeConfigButton.setActionCommand("Remove");
    removeConfigButton.addActionListener(__ -> {
      onRemove.run();
      closeDialog();
    });
    removeConfigButton.setEnabled(!config.isEmpty());

    applyConfigButton = new JButton("Apply");
    applyConfigButton.setActionCommand("Apply");
    applyConfigButton.addActionListener(__ -> {
      onApply.accept(config);
      closeDialog();
    });

    cancelButton = new JButton("Cancel");
    cancelButton.setActionCommand("Cancel");
    cancelButton.addActionListener(__ -> closeDialog());


    JPanel contentPanel = new JPanel();

    GroupLayout groupLayout = new GroupLayout(contentPanel);

    GroupLayout.ParallelGroup labels = groupLayout.createParallelGroup(LEADING)
        .addComponent(lblExportDirectory)
        .addComponent(lblPemPrivateKey)
        .addComponent(lblDateFrom)
        .addComponent(lblDateTo);

    GroupLayout.ParallelGroup fields = groupLayout.createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup().addComponent(txtExportDirectory).addPreferredGap(RELATED).addComponent(btnChooseExportDirectory))
        .addGroup(groupLayout.createSequentialGroup().addComponent(pemPrivateKeyFilePath).addPreferredGap(RELATED).addComponent(btnPemFileChooseButton))
        .addComponent(pickStartDate)
        .addComponent(pickEndDate);

    GroupLayout.SequentialGroup horizontalGroup = groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(LEADING)
            .addGroup(groupLayout.createSequentialGroup()
                .addGroup(labels)
                .addPreferredGap(RELATED)
                .addGroup(fields)
            ))
        .addContainerGap();

    GroupLayout.ParallelGroup verticalGroup = groupLayout
        .createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(lblExportDirectory)
                .addComponent(btnChooseExportDirectory)
                .addComponent(txtExportDirectory, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            )
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(lblPemPrivateKey).addComponent(pemPrivateKeyFilePath, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE).addComponent(btnPemFileChooseButton)).addPreferredGap(RELATED)
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(lblDateFrom).addComponent(pickStartDate))
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(lblDateTo).addComponent(pickEndDate))
            .addContainerGap());

    groupLayout.setHorizontalGroup(horizontalGroup);
    groupLayout.setVerticalGroup(verticalGroup);
    contentPanel.setLayout(groupLayout);
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
    buttonPane.add(cancelButton);
    buttonPane.add(removeConfigButton);
    buttonPane.add(applyConfigButton);

    getRootPane().setDefaultButton(applyConfigButton);
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    getContentPane().add(buttonPane, BorderLayout.SOUTH);
  }

  private FileChooser buildPemFileDialog() {
    return file(this, fileFrom(pemPrivateKeyFilePath));
  }

  private FileChooser buildExportDirFileDialog() {
    return directory(
        this,
        fileFrom(txtExportDirectory),
        f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f),
        "Exclude Briefcase & ODK directories"
    );
  }

  private void closeDialog() {
    setVisible(false);
  }

  private void updateAcceptButton() {
    applyConfigButton.setEnabled(config.isValid());
  }

  private Optional<File> fileFrom(JTextField textField) {
    return Optional.ofNullable(textField.getText())
        .filter(StringUtils::nullOrEmpty)
        .map(path -> Paths.get(path).toFile());
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

}
