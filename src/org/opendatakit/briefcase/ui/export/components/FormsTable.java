package org.opendatakit.briefcase.ui.export.components;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.ui.export.ExportForms;

public class FormsTable {
  private final FormsTableView view;
  private final FormsTableViewModel viewModel;
  private final ExportForms forms;

  public FormsTable(ExportForms forms) {
    this.viewModel = new FormsTableViewModel(forms);
    this.view = new FormsTableView(viewModel);
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public void onChange(Runnable callback) {
    viewModel.onChange(callback);
  }

  public void selectAll() {
    forms.selectAll();
    viewModel.refresh();
  }

  public void clearAll() {
    forms.clearAll();
    viewModel.refresh();
  }

  public void refresh() {
    view.refresh();
  }

  public FormsTableView getView() {
    return view;
  }

  public void enable() {
    view.setEnabled(true);
  }

  public void disable() {
    view.setEnabled(false);
  }

  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void onExportProgressEvent(ExportProgressEvent event) {
    forms.appendStatus(event.getFormDefinition(), event.getText(), false);
    view.refresh();
  }

  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void onExportFailedEvent(ExportFailedEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Failed.", false);
    view.refresh();
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void onExportSucceededEvent(ExportSucceededEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Succeeded.", true);
    view.refresh();
  }

  @EventSubscriber(eventClass = ExportSucceededWithErrorsEvent.class)
  public void onExportSucceededWithErrorsEvent(ExportSucceededWithErrorsEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Succeeded, but with errors.", true);
    view.refresh();
  }
}
