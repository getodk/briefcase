package org.opendatakit.briefcase.operations.transfer.pull;

import static org.opendatakit.briefcase.reused.model.form.FormMetadataQueries.lastCursorOf;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceQueries.getStartPullFromLast;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor;
import org.opendatakit.briefcase.operations.transfer.pull.aggregate.PullFromAggregate;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PathSourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PullFormDefinition;
import org.opendatakit.briefcase.operations.transfer.pull.filesystem.PullFromCollectDir;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public interface Pull {
  static Job<Void> buildPullJob(Container container, FormMetadata formMetadata, Consumer<FormStatusEvent> onEvent) {
    SourceOrTarget source = formMetadata.getPullSource().orElseThrow(BriefcaseException::new);
    if (source.getType() == SourceOrTarget.Type.AGGREGATE) {
      AggregateServer server = (AggregateServer) source;
      Optional<Cursor> lastCursor = container.preferences.query(getStartPullFromLast())
          ? container.formMetadata.query(lastCursorOf(formMetadata.getKey()))
          : Optional.empty();

      return new PullFromAggregate(container, server, false, onEvent)
          .pull(formMetadata, formMetadata.getFormFile(), lastCursor);
    } else if (source.getType() == SourceOrTarget.Type.CENTRAL) {
      CentralServer server = (CentralServer) source;

      String token = container.http.execute(server.getSessionTokenRequest())
          .orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));
      return new org.opendatakit.briefcase.operations.transfer.pull.central.PullFromCentral(container, server, token, onEvent)
          .pull(formMetadata, container.workspace.buildFormFile(formMetadata));
    } else if (source.getType() == SourceOrTarget.Type.COLLECT_DIRECTORY && ((PathSourceOrTarget) source).exists()) {
      Path formFile = ((PathSourceOrTarget) source).getPath();
      return new PullFromCollectDir(container, onEvent)
          .pull(formMetadata.withFormFile(formFile), formMetadata.withFormFile(container.workspace.buildFormFile(formMetadata)));
    } else if (source.getType() == SourceOrTarget.Type.FORM_DEFINITION && ((PathSourceOrTarget) source).exists()) {
      Path formFile = ((PathSourceOrTarget) source).getPath();
      return new PullFormDefinition(container, onEvent)
          .pull(formMetadata.withFormFile(formFile), formMetadata.withFormFile(container.workspace.buildFormFile(formMetadata)));
    } else {

      throw new BriefcaseException();
    }
  }

  Job<Void> execute();
}
