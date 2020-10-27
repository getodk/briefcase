/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.lang.Integer.parseInt;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.ui.reused.UiFieldValidator.EMAIL;
import static org.opendatakit.briefcase.ui.reused.UiFieldValidator.NUMBER;
import static org.opendatakit.briefcase.ui.reused.UiFieldValidator.REQUIRED;
import static org.opendatakit.briefcase.ui.reused.UiFieldValidator.URI;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.ui.reused.FocusAdapterBuilder;
import org.opendatakit.briefcase.ui.reused.UI;
import org.opendatakit.briefcase.ui.reused.UiFieldValidator;
import org.opendatakit.briefcase.ui.reused.WindowAdapterBuilder;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:WhitespaceAround", "checkstyle:OneStatementPerLine", "checkstyle:RightCurlyAlone"})
public class CentralServerDialogForm extends JDialog {

  private final List<Consumer<CentralServer>> onConnectCallbacks = new ArrayList<>();
  JButton cancelButton;
  JButton connectButton;
  JTextField urlField;
  JTextField emailField;
  JPasswordField passwordField;
  JProgressBar progressBar;
  private JPanel dialog;
  private JPanel actions;
  private JLabel projectIdFieldLabel;
  private JTextField projectIdField;
  private JLabel urlFieldLabel;
  private JLabel emailFieldLabel;
  private JLabel passwordFieldLabel;
  private UiFieldValidator urlValidator;
  private UiFieldValidator projectIdValidator;
  private UiFieldValidator emailValidator;
  private UiFieldValidator passwordValidator;

  CentralServerDialogForm() {
    $$$setupUI$$$();
    setContentPane(dialog);
    setPreferredSize(new Dimension(500, 240));
    setModal(true);
    pack();
    setLocationRelativeTo(null);
    setTitle("Central Server Configuration");
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapterBuilder().onClosing(e -> dispose()).build());

    dialog.registerKeyboardAction(e -> dispose(), getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    cancelButton.addActionListener(e -> dispose());

    connectButton.addActionListener(__ -> triggerConnect());

    urlField.addFocusListener(new FocusAdapterBuilder()
        .onFocusLost(e -> urlField.setText(CentralServer.cleanUrl(urlField.getText())))
        .build());

    urlValidator = UiFieldValidator.of(urlField, urlFieldLabel, REQUIRED, URI).onChange(this::updateConnectButton);
    projectIdValidator = UiFieldValidator.of(projectIdField, projectIdFieldLabel, REQUIRED, NUMBER).onChange(this::updateConnectButton);
    emailValidator = UiFieldValidator.of(emailField, emailFieldLabel, REQUIRED, EMAIL).onChange(this::updateConnectButton);
    passwordValidator = UiFieldValidator.of(passwordField, passwordFieldLabel, REQUIRED).onChange(this::updateConnectButton);

    getRootPane().setDefaultButton(connectButton);
  }

  private void updateConnectButton() {
    connectButton.setEnabled(Stream.of(
        urlValidator.isValid(),
        projectIdValidator.isValid(),
        emailValidator.isValid(),
        passwordValidator.isValid()
    ).reduce(true, Boolean::logicalAnd));
  }

  private void triggerConnect() {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      URL baseUrl = new URL(urlField.getText());

      CentralServer server = CentralServer.of(
          baseUrl,
          parseInt(projectIdField.getText()),
          UI.credentialsFromFields(emailField, passwordField).orElseThrow(BriefcaseException::new)
      );

      onConnectCallbacks.forEach(callback -> callback.accept(server));
    } catch (BriefcaseException e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      errorMessage("Invalid Central configuration", "Please, check data and try again.\n\nError: " + e.getCause().getMessage());
    } catch (MalformedURLException e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      errorMessage("Invalid Central configuration", "Malformed URL. Please, review data and try again.\n\nError: " + e.getMessage());
    }
  }

  private void createUIComponents() {
    // TODO: place custom component creation code here
  }

  void onConnect(Consumer<CentralServer> callback) {
    onConnectCallbacks.add(callback);
  }

  void hideDialog() {
    setVisible(false);
  }

  void setTestingConnection() {
    cancelButton.setEnabled(false);
    urlField.setEditable(false);
    emailField.setEditable(false);
    passwordField.setEditable(false);
    connectButton.setEnabled(false);
    progressBar.setVisible(true);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  void unsetTestingConnection() {
    cancelButton.setEnabled(true);
    urlField.setEditable(true);
    emailField.setEditable(true);
    passwordField.setEditable(true);
    connectButton.setEnabled(true);
    progressBar.setVisible(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    dialog = new JPanel();
    dialog.setLayout(new GridBagLayout());
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridBagLayout());
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 3;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    dialog.add(panel1, gbc);
    actions = new JPanel();
    actions.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(actions, gbc);
    cancelButton = new JButton();
    cancelButton.setText("Cancel");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    actions.add(cancelButton, gbc);
    connectButton = new JButton();
    connectButton.setEnabled(false);
    connectButton.setHideActionText(false);
    connectButton.setText("Connect");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.EAST;
    actions.add(connectButton, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    actions.add(spacer1, gbc);
    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    actions.add(progressBar, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(spacer4, gbc);
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    dialog.add(panel2, gbc);
    urlField = new JTextField();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(urlField, gbc);
    urlFieldLabel = new JLabel();
    urlFieldLabel.setText("URL");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(urlFieldLabel, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(spacer5, gbc);
    emailFieldLabel = new JLabel();
    emailFieldLabel.setText("Email");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(emailFieldLabel, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 7;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(spacer6, gbc);
    emailField = new JTextField();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 7;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(emailField, gbc);
    passwordFieldLabel = new JLabel();
    passwordFieldLabel.setText("Password");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 8;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(passwordFieldLabel, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 8;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(spacer7, gbc);
    final JPanel spacer8 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel2.add(spacer8, gbc);
    passwordField = new JPasswordField();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 8;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(passwordField, gbc);
    projectIdFieldLabel = new JLabel();
    projectIdFieldLabel.setText("Project ID");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(projectIdFieldLabel, gbc);
    projectIdField = new JTextField();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(projectIdField, gbc);
    final JLabel label1 = new JLabel();
    Font label1Font = this.$$$getFont$$$(null, Font.PLAIN, -1, label1.getFont());
    if (label1Font != null) label1.setFont(label1Font);
    label1.setText("You can copy and paste the URL from your web browser");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.WEST;
    panel2.add(label1, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel2.add(spacer9, gbc);
    final JLabel label2 = new JLabel();
    Font label2Font = this.$$$getFont$$$(null, Font.PLAIN, -1, label2.getFont());
    if (label2Font != null) label2.setFont(label2Font);
    label2.setText("Project ID is the last number in the URL of the Project Overview");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 5;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.WEST;
    panel2.add(label2, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 6;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel2.add(spacer10, gbc);
    final JPanel spacer11 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer11, gbc);
  }

  /**
   * @noinspection ALL
   */
  private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
    if (currentFont == null) return null;
    String resultName;
    if (fontName == null) {
      resultName = currentFont.getName();
    } else {
      Font testFont = new Font(fontName, Font.PLAIN, 10);
      if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
        resultName = fontName;
      } else {
        resultName = currentFont.getName();
      }
    }
    return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return dialog;
  }

}
