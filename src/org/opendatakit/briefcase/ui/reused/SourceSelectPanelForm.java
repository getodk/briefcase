package org.opendatakit.briefcase.ui.reused;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.opendatakit.briefcase.ui.ODKCollectFileChooser;
import org.opendatakit.briefcase.ui.WrappedFileChooser;

@SuppressWarnings("checkstyle:MethodName")
public class SourceSelectPanelForm extends JComponent {
  private JComboBox sourceComboBox;
  private JButton configureButton;
  public JPanel container;
  private final List<Consumer<AggregateServerConnectionConfiguration>> onAggregateSourceCallback = new ArrayList<>();
  private final List<Consumer<Path>> onCustomDirectorySourceCallbacks = new ArrayList<>();
  public static final String SOURCE_AGGREGATE = "Aggregate Server";
  public static final String SOURCE_CUSTOM_DIR = "Custom ODK Directory";

  SourceSelectPanelForm() {
    $$$setupUI$$$();

    configureButton.addActionListener(__ -> {
      configureButton.setEnabled(false);
      triggerConfigure();
      configureButton.setEnabled(true);
    });
  }

  private void triggerConfigure() {
    String source = (String) sourceComboBox.getSelectedItem();
    if (source.equals(SOURCE_AGGREGATE)) {
      AggregateServerConnectionDialog dialog = AggregateServerConnectionDialog.empty(__ -> true);
      dialog.onConnect(this::triggerAggregateConnection);
      dialog.getForm().setVisible(true);
    } else if (source.equals(SOURCE_CUSTOM_DIR)) {
      // Invoke the file chooser here
      WrappedFileChooser fc = new WrappedFileChooser(SourceSelectPanelForm.this,
          new ODKCollectFileChooser(SourceSelectPanelForm.this));

      int retVal = fc.showDialog();

      if (retVal == JFileChooser.APPROVE_OPTION)
        triggerCustomDirSource(Paths.get(fc.getSelectedFile().getAbsolutePath()));
    }
  }

  public void onAggregateConnection(Consumer<AggregateServerConnectionConfiguration> consumer) {
    onAggregateSourceCallback.add(consumer);
  }

  private void triggerAggregateConnection(AggregateServerConnectionConfiguration conf) {
    onAggregateSourceCallback.forEach(callback -> callback.accept(conf));
  }

  public void onCustomDir(Consumer<Path> consumer) {
    onCustomDirectorySourceCallbacks.add(consumer);
  }

  private void triggerCustomDirSource(Path path) {
    onCustomDirectorySourceCallbacks.forEach(callback -> callback.accept(path));
  }

  private void createUIComponents() {
    // TODO: place custom component creation code here
  }

  public JPanel getContainer() {
    return container;
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    container = new JPanel();
    container.setLayout(new GridBagLayout());
    final JLabel label1 = new JLabel();
    label1.setText("Pull Data From");
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    container.add(label1, gbc);
    sourceComboBox = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    defaultComboBoxModel1.addElement("Aggregate Server");
    defaultComboBoxModel1.addElement("Custom ODK Directory");
    sourceComboBox.setModel(defaultComboBoxModel1);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(sourceComboBox, gbc);
    configureButton = new JButton();
    configureButton.setText("Configure");
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(configureButton, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer2, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return container;
  }
}
