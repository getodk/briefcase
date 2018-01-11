package org.opendatakit.briefcase.ui.export.components;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.bushe.swing.event.EventBus;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.ui.export.ExportForms;
import org.opendatakit.briefcase.util.BadFormDefinition;

public class FormsTableTest {
  @Test
  public void can_select_all_forms() {
    ExportForms forms = new ExportForms(IntStream.range(0, 10).boxed().map(this::uncheckedFormStatusFactory).collect(toList()));
    TestFormsTableViewModel viewModel = new TestFormsTableViewModel(forms);
    FormsTable formsTable = new FormsTable(forms, new TestFormsTableView(viewModel), viewModel);

    assertThat(forms.noneSelected(), Matchers.is(true));

    formsTable.selectAll();

    assertThat(forms.allSelected(), Matchers.is(true));
  }

  @Test
  public void can_clear_selection_of_forms() {
    ExportForms forms = new ExportForms(IntStream.range(0, 10).boxed().map(this::uncheckedFormStatusFactory).collect(toList()));
    TestFormsTableViewModel viewModel = new TestFormsTableViewModel(forms);
    FormsTable formsTable = new FormsTable(forms, new TestFormsTableView(viewModel), viewModel);
    formsTable.selectAll();

    formsTable.clearAll();

    assertThat(forms.noneSelected(), Matchers.is(true));
  }

  @Test
  @Ignore
  public void appends_to_a_forms_status_history_when_export_events_are_sent() {
    FormStatus theForm = uncheckedFormStatusFactory(1);
    ExportForms forms = new ExportForms(Collections.singletonList(theForm));
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

  private FormStatus uncheckedFormStatusFactory(Integer n) {
    try {
      return new FormStatus(EXPORT, new TestFormDefinition(n));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private static class TestFormDefinition extends BriefcaseFormDefinition {
    private static AtomicInteger idSeq = new AtomicInteger(1);
    private static final File formDir;
    private static final File formFile;

    static {
      try {
        Path dir = Files.createTempDirectory("briefcase_test");
        Path sourceFile = Paths.get(TestFormDefinition.class.getResource("/basic.xml").toURI());
        Path file = dir.resolve("form.xml");
        Files.copy(sourceFile, file);
        formDir = dir.toFile();
        formFile = file.toFile();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    private final int id;

    private TestFormDefinition() throws BadFormDefinition {
      super(formDir, formFile);
      id = idSeq.getAndIncrement();
    }

    TestFormDefinition(int id) throws BadFormDefinition {
      super(formDir, formFile);
      this.id = id;
    }

    @Override
    public LocationType getFormLocation() {
      return LocationType.LOCAL;
    }

    @Override
    public String getFormName() {
      return "Form #" + id;
    }

    @Override
    public String getFormId() {
      return "" + id;
    }

    @Override
    public String getVersionString() {
      return "1";
    }
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