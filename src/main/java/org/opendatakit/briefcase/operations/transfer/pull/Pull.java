package org.opendatakit.briefcase.operations.transfer.pull;

import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.AGGREGATE;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.CENTRAL;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.COLLECT_DIRECTORY;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.FORM_DEFINITION;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.operations.transfer.pull.central.PullFromCentral;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PathSourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PullFormDefinition;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PullFromCollectDir;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormMetadataPort;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public interface Pull {
  static Job<Void> buildPullJob(Workspace workspace, Http http, FormMetadataPort formMetadataPort, SubmissionMetadataPort submissionMetadataPort, FormMetadata formMetadata, Consumer<FormStatusEvent> onEvent, Optional<Cursor> lastCursor) {
    SourceOrTarget source = formMetadata.getPullSource().orElseThrow(BriefcaseException::new);

    if (source.getType() == AGGREGATE) {
      return new PullFromAggregate(http, formMetadataPort, submissionMetadataPort, (AggregateServer) source, false, onEvent)
          .pull(formMetadata, formMetadata.getFormFile(), lastCursor);
    }

    if (source.getType() == CENTRAL) {
      CentralServer server = (CentralServer) source;

      String token = http.execute(server.getSessionTokenRequest())
          .orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));
      return new PullFromCentral(http, formMetadataPort, submissionMetadataPort, server, token, onEvent)
          .pull(formMetadata, workspace.buildFormFile(formMetadata));
    }

    if (source.getType() == COLLECT_DIRECTORY && ((PathSourceOrTarget) source).exists()) {
      Path formFile = ((PathSourceOrTarget) source).getPath();
      return new PullFromCollectDir(formMetadataPort, submissionMetadataPort, onEvent)
          .pull(formMetadata.withFormFile(formFile), formMetadata.withFormFile(workspace.buildFormFile(formMetadata)));
    }

    if (source.getType() == FORM_DEFINITION && ((PathSourceOrTarget) source).exists()) {
      Path formFile = ((PathSourceOrTarget) source).getPath();
      return new PullFormDefinition(formMetadataPort, onEvent)
          .pull(formMetadata.withFormFile(formFile), formMetadata.withFormFile(workspace.buildFormFile(formMetadata)));
    }

    throw new BriefcaseException();
  }
}
