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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatus.TransferType;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsSucceededEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.opendatakit.briefcase.util.ServerFetcher;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormTransferTable extends JTable {

  /**
	 * 
	 */
  private static final long serialVersionUID = 8511088963758308085L;
  
  public class JTableButtonRenderer implements TableCellRenderer {
    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JButton button = (JButton)value;
      button.setOpaque(true);
      if (isSelected) {
        button.setBackground(table.getSelectionBackground());
      } else {
        button.setBackground(table.getBackground());
      }
      return button;
    }
  }
  
  public class JTableButtonMouseListener implements MouseListener {
    public JTableButtonMouseListener() {
    }
  
    @Override public void mouseClicked(MouseEvent e) {
      int column = FormTransferTable.this.getColumnModel().getColumnIndexAtX(e.getX());
      int row    = e.getY()/FormTransferTable.this.getRowHeight(); 
  
      if (row < FormTransferTable.this.getRowCount() && row >= 0 &&
          column < FormTransferTable.this.getColumnCount() && column >= 0) {
        Object value = FormTransferTable.this.getValueAt(row, column);
        if (value instanceof JButton) {
          ((JButton)value).doClick();
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
  }
  
  public static class DetailButton extends JButton implements ActionListener {
    
    /**
     * 
     */
    private static final long serialVersionUID = -5106458166776020642L;
    private static final Log logger = LogFactory.getLog( DetailButton.class);
    public static final String LABEL = "Details...";
    
    final FormStatus status;
    
    DetailButton(FormStatus status) {
      super(LABEL);
      this.status = status;
      this.addActionListener(this);
      logger.debug("creating details button");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        final String history = status.getStatusHistory();
        setEnabled(false);
        ScrollingStatusListDialog.showDialog(JOptionPane.getFrameForComponent(this), status.getFormDefinition(), history);
      } finally {
        setEnabled(true);
      }
    }
  }

  static class FormTransferTableModel extends AbstractTableModel {
    /**
		 * 
		 */
    private static final long serialVersionUID = 7108326237416622721L;

    public static final int BUTTON_COLUMN = 3;     

    private static final Log logger = LogFactory.getLog( FormTransferTableModel.class);
    
    final String[] columnNames;
    final JButton btnSelectOrClearAllForms;
    final TransferType transferType;
    final JButton btnTransfer;
    List<FormStatus> formStatuses = new ArrayList<FormStatus>();
    private Map<FormStatus, DetailButton> buttonMap = new HashMap<FormStatus, DetailButton>();
    
    public FormTransferTableModel(JButton btnSelectOrClearAllForms, TransferType transferType, JButton btnTransfer) {
      super();
      AnnotationProcessor.process(this);// if not using AOP

      this.columnNames = new String[]{ "Selected", "Form Name", ((transferType == TransferType.UPLOAD) ?
          PushTransferPanel.TAB_NAME : PullTransferPanel.TAB_NAME) + " Status", DetailButton.LABEL };
      
      this.btnSelectOrClearAllForms = btnSelectOrClearAllForms;
      this.transferType = transferType;
      this.btnTransfer = btnTransfer;
      // initially the transfer button is disabled.
      btnTransfer.setEnabled(false);
      btnSelectOrClearAllForms.setText("Clear all");

      btnSelectOrClearAllForms.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          boolean anyDeselected = false;
          for (FormStatus f : formStatuses) {
            anyDeselected = anyDeselected || !f.isSelected();
          }

          // clear-all if all were selected, otherwise, select-all...
          for (FormStatus f : formStatuses) {
            f.setSelected(anyDeselected);
          }
          FormTransferTableModel.this.updateButtonsAfterStatusChange();
          FormTransferTableModel.this.fireTableDataChanged();
        }
      });
    }

    private void updateButtonsAfterStatusChange() {
      boolean anyDeselected = false;
      boolean anySelected = false;
      for (FormStatus f : formStatuses) {
        anyDeselected = anyDeselected || !f.isSelected();
        anySelected = anySelected || f.isSelected();
      }
      btnTransfer.setEnabled(anySelected);
      if (!anyDeselected) {
        FormTransferTableModel.this.btnSelectOrClearAllForms.setText("Clear all");
      } else {
        FormTransferTableModel.this.btnSelectOrClearAllForms.setText("Select all");
      }
    }

    public TransferType getTransferType() {
      return transferType;
    }
    
    public void setFormStatusList(List<FormStatus> statuses) {
      formStatuses = statuses;
      updateButtonsAfterStatusChange();
      fireTableDataChanged();
    }

    public List<FormStatus> getSelectedForms() {
      List<FormStatus> selected = new ArrayList<FormStatus>();
      for (FormStatus s : formStatuses) {
        if (s.isSelected()) {
          selected.add(s);
        }
      }
      return selected;
    }
    
    public List<FormStatus> getSelectedNonActiveForms() {
        List<FormStatus> selected = new ArrayList<FormStatus>();
        for (FormStatus s : formStatuses) {
      	  if (s.isSelected() && s.getStatusString().isEmpty()) {
            selected.add(s);
          }
        }
        return selected;
      }

    @Override
    public int getRowCount() {
      return formStatuses.size();
    }

    public String getColumnName(int col) {
      return columnNames[col];
    }

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    /*
     * JTable uses this method to determine the default renderer/ editor for
     * each cell. If we didn't implement this method, then the boolean column would
     * contain text ("true"/"false"), rather than a check box.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class getColumnClass(int c) {
      switch (c) {
      case 0:
        return Boolean.class;
      case 1:
        return String.class;
      case 2:
        return String.class;
      case 3:
        return JButton.class;
      default:
        throw new IllegalStateException("unexpected column choice");
      }
    }

    public boolean isCellEditable(int row, int col) {
	  // While the table is in active transfer state, you cannot de-select
	  // already selected forms
	  if (!formStatuses.get(row).getStatusString().isEmpty() && !isTransferTerminated(formStatuses.get(row).getStatusString())) {
		return false;
	  }
      return col == 0; // only the checkbox...
    }

    private boolean isTransferTerminated(String statusString) {
      return statusString.equals(ServerFetcher.SUCCESS_STATUS) || statusString.equals(ServerFetcher.FAILED_STATUS) ;
    }

	public void setValueAt(Object value, int row, int col) {
      FormStatus status = formStatuses.get(row);
      switch (col) {
      case 0:
        status.setSelected((Boolean) value);
        updateButtonsAfterStatusChange();
        break;
      case 2:
        status.setStatusString((String) value, true);
        break;
      case 3:
        logger.warn("attempting to set button value");
        break;
      default:
        throw new IllegalStateException("unexpected column choice");
      }
      fireTableCellUpdated(row, col);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      FormStatus status = formStatuses.get(rowIndex);
      switch (columnIndex) {
      case 0:
        return status.isSelected();
      case 1:
        return status.getFormName();
      case 2:
        return status.getStatusString();
      case 3:
        DetailButton button = buttonMap.get(status);
        if ( button == null ) {
          button = new DetailButton(status);
          buttonMap.put(status, button);
        }
        return button;
      default:
        throw new IllegalStateException("unexpected column choice");
      }
    }

    @EventSubscriber(eventClass = FormStatusEvent.class)
    public void fireStatusChange(FormStatusEvent fse) {
      FormStatus fs = fse.getStatus();
      if ( fs.getTransferType() == transferType ) {
        for (int rowIndex = 0; rowIndex < formStatuses.size(); ++rowIndex) {
          FormStatus status = formStatuses.get(rowIndex);
          if (status.equals(fs)) {
            fireTableRowsUpdated(rowIndex, rowIndex);
            return;
          }
        }
      }
    }

  }

  public FormTransferTable(JButton btnSelectOrClearAllForms, FormStatus.TransferType transferType, JButton btnTransfer) {
    super(new FormTransferTableModel(btnSelectOrClearAllForms, transferType, btnTransfer));
    AnnotationProcessor.process(this);// if not using AOP
    // set the button column renderer to a custom renderer
    getColumn(getColumnName(FormTransferTableModel.BUTTON_COLUMN)).setCellRenderer(new JTableButtonRenderer());
    addMouseListener(new JTableButtonMouseListener());
    
    TableColumnModel columns = this.getColumnModel();
    // determine width of "Selected" column header
    TableCellRenderer headerRenderer = this.getTableHeader().getDefaultRenderer();
    Component comp = headerRenderer.getTableCellRendererComponent(null, columns.getColumn(0)
        .getHeaderValue(), false, false, 0, 0);
    int headerWidth = comp.getPreferredSize().width;
    columns.getColumn(0).setMinWidth(headerWidth);
    columns.getColumn(0).setMaxWidth(headerWidth);
    columns.getColumn(0).setPreferredWidth(headerWidth);
    
    // create a detail button (that we'll throw away)
    // so we can get the needed column and row dimensions.
    comp = new DetailButton(null);
    int buttonWidth = comp.getPreferredSize().width;
    int buttonHeight = comp.getPreferredSize().height;
    columns.getColumn(FormTransferTableModel.BUTTON_COLUMN).setMinWidth(buttonWidth);
    columns.getColumn(FormTransferTableModel.BUTTON_COLUMN).setMaxWidth(buttonWidth);
    columns.getColumn(FormTransferTableModel.BUTTON_COLUMN).setPreferredWidth(buttonWidth);
    
    // set the row height to be big enough to include a button and have space above and below it
    setRowHeight(buttonHeight); // btn used is arbitrary...

    // and scale the others to be wider...
    columns.getColumn(1).setPreferredWidth(5 * headerWidth);
    columns.getColumn(2).setPreferredWidth(5 * headerWidth);
    this.setFillsViewportHeight(true);

    // set the default sortingOrder to the Form Name column
    TableRowSorter sorter = new FormNameColumnTableRowSorter(getModel());
    sorter.sort();
  }

  private class FormNameColumnTableRowSorter extends TableRowSorter {

    private final List<RowSorter.SortKey> sortKeys = new ArrayList<>(1);

    public FormNameColumnTableRowSorter(TableModel model) {
      super(model);
      this.setSortsOnUpdates(true);
      sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
      setSortKeys(sortKeys);
      setRowSorter(this);
    }
  }

  public void setFormStatusList(List<FormStatus> statuses) {
    FormTransferTableModel model = (FormTransferTableModel) this.dataModel;
    model.setFormStatusList(statuses);
  }

  @EventSubscriber(eventClass = RetrieveAvailableFormsSucceededEvent.class)
  public void formsAvailableFromServer(RetrieveAvailableFormsSucceededEvent event) {
    FormTransferTableModel model = (FormTransferTableModel) this.dataModel;
    if ( event.getTransferType() == model.getTransferType() ) {
      setFormStatusList(event.getFormsToTransfer());
    }
  }

  public List<FormStatus> getSelectedForms() {
    FormTransferTableModel model = (FormTransferTableModel) this.dataModel;
    return model.getSelectedForms();
  }
  
  public List<FormStatus> getSelectedNonActiveForms() {
    FormTransferTableModel model = (FormTransferTableModel) this.dataModel;
    return model.getSelectedNonActiveForms();
  }
  
}
