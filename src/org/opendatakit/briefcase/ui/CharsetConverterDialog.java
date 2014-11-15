/*
 * Copyright (C) 2014 University of Washington
 *
 * Originally developed by Dobility, Inc. (as part of SurveyCTO)
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 *
 * Author Meletis Margaritis
 */
public class CharsetConverterDialog extends JDialog implements ActionListener {

  /**
   *
   */
  private static final long serialVersionUID = -5321396641987129789L;

  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final String DIALOG_TITLE = "Re-encode .csv as UTF-8";

  private static final String SELECT_FILE_LABEL = "1. Select the file to convert...";
  private static final String SELECT_FILE_BUTTON = "Browse...";
  private static final String BROWSE_COMMAND = "BrowseCommand";
  private static final String SELECT_SOURCE_ENCODING_LABEL = "2. Select source encoding...";
  private static final String PREVIEW_LABEL = "Preview:";
  private static final String REPLACE_EXISTING_FILE_LABEL = "3. Replace existing file?";
  private static final String CONVERT_COMMAND = "ConvertCommand";
  private static final String CONVERT_BUTTON_LABEL = "4. Convert";
  private static final String CANCEL_COMMAND = "CancelCommand";
  private static final String CANCEL_BUTTON_LABEL = "Cancel";

  private static final String DONE_MESSAGE = "The file has been successfully converted to UTF-8.";
  private static final String DONE_TITLE = "Done!";

  private static CharsetEntry[] commonCharsetEntries = new CharsetEntry[] {
       new CharsetEntry("Windows Latin 1", "Windows-1252"),
       new CharsetEntry("Mac OS Roman", "MacRoman"),
       new CharsetEntry("UNICODE", "UTF-8"),
       new CharsetEntry("UNICODE", "UTF-16"),
       new CharsetEntry("Windows Central European", "Windows-1250"),
       new CharsetEntry("Windows Greek", "Windows-1253"),
       new CharsetEntry("ISO 646", "US-ASCII")
  };

  private JTextField tfFile;
  private JList<CharsetEntry> listCharset;
  private JTextArea previewArea;
  private final JCheckBox cbOverride;
  private final JButton cancelButton;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          // Set System L&F
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

          CharsetConverterDialog window = new CharsetConverterDialog(new JFrame());
          window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  public void setVisible(boolean b) {
    if (b) {
      initialize();
    }
    super.setVisible(b);
  }

  /**
   * Create the dialog.
   */
  public CharsetConverterDialog(Window owner) {
    super(owner, ModalityType.DOCUMENT_MODAL);
    setTitle(DIALOG_TITLE);
    setBounds(100, 100, 600, 530);
    getContentPane().setLayout(new BorderLayout());
    JPanel contentPanel = new JPanel();
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    GridBagLayout gbl_contentPanel = new GridBagLayout();
    contentPanel.setLayout(gbl_contentPanel);
    {
      JLabel lblNewLabel = new JLabel(SELECT_FILE_LABEL);
      GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
      gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
      gbc_lblNewLabel.gridwidth = 2;
      gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
      gbc_lblNewLabel.gridx = 0;
      gbc_lblNewLabel.gridy = 0;
      contentPanel.add(lblNewLabel, gbc_lblNewLabel);
    }
    {
      tfFile = new JTextField();
      tfFile.setEditable(false);
      GridBagConstraints gbc_tfFile = new GridBagConstraints();
      gbc_tfFile.weightx = 1.0;
      gbc_tfFile.insets = new Insets(0, 0, 5, 5);
      gbc_tfFile.fill = GridBagConstraints.HORIZONTAL;
      gbc_tfFile.gridx = 0;
      gbc_tfFile.gridy = 1;
      contentPanel.add(tfFile, gbc_tfFile);
      tfFile.setColumns(10);
    }
    {
      JButton btnBrowse = new JButton(SELECT_FILE_BUTTON);
      btnBrowse.setActionCommand(BROWSE_COMMAND);
      btnBrowse.addActionListener(this);
      GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
      gbc_btnBrowse.insets = new Insets(0, 0, 5, 0);
      gbc_btnBrowse.gridx = 1;
      gbc_btnBrowse.gridy = 1;
      contentPanel.add(btnBrowse, gbc_btnBrowse);
    }
    {
      JLabel lblEncoding = new JLabel(SELECT_SOURCE_ENCODING_LABEL);
      GridBagConstraints gbc_lblEncoding = new GridBagConstraints();
      gbc_lblEncoding.anchor = GridBagConstraints.WEST;
      gbc_lblEncoding.insets = new Insets(0, 0, 5, 5);
      gbc_lblEncoding.gridx = 0;
      gbc_lblEncoding.gridy = 2;
      contentPanel.add(lblEncoding, gbc_lblEncoding);
    }
    {
      listCharset = new JList<CharsetEntry>();
      listCharset.setVisibleRowCount(7);
      listCharset.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      GridBagConstraints gbc_cbCharset = new GridBagConstraints();
      gbc_cbCharset.gridwidth = 2;
      gbc_cbCharset.insets = new Insets(0, 0, 5, 0);
      gbc_cbCharset.fill = GridBagConstraints.BOTH;
      gbc_cbCharset.gridx = 0;
      gbc_cbCharset.gridy = 3;
      JScrollPane listScrollPane = new JScrollPane(listCharset);
      contentPanel.add(listScrollPane, gbc_cbCharset);
    }
    {
      JLabel lblPreview = new JLabel(PREVIEW_LABEL);
      GridBagConstraints gbc_lblPreview = new GridBagConstraints();
      gbc_lblPreview.anchor = GridBagConstraints.WEST;
      gbc_lblPreview.insets = new Insets(0, 0, 5, 5);
      gbc_lblPreview.gridx = 0;
      gbc_lblPreview.gridy = 4;
      contentPanel.add(lblPreview, gbc_lblPreview);
    }
    {
      JScrollPane scrollPane = new JScrollPane();
      GridBagConstraints gbc_scrollPane = new GridBagConstraints();
      gbc_scrollPane.weighty = 1.0;
      gbc_scrollPane.weightx = 1.0;
      gbc_scrollPane.gridwidth = 2;
      gbc_scrollPane.fill = GridBagConstraints.BOTH;
      gbc_scrollPane.gridx = 0;
      gbc_scrollPane.gridy = 5;
      contentPanel.add(scrollPane, gbc_scrollPane);
      {
        previewArea = new JTextArea();
        previewArea.setLineWrap(true);
        previewArea.setRows(10);
        previewArea.setFont(UIManager.getDefaults().getFont("Label.font").deriveFont(Font.PLAIN));
        previewArea.setEditable(false);
        scrollPane.setViewportView(previewArea);
      }
    }
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        cbOverride = new JCheckBox(REPLACE_EXISTING_FILE_LABEL);
        cbOverride.setSelected(false);
        buttonPane.add(cbOverride);
      }
      {
        JButton okButton = new JButton(CONVERT_BUTTON_LABEL);
        okButton.setActionCommand(CONVERT_COMMAND);
        okButton.addActionListener(this);
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
      {
        cancelButton = new JButton(CANCEL_BUTTON_LABEL);
        cancelButton.setActionCommand(CANCEL_COMMAND);
        cancelButton.addActionListener(this);
        buttonPane.add(cancelButton);
      }
    }
  }

  protected void initialize() {
    DefaultListModel<CharsetEntry> defaultListModel = new DefaultListModel<CharsetEntry>();

    for (CharsetEntry commonCharsetEntry : commonCharsetEntries) {
      try {
        if (Charset.isSupported(commonCharsetEntry.getCharsetName())) {
          defaultListModel.addElement(commonCharsetEntry);
        }
      } catch (IllegalCharsetNameException e) {
        // just ignore it. It will happen for "Mac OS Roman" under Windows
      }
    }

    SortedMap<String,Charset> charsetSortedMap = Charset.availableCharsets();
    for (Map.Entry<String, Charset> charsetMapEntry : charsetSortedMap.entrySet()) {
      CharsetEntry charsetEntry = new CharsetEntry(charsetMapEntry.getValue().displayName(), charsetMapEntry.getKey());
      if (!defaultListModel.contains(charsetEntry)) {
        defaultListModel.addElement(charsetEntry);
      }
    }

    listCharset.setModel(defaultListModel);

    if (defaultListModel.size() > 0) {
      listCharset.setSelectedIndex(0);

      listCharset.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          updatePreview();
        }
      });
    } else {
      JOptionPane.showMessageDialog(this,
              "It appears that your installed Java Runtime Environment does not support any charset encodings!",
              "Error!", JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    String actionCommand = e.getActionCommand();

    if (BROWSE_COMMAND.equals(actionCommand)) {
      WrappedFileChooser dlg = new WrappedFileChooser(window, new FileChooser(true, "Select the file to convert...", "Select"));
      String path = getFilePath();
      if (path != null && path.trim().length() != 0) {
        dlg.setSelectedFile(new File(path.trim()));
      }
      int retVal = dlg.showDialog();
      if (retVal == JFileChooser.APPROVE_OPTION) {
        if (dlg.getSelectedFile() != null) {
          String selectedPath = dlg.getSelectedFile().getAbsolutePath();
          tfFile.setText(selectedPath);

          updatePreview();
        }
      }
    } else if (CONVERT_COMMAND.equals(actionCommand)) {
      String filePath = getFilePath();
      if (filePath == null || filePath.trim().length() == 0) {
        return;
      }

      File destinationPath;
      if (cbOverride.isSelected()) {
        destinationPath = new File(filePath);
      } else {
        WrappedFileChooser dlg = new WrappedFileChooser(window, new FileChooser(false, "Select the file to convert to...", "Save"));
        dlg.setSelectedFile(new File(filePath));
        int retVal = dlg.showDialog();
        if (retVal == JFileChooser.APPROVE_OPTION && dlg.getSelectedFile() != null) {
          destinationPath = dlg.getSelectedFile();
        } else {
          return;
        }
      }


      File file = new File(filePath);
      try {
        List<String> lines = FileUtils.readLines(file, getCharsetName());
        FileUtils.writeLines(destinationPath, "UTF-8", lines, LINE_SEPARATOR);

        JOptionPane.showMessageDialog(this,
                DONE_MESSAGE,
                DONE_TITLE, JOptionPane.INFORMATION_MESSAGE);

        cancelButton.setText("Close");

      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
                ex.getMessage(),
                "Error converting file...", JOptionPane.ERROR_MESSAGE);
      }
    } else if (CANCEL_COMMAND.equals(actionCommand)) {
      closeDialog();
    }
  }

  private synchronized void updatePreview() {
    final String filePath = getFilePath();
    if (filePath == null || filePath.trim().length() == 0) {
      return;
    }

    final Window pleaseWaitWindow = ODKOptionPane.showMessageDialog(this, "Creating preview, please wait...");

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        File file = new File(filePath);
        BufferedReader bufferedReader = null;
        try {
          bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), getCharsetName()));
          List<String> lines = new ArrayList<String>();
          int N = 100;
          String line;
          int c = 0;
          while ((line = bufferedReader.readLine()) != null && c < N) {
            lines.add(line);
          }

          previewArea.setText(join(lines, LINE_SEPARATOR));
          previewArea.setCaretPosition(0);

        } catch (Exception ex) {
          ex.printStackTrace();

          JOptionPane.showMessageDialog(CharsetConverterDialog.this,
                  ex.getMessage(),
                  "Error reading file...", JOptionPane.ERROR_MESSAGE);
        } finally {
          IOUtils.closeQuietly(bufferedReader);

          pleaseWaitWindow.setVisible(false);
          pleaseWaitWindow.dispose();
        }
      }
    });
  }

  private String join(List<String> lines, String lineSeparator) {
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      if (sb.length() > 0) {
        sb.append(lineSeparator);
      }
      sb.append(line);
    }
    return sb.toString();
  }

  private String getCharsetName() {
    return listCharset.getSelectedValue().getCharsetName();
  }

  private String getFilePath() {
    return tfFile.getText();
  }

  private void closeDialog() {
    setVisible(false);

    dispose();
  }

  static class CharsetEntry {

    private final String displayName;
    private final String charsetName;

    CharsetEntry(String charsetName) {
      this(charsetName, charsetName);
    }

    CharsetEntry(String displayName, String charsetName) {
      this.displayName = displayName;
      this.charsetName = charsetName;
    }

    String getDisplayName() {
      return displayName;
    }

    String getCharsetName() {
      return charsetName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CharsetEntry that = (CharsetEntry) o;

      return charsetName.equals(that.charsetName);
    }

    @Override
    public int hashCode() {
      return charsetName.hashCode();
    }

    @Override
    public String toString() {
      if (getDisplayName().equals(getCharsetName())) {
        return getDisplayName();
      } else {
        return getDisplayName() + " (" + getCharsetName() + ")";
      }
    }
  }

  class FileChooser extends AbstractFileChooser {

    /**
     * 
     */
    private static final long serialVersionUID = 6458668203143472878L;

    public FileChooser(boolean open, String title, String buttonText) {
      super();
      setFileSelectionMode(JFileChooser.FILES_ONLY);
      setDialogType(open ? JFileChooser.OPEN_DIALOG : SAVE_DIALOG);
      setDialogTitle(title);
      setApproveButtonText(buttonText);
    }

    @Override
    public boolean testAndMessageBadFolder(File f, Container parentWindow) {
      return true;
    }
  }
}
