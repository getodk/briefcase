package org.opendatakit.briefcase.model.form;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.opendatakit.briefcase.model.form.AsJson.getJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class SubmissionExportMetadata implements AsJson {
  private final String instanceId;
  private final OffsetDateTime submissionDate;
  private final OffsetDateTime exportDateTime;

  SubmissionExportMetadata(String instanceId, OffsetDateTime submissionDate, OffsetDateTime exportDateTime) {
    this.instanceId = instanceId;
    this.submissionDate = submissionDate;
    this.exportDateTime = exportDateTime;
  }

  static SubmissionExportMetadata from(JsonNode root) {
    return new SubmissionExportMetadata(
        getJson(root, "instanceId").map(JsonNode::asText).orElseThrow(BriefcaseException::new),
        getJson(root, "submissionDate").map(JsonNode::asText).map(OffsetDateTime::parse).orElseThrow(BriefcaseException::new),
        getJson(root, "exportDateTime").map(JsonNode::asText).map(OffsetDateTime::parse).orElseThrow(BriefcaseException::new)
    );
  }

  @Override
  public ObjectNode asJson(ObjectMapper mapper) {
    ObjectNode root = mapper.createObjectNode();
    root.put("instanceId", instanceId);
    root.put("submissionDate", submissionDate.format(ISO_OFFSET_DATE_TIME));
    root.put("exportDateTime", exportDateTime.format(ISO_OFFSET_DATE_TIME));
    return root;
  }

  public boolean isBefore(OffsetDateTime submissionDate) {
    return this.submissionDate.isBefore(submissionDate);
  }
}
