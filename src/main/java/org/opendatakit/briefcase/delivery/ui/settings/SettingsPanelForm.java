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

package org.opendatakit.briefcase.delivery.ui.settings;

import static org.opendatakit.briefcase.reused.http.Http.DEFAULT_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.http.Http.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.reused.http.Http.MIN_HTTP_CONNECTIONS;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.reused.api.OptionalProduct;

@SuppressWarnings({"checkstyle:MethodName", "checkstyle:OneStatementPerLine", "checkstyle:RightCurlyAlone"})
public class SettingsPanelForm {
  public JPanel container;
  private JCheckBox resumeLastPullField;
  private JCheckBox rememberPasswordsField;
  private JCheckBox sendUsageDataField;
  private JCheckBox useHttpProxyField;
  private JTextField httpProxyHostField;
  private JSpinner httpProxyPortField;
  private JLabel httpProxyPortLabel;
  private JLabel httpProxyHostLabel;
  private JButton reloadCacheButton;
  private JButton cleanAllPullResumePointsButton;
  private JSpinner maxHttpConnectionsField;
  private JLabel maxHttpConnectionsLabel;
  private JPanel maxHttpConnectionContainer;
  private JPanel httpProxyContainer;
  private JLabel versionLabel;
  private final List<Consumer<Path>> onStorageLocationCallbacks = new ArrayList<>();
  private final List<Runnable> onClearStorageLocationCallbacks = new ArrayList<>();
  private final List<Consumer<HttpHost>> onHttpProxyCallbacks = new ArrayList<>();
  private final List<Runnable> onClearHttpProxyCallbacks = new ArrayList<>();

  SettingsPanelForm() {
    httpProxyPortField = new JIntegerSpinner(8080, 0, 65535, 1);
    maxHttpConnectionsField = new JIntegerSpinner(DEFAULT_HTTP_CONNECTIONS, MIN_HTTP_CONNECTIONS, MAX_HTTP_CONNECTIONS, 1);
    $$$setupUI$$$();

    useHttpProxyField.addActionListener(__ -> updateHttpProxyFields());

    httpProxyHostField.addFocusListener(onFocusLost(this::processHttpProxyFields));

    httpProxyPortField.addChangeListener(__ -> processHttpProxyFields());

    updateProxyFields(useHttpProxyField.isSelected());
  }

  void setVersion(String version) {
    versionLabel.setText("ODK Briefcase " + version);
  }

  void updateHttpProxyFields() {
    if (useHttpProxyField.isSelected()) {
      httpProxyHostField.setEnabled(true);
      httpProxyPortField.setEnabled(true);
    } else {
      httpProxyHostField.setEnabled(false);
      httpProxyHostField.setText("127.0.0.1");
      httpProxyPortField.setEnabled(false);
      httpProxyPortField.setValue(8080);
      onClearHttpProxyCallbacks.forEach(Runnable::run);
    }
  }

  private void processHttpProxyFields() {
    OptionalProduct.all(
        Optional.ofNullable(httpProxyHostField.getText()).map(String::trim).filter(s -> !s.isEmpty()),
        Optional.ofNullable(httpProxyPortField.getValue()).map(o -> (Integer) o)
    ).map(HttpHost::new).ifPresent(this::setHttpProxy);
  }

  private void updateProxyFields(boolean enabled) {
    httpProxyHostField.setEnabled(enabled);
    httpProxyPortField.setEnabled(enabled);
  }

  void enableUseHttpProxy() {
    useHttpProxyField.setSelected(true);
  }

  void setHttpProxy(HttpHost proxy) {
    httpProxyHostField.setText(proxy.getHostName());
    httpProxyPortField.setValue(proxy.getPort());
    onHttpProxyCallbacks.forEach(callback -> callback.accept(proxy));
  }

  void onHttpProxy(Consumer<HttpHost> onSet, Runnable onClear) {
    onHttpProxyCallbacks.add(onSet);
    onClearHttpProxyCallbacks.add(onClear);
  }

  private FocusListener onFocusLost(Runnable callback) {
    return new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        super.focusLost(e);
        callback.run();
      }
    };
  }

  void onMaxHttpConnectionsChange(Consumer<Integer> callback) {
    maxHttpConnectionsField.addChangeListener(__ -> callback.accept((Integer) maxHttpConnectionsField.getValue()));
  }

  void setMaxHttpConnections(int value) {
    maxHttpConnectionsField.setValue(value);
  }

  void onResumeLastPullChange(Consumer<Boolean> callback) {
    resumeLastPullField.addActionListener(__ -> callback.accept(resumeLastPullField.isSelected()));
  }

  void setResumeLastPull(Boolean enabled) {
    resumeLastPullField.setSelected(enabled);
  }

  void onRememberPasswordsChange(Consumer<Boolean> callback) {
    rememberPasswordsField.addActionListener(__ -> callback.accept(rememberPasswordsField.isSelected()));
  }

  void setRememberPasswords(Boolean enabled) {
    rememberPasswordsField.setSelected(enabled);
  }

  void onSendUsageDataChange(Consumer<Boolean> callback) {
    sendUsageDataField.addActionListener(__ -> callback.accept(sendUsageDataField.isSelected()));
  }

  void setSendUsageData(Boolean enabled) {
    sendUsageDataField.setSelected(enabled);
  }

  void onReloadCache(Runnable callback) {
    reloadCacheButton.addActionListener(__ -> callback.run());
  }

  void onCleanAllPullResumePoints(Runnable callback) {
    cleanAllPullResumePointsButton.addActionListener(__ -> callback.run());
  }

  private void createUIComponents() {
  }


  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    container = new JPanel();
    container.setLayout(new GridBagLayout());
    rememberPasswordsField = new JCheckBox();
    rememberPasswordsField.setText("Remember passwords (unencrypted)");
    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 8;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(rememberPasswordsField, gbc);
    sendUsageDataField = new JCheckBox();
    sendUsageDataField.setText("Send usage data and crash logs to core developers");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 10;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(sendUsageDataField, gbc);
    useHttpProxyField = new JCheckBox();
    useHttpProxyField.setText("Use HTTP Proxy");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 12;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(useHttpProxyField, gbc);
    final JPanel spacer1 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 11;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer1, gbc);
    final JPanel spacer2 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 6;
    gbc.gridy = 1;
    gbc.gridheight = 14;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer2, gbc);
    final JPanel spacer3 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridheight = 14;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    container.add(spacer3, gbc);
    final JPanel spacer4 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer4, gbc);
    final JPanel spacer5 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 16;
    gbc.gridwidth = 5;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer5, gbc);
    final JPanel spacer6 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 9;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer6, gbc);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 17;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(panel1, gbc);
    panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Troubleshooting"));
    reloadCacheButton = new JButton();
    reloadCacheButton.setText("Reload forms from storage location");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 8;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(reloadCacheButton, gbc);
    final JPanel spacer7 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 7;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel1.add(spacer7, gbc);
    final JPanel spacer8 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 9;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel1.add(spacer8, gbc);
    final JPanel spacer9 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 8;
    gbc.weightx = 0.5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(spacer9, gbc);
    final JPanel spacer10 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 8;
    gbc.weightx = 0.5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(spacer10, gbc);
    final JLabel label1 = new JLabel();
    label1.setText("The form list in Briefcase can get out of date if files are moved manually.");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.WEST;
    panel1.add(label1, gbc);
    final JLabel label2 = new JLabel();
    label2.setText("This reload is always safe.");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.WEST;
    panel1.add(label2, gbc);
    final JPanel spacer11 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(spacer11, gbc);
    final JPanel spacer12 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel1.add(spacer12, gbc);
    final JPanel spacer13 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(spacer13, gbc);
    final JPanel spacer14 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel1.add(spacer14, gbc);
    cleanAllPullResumePointsButton = new JButton();
    cleanAllPullResumePointsButton.setText("Clear pull history");
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel1.add(cleanAllPullResumePointsButton, gbc);
    final JLabel label3 = new JLabel();
    label3.setText("Use if you wish to pull every submission, regardless of last submission pulled.");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.WEST;
    panel1.add(label3, gbc);
    final JPanel spacer15 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.VERTICAL;
    panel1.add(spacer15, gbc);
    final JPanel spacer16 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 20;
    gbc.gridwidth = 6;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer16, gbc);
    resumeLastPullField = new JCheckBox();
    resumeLastPullField.setText("Start pull from last submission pulled");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 6;
    gbc.gridwidth = 5;
    gbc.anchor = GridBagConstraints.WEST;
    container.add(resumeLastPullField, gbc);
    final JPanel spacer17 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 7;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer17, gbc);
    final JPanel spacer18 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer18, gbc);
    httpProxyContainer = new JPanel();
    httpProxyContainer.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 15;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(httpProxyContainer, gbc);
    final JPanel spacer19 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    httpProxyContainer.add(spacer19, gbc);
    httpProxyHostLabel = new JLabel();
    httpProxyHostLabel.setText("Host");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    httpProxyContainer.add(httpProxyHostLabel, gbc);
    httpProxyPortLabel = new JLabel();
    httpProxyPortLabel.setText("Port");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    httpProxyContainer.add(httpProxyPortLabel, gbc);
    httpProxyHostField = new JTextField();
    httpProxyHostField.setPreferredSize(new Dimension(150, 30));
    httpProxyHostField.setText("127.0.0.1");
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    httpProxyContainer.add(httpProxyHostField, gbc);
    httpProxyPortField.setPreferredSize(new Dimension(150, 30));
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    httpProxyContainer.add(httpProxyPortField, gbc);
    final JPanel spacer20 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0.1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    httpProxyContainer.add(spacer20, gbc);
    final JPanel spacer21 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    httpProxyContainer.add(spacer21, gbc);
    final JPanel spacer22 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 14;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer22, gbc);
    maxHttpConnectionContainer = new JPanel();
    maxHttpConnectionContainer.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.BOTH;
    container.add(maxHttpConnectionContainer, gbc);
    gbc = new GridBagConstraints();
    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    maxHttpConnectionContainer.add(maxHttpConnectionsField, gbc);
    maxHttpConnectionsLabel = new JLabel();
    maxHttpConnectionsLabel.setText("Maximum simultaneous HTTP connections");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.WEST;
    maxHttpConnectionContainer.add(maxHttpConnectionsLabel, gbc);
    final JPanel spacer23 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    maxHttpConnectionContainer.add(spacer23, gbc);
    final JPanel spacer24 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 4;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    maxHttpConnectionContainer.add(spacer24, gbc);
    final JPanel spacer25 = new JPanel();
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 18;
    gbc.gridwidth = 5;
    gbc.fill = GridBagConstraints.VERTICAL;
    container.add(spacer25, gbc);
    versionLabel = new JLabel();
    versionLabel.setEnabled(false);
    Font versionLabelFont = this.$$$getFont$$$(null, Font.PLAIN, -1, versionLabel.getFont());
    if (versionLabelFont != null) versionLabel.setFont(versionLabelFont);
    versionLabel.setText("[version placeholder]");
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 19;
    gbc.gridwidth = 5;
    gbc.weightx = 1.0;
    container.add(versionLabel, gbc);
  }

  /**
   * @noinspection ALL
   */
  private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
    if (currentFont == null) return null;
    String resultName;
    if (fontName == null) {resultName = currentFont.getName();} else {
      Font testFont = new Font(fontName, Font.PLAIN, 10);
      if (testFont.canDisplay('a') && testFont.canDisplay('1')) {resultName = fontName;} else {resultName = currentFont.getName();}
    }
    return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() { return container; }

}
