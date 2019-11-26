package org.opendatakit.briefcase.pull;

import static org.opendatakit.briefcase.model.form.FormMetadataCommands.updateAsPulled;
import static org.opendatakit.briefcase.reused.job.Job.run;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.util.TransferFromODK;

public class PullFromCollect {
  public static JobsRunner pullForms(FormMetadataPort formMetadataPort, TransferForms forms, Path briefcaseDir, Path collectDir, Consumer<PullEvent> onEvent) {
    return JobsRunner.launchAsync(forms.map(form -> {
      TransferFromODK action = new TransferFromODK(briefcaseDir, collectDir.toFile(), new TerminationFuture(), TransferForms.of(form));
      return run(jobStatus -> {
        try {
          boolean success = action.doAction();
          if (success) {
            onEvent.accept(PullEvent.Success.of(form));
            formMetadataPort.execute(updateAsPulled(FormKey.from(form), briefcaseDir, form.getFormDir(briefcaseDir)));
          } // TODO Originally there was no explicit side effect on non successful individual pulls. We might want to give some feedback
        } catch (Exception e) {
          // This will lift any checked exception thrown by the underlying code
          // into a BriefcaseException that is managed by the error management
          // flow driven by the Launcher class
          throw new BriefcaseException("Failed to pull form (legacy)", e);
        }
      });
    })).onComplete(() -> onEvent.accept(new PullEvent.PullComplete()));
  }
}
