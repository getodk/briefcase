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
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.text.AbstractDocument;

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
    private JTextField txtPort;

    private JLabel lblGeneralSettings;
    private JLabel lblProxySettings;
    
    public SettingsPanel(MainBriefcaseWindow parentWindow) {
        this.parentWindow = parentWindow;
        lblBriefcaseDirectory = new JLabel(MessageStrings.BRIEFCASE_STORAGE_LOCATION);

        txtBriefcaseDir = new JTextField();
        txtBriefcaseDir.setFocusable(false);
        txtBriefcaseDir.setEditable(false);
        txtBriefcaseDir.setColumns(10);

        btnChoose = new JButton("Change...");
        btnChoose.addActionListener(new FolderActionListener());

        FocusListener proxyFocusListener = new ProxyFocusListener();

        lblHost = new JLabel(MessageStrings.PROXY_HOST);
        txtHost = new JTextField();
        txtHost.setEnabled(false);
        txtHost.setColumns(10);
        txtHost.addFocusListener(proxyFocusListener);
        
        lblPort = new JLabel(MessageStrings.PROXY_PORT);
        txtPort = new JTextField();
        txtPort.setEnabled(false);
        ((AbstractDocument)txtPort.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        txtPort.setColumns(10);
        txtPort.addFocusListener(proxyFocusListener);
        
        lblProxy = new JLabel(MessageStrings.PROXY_TOGGLE);
        chkProxy = new JCheckBox();
        chkProxy.setSelected(false);
        chkProxy.addActionListener(new ProxyToggleListener());
        
        lblGeneralSettings = new JLabel(MessageStrings.GENERAL_SETTINGS_STRING);
        lblProxySettings = new JLabel(MessageStrings.PROXY_SETTINGS_STRING);
        
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            	groupLayout.createParallelGroup(Alignment.LEADING)
            		.addGroup(groupLayout.createSequentialGroup()
            			.addContainerGap()
            			.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
            				.addGroup(groupLayout.createSequentialGroup()
            					.addComponent(lblProxy)
            					.addGap(4)
            					.addComponent(chkProxy, 0, 64, Short.MAX_VALUE)
            					.addPreferredGap(ComponentPlacement.RELATED)
            					.addComponent(lblHost)
            					.addGap(4)
            					.addComponent(txtHost, GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
            					.addPreferredGap(ComponentPlacement.RELATED)
            					.addComponent(lblPort)
            					.addGap(3)
            					.addComponent(txtPort, GroupLayout.DEFAULT_SIZE, 39, Short.MAX_VALUE)
            					.addPreferredGap(ComponentPlacement.RELATED, 14, Short.MAX_VALUE))
            				.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
            					.addComponent(lblBriefcaseDirectory)
            					.addGap(18)
            					.addComponent(txtBriefcaseDir, GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)
            					.addGap(18)
            					.addComponent(btnChoose))
            				.addComponent(lblGeneralSettings)
            				.addComponent(lblProxySettings, GroupLayout.PREFERRED_SIZE, 132, GroupLayout.PREFERRED_SIZE))
            			.addContainerGap())
            );
            groupLayout.setVerticalGroup(
            	groupLayout.createParallelGroup(Alignment.LEADING)
            		.addGroup(groupLayout.createSequentialGroup()
            			.addGap(18)
            			.addComponent(lblGeneralSettings)
            			.addPreferredGap(ComponentPlacement.RELATED)
            			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            				.addComponent(txtBriefcaseDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            				.addComponent(btnChoose)
            				.addComponent(lblBriefcaseDirectory))
            			.addGap(14)
            			.addComponent(lblProxySettings, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
            			.addPreferredGap(ComponentPlacement.RELATED)
            			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
            				.addComponent(lblProxy)
            				.addComponent(chkProxy, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            				.addComponent(txtHost, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            				.addComponent(lblHost)
            				.addComponent(txtPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            				.addComponent(lblPort))
            			.addGap(314))
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
		  txtPort.setText("" + currentProxy.getPort());
		  txtPort.setEnabled(true);
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
    	BriefcasePreferences.setBriefcaseProxyProperty(new HttpHost(txtHost.getText(), Integer.parseInt(txtPort.getText())));
    }

    class ProxyToggleListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
        	if (e.getSource() == chkProxy) {
        		if (chkProxy.isSelected()) {
        			txtHost.setEnabled(true);
        			txtPort.setEnabled(true);
        			if (!StringUtils.isNotEmptyNotNull(txtPort.getText())) {
        				txtPort.setText("80");
        			}
        			updateProxySettings();
        		} else {
        			txtHost.setEnabled(false);
        			txtPort.setEnabled(false);
        			BriefcasePreferences.setBriefcaseProxyProperty(null);
        		}
    		}
        }

    }

    class ProxyFocusListener implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {	
		}

		@Override
		public void focusLost(FocusEvent e) {
			updateProxySettings();
		}

    }

}


