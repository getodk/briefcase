package org.opendatakit.briefcase.operations.transfer.push;

import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.AGGREGATE;
import static org.opendatakit.briefcase.operations.transfer.SourceOrTarget.Type.CENTRAL;

import java.util.function.Consumer;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.operations.transfer.push.aggregate.PushToAggregate;
import org.opendatakit.briefcase.operations.transfer.push.central.PushToCentral;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.Job;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.form.FormStatusEvent;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadataPort;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public interface Push {
  public static Job<Void> buildPushJob(Http http, SubmissionMetadataPort submissionMetadataPort, FormMetadata formMetadata, SourceOrTarget target, boolean forceSendForm, Consumer<FormStatusEvent> onEvent) {

    if (target.getType() == AGGREGATE)
      return new PushToAggregate(http, (AggregateServer) target, forceSendForm, onEvent).push(formMetadata);

    if (target.getType() == CENTRAL) {
      CentralServer server = (CentralServer) target;

      String token = http.execute(server.getSessionTokenRequest())
          .orElseThrow(() -> new BriefcaseException("Can't authenticate with ODK Central"));
      return new PushToCentral(http, submissionMetadataPort, server, token, onEvent).push(formMetadata);
    }

    throw new BriefcaseException();
  }
}
