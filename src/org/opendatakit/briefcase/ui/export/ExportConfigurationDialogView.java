package org.opendatakit.briefcase.ui.export;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

public class ExportConfigurationDialogView extends JDialog {
  private final JTextField exportDirectoryField;
  private final JTextField pemFileField;
  private final DatePicker dateRangeStartField;
  private final DatePicker dateRangeEndField;
  private final JButton applyConfigButton;

  private final List<Consumer<Path>> onSelectExportDirCallbacks = new ArrayList<>();
  private final List<Consumer<Path>> onSelectPemFileCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectDateRangeStartCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectDateRangeEndCallbacks = new ArrayList<>();
  private final List<Runnable> onClickRemoveConfigCallbacks = new ArrayList<>();
  private final List<Runnable> onClickApplyConfigCallbacks = new ArrayList<>();

  ExportConfigurationDialogView(Window app) {
    super(app, "Form export configuration", ModalityType.DOCUMENT_MODAL);

    setBounds(100, 100, 450, 234);
    getContentPane().setLayout(new BorderLayout());

    JLabel exportDirectoryLabel = new JLabel("Export Directory:");
    JButton exportDirectoryButton = new JButton("Choose...");
    exportDirectoryField = new JTextField();
    exportDirectoryField.setFocusable(false);
    exportDirectoryField.setEditable(false);
    exportDirectoryField.setColumns(10);
    exportDirectoryButton.addActionListener(__ -> buildExportDirFileDialog().choose()
        .ifPresent(file -> setExportDir(Paths.get(file.toURI()))));

    JLabel pemFileLabel = new JLabel("PEM Private Key File:");
    JButton pemFileButton = new JButton("Choose...");
    pemFileField = new JTextField();
    pemFileField.setFocusable(false);
    pemFileField.setEditable(false);
    pemFileField.setColumns(10);
    pemFileButton.addActionListener(__ -> buildPemFileDialog().choose()
        .ifPresent(file -> setPemFile(Paths.get(file.toURI()))));

    JLabel dateRangeStartLabel = new JLabel("Start Date (inclusive):");
    dateRangeStartField = createDatePicker();
    dateRangeStartField.addDateChangeListener(event -> onSelectDateRangeStartCallbacks.forEach(consumer -> consumer.accept(LocalDate.of(
        event.getNewDate().getYear(),
        event.getNewDate().getMonthValue(),
        event.getNewDate().getDayOfMonth()
    ))));
    JLabel dateRangeEndLabel = new JLabel("End Date (exclusive):");
    dateRangeEndField = createDatePicker();
    dateRangeEndField.addDateChangeListener(event -> onSelectDateRangeEndCallbacks.forEach(consumer -> consumer.accept(LocalDate.of(
        event.getNewDate().getYear(),
        event.getNewDate().getMonthValue(),
        event.getNewDate().getDayOfMonth()
    ))));

    JButton removeConfigButton = new JButton("Remove");
    removeConfigButton.setActionCommand("Remove");
    removeConfigButton.addActionListener(__ -> onClickRemoveConfigCallbacks.forEach(Runnable::run));

    applyConfigButton = new JButton("Apply");
    applyConfigButton.setActionCommand("Apply");
    applyConfigButton.addActionListener(__ -> onClickApplyConfigCallbacks.forEach(Runnable::run));

    JButton cancelButton = new JButton("Cancel");
    cancelButton.setActionCommand("Cancel");
    cancelButton.addActionListener(__ -> closeDialog());

    JPanel contentPanel = new JPanel();

    GroupLayout groupLayout = new GroupLayout(contentPanel);

    GroupLayout.ParallelGroup labels = groupLayout.createParallelGroup(LEADING)
        .addComponent(exportDirectoryLabel)
        .addComponent(pemFileLabel)
        .addComponent(dateRangeStartLabel)
        .addComponent(dateRangeEndLabel);

    GroupLayout.ParallelGroup fieldsAndButtons = groupLayout.createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addComponent(exportDirectoryField)
            .addPreferredGap(RELATED)
            .addComponent(exportDirectoryButton))
        .addGroup(groupLayout.createSequentialGroup()
            .addComponent(pemFileField)
            .addPreferredGap(RELATED)
            .addComponent(pemFileButton))
        .addComponent(dateRangeStartField)
        .addComponent(dateRangeEndField);

    GroupLayout.SequentialGroup horizontalGroup = groupLayout.createSequentialGroup()
        .addContainerGap()
        .addGroup(groupLayout.createParallelGroup(LEADING)
            .addGroup(groupLayout.createSequentialGroup()
                .addGroup(labels)
                .addPreferredGap(RELATED)
                .addGroup(fieldsAndButtons)
            ))
        .addContainerGap();

    GroupLayout.ParallelGroup verticalGroup = groupLayout
        .createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(exportDirectoryLabel)
                .addComponent(exportDirectoryButton)
                .addComponent(exportDirectoryField, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            )
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(pemFileLabel).addComponent(pemFileField, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE).addComponent(pemFileButton)).addPreferredGap(RELATED)
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(dateRangeStartLabel).addComponent(dateRangeStartField))
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE).addComponent(dateRangeEndLabel).addComponent(dateRangeEndField))
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

  void closeDialog() {
    setVisible(false);
  }

  void setExportDir(Path path) {
    exportDirectoryField.setText(path.toString());
    onSelectExportDirCallbacks.forEach(consumer -> consumer.accept(path));
  }

  void onSelectExportDir(Consumer<Path> callback) {
    onSelectExportDirCallbacks.add(callback);
  }

  void setPemFile(Path path) {
    pemFileField.setText(path.toString());
    onSelectPemFileCallbacks.forEach(consumer -> consumer.accept(path));
  }

  void onSelectPemFile(Consumer<Path> callback) {
    onSelectPemFileCallbacks.add(callback);
  }

  void setDateRangeStart(LocalDate date) {
    dateRangeStartField.setDate(org.threeten.bp.LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void onSelectDateRangeStart(Consumer<LocalDate> callback) {
    onSelectDateRangeStartCallbacks.add(callback);
  }

  public void clearDateRangeStart() {
    dateRangeStartField.clear();
  }

  void setDateRangeEnd(LocalDate date) {
    dateRangeEndField.setDate(org.threeten.bp.LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void onSelectDateRangeEnd(Consumer<LocalDate> callback) {
    onSelectDateRangeEndCallbacks.add(callback);
  }

  public void clearDateRangeEnd() {
    dateRangeEndField.clear();
  }

  public void onClickRemoveConfig(Runnable callback) {
    onClickRemoveConfigCallbacks.add(callback);
  }

  public void onClickApplyConfig(Runnable callback) {
    onClickApplyConfigCallbacks.add(callback);
  }

  public void enableApplyConfig() {
    applyConfigButton.setEnabled(true);
  }

  public void disableApplyConfig() {
    applyConfigButton.setEnabled(false);
  }

  public void open() {
    setVisible(true);
  }

  void showError(String message, String title) {
    showErrorDialog(getOwner(), message, title);
  }

  private FileChooser buildPemFileDialog() {
    return file(this, fileFrom(pemFileField));
  }

  private FileChooser buildExportDirFileDialog() {
    return directory(
        this,
        fileFrom(exportDirectoryField),
        f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f),
        "Exclude Briefcase & ODK directories"
    );
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
