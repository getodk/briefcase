package org.opendatakit.briefcase.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.http.HttpHost;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.util.StringUtils;


/**
 *
 */
public class SettingsPanel extends JPanel {

    public static final String TAB_NAME = "Settings";

    public static int TAB_POSITION = -1;

    private JLabel lblBriefcaseDirectory;
    private JTextField txtBriefcaseDir;
    private JButton btnChoose;
    private MainBriefcaseWindow parentWindow;

    private ArrayList<Component> navOrder = new ArrayList<Component>();
    private JLabel lblProxy;
    private JCheckBox chkProxy;
    private JLabel lblHost;
    private JTextField txtHost;
    private JLabel lblPort;
    private JSpinner spinPort;
    private JLabel lblParallel;
    private JCheckBox chkParallel;
    private JLabel lblTrackingConsent;
    private JCheckBox chkTrackingConsent;

    public SettingsPanel(MainBriefcaseWindow parentWindow) {
        this.parentWindow = parentWindow;
        lblBriefcaseDirectory = new JLabel(MessageStrings.BRIEFCASE_STORAGE_LOCATION);

        txtBriefcaseDir = new JTextField();
        txtBriefcaseDir.setFocusable(false);
        txtBriefcaseDir.setEditable(false);
        txtBriefcaseDir.setColumns(20);

        btnChoose = new JButton("Change...");
        btnChoose.addActionListener(new FolderActionListener());

        ProxyChangeListener proxyChangeListener = new ProxyChangeListener();

        lblHost = new JLabel(MessageStrings.PROXY_HOST);
        txtHost = new JTextField();
        txtHost.setEnabled(false);
        txtHost.setColumns(20);
        txtHost.addFocusListener(proxyChangeListener);

        lblPort = new JLabel(MessageStrings.PROXY_PORT);
        spinPort = new JIntegerSpinner(8080, 0, 65535, 1);
        spinPort.setEnabled(false);
        spinPort.addChangeListener(proxyChangeListener);

        lblProxy = new JLabel(MessageStrings.PROXY_TOGGLE);
        chkProxy = new JCheckBox();
        chkProxy.setSelected(false);
        chkProxy.addActionListener(new ProxyToggleListener());

        lblParallel = new JLabel(MessageStrings.PARALLEL_PULLS);
        chkParallel = new JCheckBox();
        chkParallel.setSelected(BriefcasePreferences.getBriefcaseParallelPullsProperty());
        chkParallel.addActionListener(new ParallelPullToggleListener());

        lblTrackingConsent = new JLabel(MessageStrings.TRACKING_CONSENT);
        chkTrackingConsent = new JCheckBox();
        chkTrackingConsent.setSelected(BriefcasePreferences.getBriefcaseTrackingConsentProperty());
        chkTrackingConsent.addActionListener(new TrackingConsentToggleListener());

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
          groupLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(
              groupLayout.createParallelGroup(Alignment.TRAILING)
                .addComponent(chkProxy)
                .addComponent(chkParallel)
                .addComponent(chkTrackingConsent))
            .addGroup(
              groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(
                  groupLayout.createSequentialGroup()
                    .addComponent(lblBriefcaseDirectory)
                    .addComponent(txtBriefcaseDir)
                    .addComponent(btnChoose))
                .addComponent(lblProxy)
                .addGroup(
                  groupLayout.createSequentialGroup()
                    .addGroup(
                       groupLayout.createParallelGroup(Alignment.LEADING)
                         .addComponent(lblHost)
                         .addComponent(lblPort))
                    .addGroup(
                      groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(txtHost, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(spinPort, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
                      .addComponent(lblParallel)
                      .addComponent(lblTrackingConsent))
            .addContainerGap()
        );
        groupLayout.setVerticalGroup(
          groupLayout.createSequentialGroup()
              .addContainerGap()
              .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(txtBriefcaseDir)
                .addComponent(btnChoose)
                .addComponent(lblBriefcaseDirectory))
              .addPreferredGap(ComponentPlacement.RELATED)
              .addGroup(groupLayout.createParallelGroup(Alignment.CENTER)
                .addComponent(chkProxy)
                .addComponent(lblProxy))
              .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(lblHost)
                .addComponent(txtHost))
              .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(lblPort)
                .addComponent(spinPort))
              .addPreferredGap(ComponentPlacement.RELATED)
              .addGroup(groupLayout.createParallelGroup(Alignment.CENTER)
                .addComponent(lblParallel)
                .addComponent(chkParallel))
              .addGroup(groupLayout.createParallelGroup(Alignment.CENTER)
                .addComponent(lblTrackingConsent)
                .addComponent(chkTrackingConsent))
              .addContainerGap()
        );

        setLayout(groupLayout);

        navOrder.add(lblBriefcaseDirectory);
        navOrder.add(txtBriefcaseDir);
        navOrder.add(btnChoose);
        
        setCurrentProxySettings();
    }
    
    private void setCurrentProxySettings() {
      HttpHost currentProxy = BriefcasePreferences.getBriefCaseProxyConnection();
      if (currentProxy != null) {
          chkProxy.setSelected(true);
          txtHost.setText(currentProxy.getHostName());
          txtHost.setEnabled(true);
          spinPort.setValue(currentProxy.getPort());
          spinPort.setEnabled(true);
      } else {
        txtHost.setText("127.0.0.1");
      }
    }

    public ArrayList<Component> getTraversalOrdering() {
        return navOrder;
    }

    public JTextField getTxtBriefcaseDir() {
        return txtBriefcaseDir;
    }

    class FolderActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // briefcase...
            parentWindow.establishBriefcaseStorageLocation(true);
        }

    }
    
    private void updateProxySettings() {
        BriefcasePreferences.setBriefcaseProxyProperty(new HttpHost(txtHost.getText(), (int)spinPort.getValue()));
    }

    class ProxyToggleListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == chkProxy) {
                if (chkProxy.isSelected()) {
                    txtHost.setEnabled(true);
                    spinPort.setEnabled(true);
                    if (!StringUtils.isNotEmptyNotNull(txtHost.getText())) {
                      txtHost.setText("127.0.0.1");
                    }
                    updateProxySettings();
                } else {
                    txtHost.setEnabled(false);
                    spinPort.setEnabled(false);
                    BriefcasePreferences.setBriefcaseProxyProperty(null);
                }
            }
        }

    }

    class ProxyChangeListener implements FocusListener, ChangeListener {

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void focusLost(FocusEvent e) {
            updateProxySettings();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
          updateProxySettings();
        }

    }

    private class ParallelPullToggleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == chkParallel) {
                BriefcasePreferences.setBriefcaseParallelPullsProperty(
                        !BriefcasePreferences.getBriefcaseParallelPullsProperty());
            }
        }
    }

    /**
     * This listener will pass the user's consent to being tracked onto the
     * application's preferences so it can be persisted and used elsewhere.
     */
    public class TrackingConsentToggleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == chkTrackingConsent) {
                BriefcasePreferences.setBriefcaseTrackingConsentProperty(chkTrackingConsent.isSelected());
            }
        }
    }
}