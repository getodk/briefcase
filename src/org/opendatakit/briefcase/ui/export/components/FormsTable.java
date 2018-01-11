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

  FormsTable(ExportForms forms, FormsTableView view, FormsTableViewModel viewModel) {
    this.viewModel = viewModel;
    this.view = view;
    this.forms = forms;
    AnnotationProcessor.process(this);
  }

  public static FormsTable from(ExportForms forms) {
    FormsTableViewModel viewModel = new FormsTableViewModel(forms);
    return new FormsTable(forms, new FormsTableView(viewModel), viewModel);
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
    viewModel.refresh();
  }

  public FormsTableView getView() {
    return view;
  }

  public void enable() {
    viewModel.enable();
  }

  public void disable() {
    viewModel.disable();
  }

  @EventSubscriber(eventClass = ExportProgressEvent.class)
  public void onExportProgressEvent(ExportProgressEvent event) {
    forms.appendStatus(event.getFormDefinition(), event.getText(), false);
    viewModel.refresh();
  }

  @EventSubscriber(eventClass = ExportFailedEvent.class)
  public void onExportFailedEvent(ExportFailedEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Failed.", false);
    viewModel.refresh();
  }

  @EventSubscriber(eventClass = ExportSucceededEvent.class)
  public void onExportSucceededEvent(ExportSucceededEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Succeeded.", true);
    viewModel.refresh();
  }

  @EventSubscriber(eventClass = ExportSucceededWithErrorsEvent.class)
  public void onExportSucceededWithErrorsEvent(ExportSucceededWithErrorsEvent event) {
    forms.appendStatus(event.getFormDefinition(), "Succeeded, but with errors.", true);
    viewModel.refresh();
  }
}
