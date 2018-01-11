package org.opendatakit.briefcase.ui.export.components;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static org.opendatakit.briefcase.ui.ODKOptionPane.showErrorDialog;
import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.FileChooser.directory;
import static org.opendatakit.briefcase.ui.reused.FileChooser.file;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import com.github.lgooddatepicker.components.DatePicker;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.StringUtils;

class ConfigurationPanelView extends JPanel {
  protected final JTextField exportDirectoryField;
  protected final JTextField pemFileField;
  protected final DatePicker dateRangeStartField;
  protected final DatePicker dateRangeEndField;
  private final List<Consumer<Path>> onSelectExportDirCallbacks = new ArrayList<>();
  private final List<Consumer<Path>> onSelectPemFileCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectDateRangeStartCallbacks = new ArrayList<>();
  private final List<Consumer<LocalDate>> onSelectDateRangeEndCallbacks = new ArrayList<>();
  private final JButton exportDirectoryButton;
  private final JButton pemFileButton;

  ConfigurationPanelView() {
    JLabel exportDirectoryLabel = new JLabel("Export Directory:");
    exportDirectoryButton = new JButton("Choose...");
    exportDirectoryField = new JTextField();
    exportDirectoryField.setFocusable(false);
    exportDirectoryField.setEditable(false);
    exportDirectoryField.setColumns(10);
    exportDirectoryButton.addActionListener(__ -> buildExportDirFileDialog().choose()
        .ifPresent(file -> setExportDir(Paths.get(file.toURI()))));

    JLabel pemFileLabel = new JLabel("PEM Private Key File:");
    pemFileButton = new JButton("Choose...");
    pemFileField = new JTextField();
    pemFileField.setFocusable(false);
    pemFileField.setEditable(false);
    pemFileField.setColumns(10);
    pemFileButton.addActionListener(__ -> buildPemFileDialog().choose()
        .ifPresent(file -> setPemFile(Paths.get(file.toURI()))));

    JLabel dateRangeStartLabel = new JLabel("Start Date (inclusive):");
    dateRangeStartField = createDatePicker();
    dateRangeStartField.addDateChangeListener(event -> {
      LocalDate date = event.getNewDate() != null
          ? LocalDate.of(event.getNewDate().getYear(), event.getNewDate().getMonthValue(), event.getNewDate().getDayOfMonth())
          : null;
      onSelectDateRangeStartCallbacks.forEach(consumer -> consumer.accept(date));
    });
    JLabel dateRangeEndLabel = new JLabel("End Date (exclusive):");
    dateRangeEndField = createDatePicker();
    dateRangeEndField.addDateChangeListener(event -> {
      LocalDate date = event.getNewDate() != null
          ? LocalDate.of(event.getNewDate().getYear(), event.getNewDate().getMonthValue(), event.getNewDate().getDayOfMonth())
          : null;
      onSelectDateRangeEndCallbacks.forEach(consumer -> consumer.accept(date));
    });

    GroupLayout groupLayout = new GroupLayout(this);

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

    GroupLayout.ParallelGroup verticalGroup = groupLayout.createParallelGroup(LEADING)
        .addGroup(groupLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(exportDirectoryLabel)
                .addComponent(exportDirectoryButton)
                .addComponent(exportDirectoryField, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            )
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(pemFileLabel)
                .addComponent(pemFileField, DEFAULT_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(pemFileButton)
            )
            .addPreferredGap(RELATED)
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(dateRangeStartLabel)
                .addComponent(dateRangeStartField)
            )
            .addPreferredGap(RELATED)
            .addGroup(groupLayout.createParallelGroup(BASELINE)
                .addComponent(dateRangeEndLabel)
                .addComponent(dateRangeEndField)
            )
            .addContainerGap());
    groupLayout.setHorizontalGroup(horizontalGroup);
    groupLayout.setVerticalGroup(verticalGroup);
    setLayout(groupLayout);
    setBorder(new EmptyBorder(5, 5, 5, 5));
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
    // Route the change through the date picker's date to avoid repeated set calls
    dateRangeStartField.setDate(org.threeten.bp.LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void onSelectDateRangeStart(Consumer<LocalDate> callback) {
    onSelectDateRangeStartCallbacks.add(callback);
  }

  public void clearDateRangeStart() {
    dateRangeStartField.clear();
  }

  void setDateRangeEnd(LocalDate date) {
    // Route the change through the date picker's date to avoid repeated set calls
    dateRangeEndField.setDate(org.threeten.bp.LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
  }

  void onSelectDateRangeEnd(Consumer<LocalDate> callback) {
    onSelectDateRangeEndCallbacks.add(callback);
  }

  public void clearDateRangeEnd() {
    dateRangeEndField.clear();
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

  public void showError(String message, String title) {
    showErrorDialog(this, message, title);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (enabled) {
      exportDirectoryButton.setEnabled(true);
      pemFileButton.setEnabled(true);
      dateRangeStartField.setEnabled(true);
      dateRangeEndField.setEnabled(true);
    } else {
      exportDirectoryButton.setEnabled(false);
      pemFileButton.setEnabled(false);
      dateRangeStartField.setEnabled(false);
      dateRangeEndField.setEnabled(false);
    }
  }
}
