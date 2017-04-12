package org.opendatakit.briefcase.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ProxyConnection;

/**
 *
 */
public class SettingsPanel extends JPanel{

    public static final String TAB_NAME = "Settings";

    public static int TAB_POSITION = -1;

    private JLabel lblBriefcaseDirectory;
    private JTextField txtBriefcaseDir;
    private JButton btnChoose;
    private MainBriefcaseWindow parentWindow;

    private ArrayList<Component> navOrder = new ArrayList<Component>();
    private JLabel lblSchema;
    private JLabel lblHost;
    private JTextField txtHost;
    private JLabel lblPort;
    private JTextField txtPort;
    private JButton btnSave;
    private JComboBox schemaComboBox;
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
                
        lblHost = new JLabel(MessageStrings.PROXY_HOST);
        txtHost = new JTextField();
        txtHost.setColumns(10);
        
        lblPort = new JLabel(MessageStrings.PROXY_PORT);
        txtPort = new JTextField();
        txtPort.setColumns(10);
        
        lblSchema = new JLabel(MessageStrings.PROXY_SCHEMA);
        String[] petStrings = { "No Proxy", "http", "https"};
        schemaComboBox = new JComboBox(petStrings);
        schemaComboBox.addActionListener(new ProxyTypeChangeListener());
        
        btnSave = new JButton("Save");
        btnSave.addActionListener(new ProxyConnectionCreateListener());
        
        lblGeneralSettings = new JLabel(MessageStrings.GENERAL_SETTINGS_STRING);
        lblProxySettings = new JLabel(MessageStrings.PROXY_SETTINGS_STRING);

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
        	groupLayout.createParallelGroup(Alignment.LEADING)
        		.addGroup(groupLayout.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        				.addGroup(groupLayout.createSequentialGroup()
        					.addComponent(lblSchema)
        					.addGap(4)
        					.addComponent(schemaComboBox, 0, 64, Short.MAX_VALUE)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(lblHost)
        					.addGap(4)
        					.addComponent(txtHost, GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(lblPort)
        					.addGap(3)
        					.addComponent(txtPort, GroupLayout.DEFAULT_SIZE, 39, Short.MAX_VALUE)
        					.addPreferredGap(ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
        					.addComponent(btnSave, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        				.addComponent(lblSchema)
        				.addComponent(schemaComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(txtHost, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(lblHost)
        				.addComponent(txtPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(lblPort)
        				.addComponent(btnSave))
        			.addGap(314))
        );

        setLayout(groupLayout);

        navOrder.add(lblBriefcaseDirectory);
        navOrder.add(txtBriefcaseDir);
        navOrder.add(btnChoose);
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
    
    class ProxyTypeChangeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
        	txtHost.setEnabled(true);
        	txtPort.setEnabled(true);
        }

    }
    
    class ProxyConnectionCreateListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
        	ProxyConnection.ProxyType proxyType;
            if (schemaComboBox.getSelectedIndex() == 0) {
            	proxyType = ProxyConnection.ProxyType.NO_PROXY;
            } else if (schemaComboBox.getSelectedIndex() == 1) {
            	proxyType = ProxyConnection.ProxyType.HTTP;
            } else {
            	proxyType = ProxyConnection.ProxyType.HTTPS;
            }
            try {
				BriefcasePreferences.setBriefcaseProxyProperty(new ProxyConnection(txtHost.getText(), Integer.parseInt(txtPort.getText()), proxyType));
				txtHost.setEnabled(false);
				txtPort.setEnabled(false);
			} catch (NumberFormatException e1) {
				JOptionPane.showMessageDialog(new JFrame(), MessageStrings.PORT_ERROR_MESSAGE, "Dialog",
				        JOptionPane.ERROR_MESSAGE);
				e1.printStackTrace();
			}
        }

    }
}


