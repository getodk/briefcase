package org.opendatakit.briefcase.ui.export.components;

import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.FormStatusBuilder.buildFormStatus;
import static org.opendatakit.briefcase.model.FormStatusBuilder.buildFormStatusList;

import java.util.Collections;
import java.util.HashMap;
import org.bushe.swing.event.EventBus;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportForms;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.FormStatus;

public class FormsTableUnitTest {
  @Test
  public void can_select_all_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    TestFormsTableViewModel viewModel = new TestFormsTableViewModel(forms);
    FormsTable formsTable = new FormsTable(forms, new TestFormsTableView(viewModel), viewModel);

    assertThat(forms.noneSelected(), Matchers.is(true));

    formsTable.selectAll();

    assertThat(forms.allSelected(), Matchers.is(true));
  }

  @Test
  public void can_clear_selection_of_forms() {
    ExportForms forms = new ExportForms(buildFormStatusList(10), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    TestFormsTableViewModel viewModel = new TestFormsTableViewModel(forms);
    FormsTable formsTable = new FormsTable(forms, new TestFormsTableView(viewModel), viewModel);
    formsTable.selectAll();

    formsTable.clearAll();

    assertThat(forms.noneSelected(), Matchers.is(true));
  }

  @Test
  @Ignore
  public void appends_to_a_forms_status_history_when_export_events_are_sent() {
    FormStatus theForm = buildFormStatus(1);
    ExportForms forms = new ExportForms(Collections.singletonList(theForm), ExportConfiguration.empty(), new HashMap<>(), new HashMap<>());
    TestFormsTableViewModel viewModel = new TestFormsTableViewModel(forms);
    new FormsTable(forms, new TestFormsTableView(viewModel), viewModel);

    // TODO Event publishing happens asynchronously. We have to work this test a little more to stop ignoring it
    EventBus.publish(new ExportProgressEvent("some progress", (BriefcaseFormDefinition) theForm.getFormDefinition()));
    EventBus.publish(new ExportFailedEvent((BriefcaseFormDefinition) theForm.getFormDefinition()));
    EventBus.publish(new ExportSucceededEvent((BriefcaseFormDefinition) theForm.getFormDefinition()));
    EventBus.publish(new ExportSucceededWithErrorsEvent((BriefcaseFormDefinition) theForm.getFormDefinition()));

    assertThat(theForm.getStatusHistory(), Matchers.containsString("some progress"));
    assertThat(theForm.getStatusHistory(), Matchers.containsString("Failed."));
    assertThat(theForm.getStatusHistory(), Matchers.containsString("Succeeded."));
    assertThat(theForm.getStatusHistory(), Matchers.containsString("Succeeded, but with errors."));
  }

  private class TestFormsTableView extends FormsTableView {
    TestFormsTableView(FormsTableViewModel model) {
      super(model);
    }
  }

  private class TestFormsTableViewModel extends FormsTableViewModel {
    TestFormsTableViewModel(ExportForms forms) {
      super(forms);
    }
  }
}