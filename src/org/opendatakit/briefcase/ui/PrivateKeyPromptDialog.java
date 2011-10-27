/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

public class PrivateKeyPromptDialog extends JDialog implements ActionListener {

	/**
	   * 
	   */
	private static final long serialVersionUID = 5451425417966734421L;
	private final JPanel contentPanel = new JPanel();
	private JTextArea txtrPrivatekey;
	private JButton btnOk;
	private JButton btnCancel;

	private String privateKey = null;
	
	public PrivateKeyPromptDialog(String label) {
		super(null, "Enter the private key for this form",
				ModalityType.DOCUMENT_MODAL);
		setBounds(100, 100, 461, 291);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
	    
	    JLabel lblFormlabel = new JLabel(label);
	    
	    txtrPrivatekey = new JTextArea();
	    txtrPrivatekey.setText("");
	    
	    btnOk = new JButton("OK");
	    btnOk.setActionCommand("OK");
	    btnOk.addActionListener(this);
	    
	    btnCancel = new JButton("Cancel");
	    btnCancel.setActionCommand("Cancel");
	    btnCancel.addActionListener(this);
		
	    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
	    gl_contentPanel.setHorizontalGroup(
	    	gl_contentPanel.createSequentialGroup()
	    			.addContainerGap()
	    			.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
	    					.addComponent(lblFormlabel, GroupLayout.PREFERRED_SIZE, 372, GroupLayout.PREFERRED_SIZE)
	    					.addComponent(txtrPrivatekey, GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
	    					.addGroup(Alignment.TRAILING, gl_contentPanel.createSequentialGroup()
		    					.addComponent(btnOk)
		    					.addPreferredGap(ComponentPlacement.RELATED)
		    					.addComponent(btnCancel)))
	    			.addContainerGap()
	    );
	    gl_contentPanel.setVerticalGroup(
	    		gl_contentPanel.createSequentialGroup()
	    			.addContainerGap()
	    			.addComponent(lblFormlabel)
	    			.addPreferredGap(ComponentPlacement.RELATED)
	    			.addComponent(txtrPrivatekey, GroupLayout.PREFERRED_SIZE, 129, Short.MAX_VALUE)
	    			.addPreferredGap(ComponentPlacement.RELATED)
	    			.addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
	    				.addComponent(btnOk)
	    				.addComponent(btnCancel))
	    			.addContainerGap()
	    );
	    
	    contentPanel.setLayout(gl_contentPanel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		privateKey = null;
		if ( e.getActionCommand().equals("OK") ) {
			privateKey = txtrPrivatekey.getText();
		}
		this.setVisible(false);
	}
	
	public String getPrivateKey() {
		return privateKey;
	}
}
