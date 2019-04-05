/*
 * Copyright (C) 2018 Nafundi
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
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.ui.reused.WindowAdapterBuilder;

@SuppressWarnings("checkstyle:MethodName")
public class RemoteServerDialogForm extends JDialog {
  private JPanel dialog;
  private JPanel actions;
  JButton cancelButton;
  JButton connectButton;
  JTextField urlField;
  JTextField usernameField;
  JPasswordField passwordField;
  JProgressBar progressBar;
  private JTextPane accountTipTextPane;
  private final List<Consumer<RemoteServer>> onConnectCallbacks = new ArrayList<>();

  RemoteServerDialogForm(String requiredPermission) {
    $$$setupUI$$$();
    accountTipTextPane.setText("Username cannot be a Google login; it must be an ODK Aggregate username with \"" + requiredPermission + "\" permissions");
    setContentPane(dialog);
    setPreferredSize(new Dimension(500, 240));
    setModal(true);
    pack();
    setLocationRelativeTo(null);
    setTitle("Aggregate Server Configuration");
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapterBuilder().onClosing(e -> dispose()).build());

    dialog.registerKeyboardAction(e -> dispose(), getKeyStroke(VK_ESCAPE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    cancelButton.addActionListener(e -> dispose());

    connectButton.addActionListener(__ -> triggerConnect());

    getRootPane().setDefaultButton(connectButton);

  }

  private void triggerConnect() {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      Optional<Credentials> credentials = OptionalProduct.all(
          Optional.ofNullable(usernameField.getText()).map(String::trim).filter(s -> !s.isEmpty()),
          Optional.of(new String(passwordField.getPassword())).filter(s -> !s.isEmpty())
      ).map(Credentials::new);

      URL baseUrl = new URL(urlField.getText());

      RemoteServer server = credentials
          .map(c -> RemoteServer.authenticated(baseUrl, c))
          .orElse(RemoteServer.normal(baseUrl));

      onConnectCallbacks.forEach(callback -> callback.accept(server));
    } catch (BriefcaseException e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      errorMessage("Invalid Aggregate configuration", "Please, check data and try again.\n\nError: " + e.getCause().getMessage());
    } catch (MalformedURLException e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      errorMessage("Invalid Aggregate configuration", "Malformed URL. Please, review data and try again.\n\nError: " + e.getMessage());
    }
  }

  private void createUIComponents() {
    // TODO: place custom component creation code here
  }

  void onConnect(Consumer<RemoteServer> callback) {
    onConnectCallbacks.add(callback);
  }

  void hideDialog() {
    setVisible(false);
  }

  void setTestingConnection() {
    cancelButton.setEnabled(false);
    urlField.setEditable(false);
    usernameField.setEditable(false);
    passwordField.setEditable(false);
    connectButton.setEnabled(false);
    progressBar.setVisible(true);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  void unsetTestingConnection() {
    cancelButton.setEnabled(true);
    urlField.setEditable(true);
    usernameField.setEditable(true);
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
    gbc.gridy = 4;
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
    gbc.gridy = 5;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    dialog.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
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
    final JLabel label1 = new JLabel();
    label1.setText("URL");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(label1, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(spacer5, gbc);
    final JLabel label2 = new JLabel();
    label2.setText("Username");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(label2, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(spacer6, gbc);
    usernameField = new JTextField();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(usernameField, gbc);
    final JLabel label3 = new JLabel();
    label3.setText("Password");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.EAST;
    panel2.add(label3, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
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
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel2.add(passwordField, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.gridwidth = 3;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer9, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.VERTICAL;
    dialog.add(spacer10, gbc);
    accountTipTextPane = new JTextPane();
    accountTipTextPane.setEditable(false);
    accountTipTextPane.setMinimumSize(new Dimension(600, 60));
    accountTipTextPane.setOpaque(false);
    accountTipTextPane.setPreferredSize(new Dimension(600, 60));
    accountTipTextPane.setText("Username cannot be a Google login; it must be an ODK Aggregate username with \"Form Manager\" permissions");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.BOTH;
    dialog.add(accountTipTextPane, gbc);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return dialog;
  }
}
