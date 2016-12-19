package org.opendatakit.briefcase.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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

    public SettingsPanel(MainBriefcaseWindow parentWindow) {
        this.parentWindow = parentWindow;
        lblBriefcaseDirectory = new JLabel(MessageStrings.BRIEFCASE_STORAGE_LOCATION);

        txtBriefcaseDir = new JTextField();
        txtBriefcaseDir.setFocusable(false);
        txtBriefcaseDir.setEditable(false);
        txtBriefcaseDir.setColumns(10);

        btnChoose = new JButton("Change...");
        btnChoose.addActionListener(new FolderActionListener());

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createSequentialGroup()
                                        .addComponent(lblBriefcaseDirectory)
                                        .addGap(18)
                                        .addComponent(txtBriefcaseDir, GroupLayout.DEFAULT_SIZE, 362,
                                                Short.MAX_VALUE).addGap(18).addComponent(btnChoose))
                        .addContainerGap()));

        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtBriefcaseDir, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnChoose).addComponent(lblBriefcaseDirectory))
                        .addContainerGap()));

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
}


