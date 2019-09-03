/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.operations.transfer.pull.central;

import static java.nio.file.Files.readAllLines;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.PathMatchers.exists;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.buildSubmissionXml;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.getResourcePath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.WorkspaceHelper;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class PullFromCentralTest {
  private static final CentralServer server = CentralServer.of(url("http://foo.bar"), 1, Credentials.from("username", "password"));
  private static final String token = "some token";
  private final Path briefcaseDir = createTempDirectory("briefcase-test-");
  private InMemoryHttp inMemoryHttp;
  private List<String> events;
  private PullFromCentralTracker tracker;
  private PullFromCentral pullOp;
  private TestRunnerStatus runnerStatus;
  private FormMetadata formMetadata;

  @Before
  public void init() {
    Workspace workspace = WorkspaceHelper.inMemory();
    events = new ArrayList<>();
    inMemoryHttp = (InMemoryHttp) workspace.http;
    pullOp = new PullFromCentral(workspace, server, token, e -> { });
    runnerStatus = new TestRunnerStatus(false);
    formMetadata = FormMetadata.empty(FormKey.of("some-form")).withFormFile(briefcaseDir.resolve("forms/Some form/Some form.xml"));
    tracker = new PullFromCentralTracker(formMetadata.getKey(), e -> events.add(e.getMessage()));
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  // TODO move to PushTestHelper and fixup 43e85490
  static List<CentralAttachment> buildAttachments(int totalAttachments) {
    return IntStream.range(0, totalAttachments)
        .mapToObj(i -> new CentralAttachment("some-file-" + i + ".txt", true))
        .collect(toList());
  }

  static String jsonOfAttachments(List<CentralAttachment> attachments) {
    return "[\n" +
        attachments.stream()
            .map(a -> String.format("" +
                "{\n" +
                "  \"name\":\"%s\",\n" +
                "  \"exists\":%s\n" +
                "}", a.getName(), a.exists() ? "true" : "false"))
            .collect(joining(",\n"))
        + "]";
  }

  static String jsonOfSubmissions(List<String> instanceIds) {
    return "[\n" +
        instanceIds.stream()
            .map(a -> String.format("" +
                "{\n" +
                "  \"instanceId\":\"%s\"\n" +
                "}", a))
            .collect(joining(",\n"))
        + "]";
  }

  @Test
  public void knows_how_to_download_a_form() throws IOException {
    String expectedContent = String.join("\n", readAllLines(getResourcePath("some-form.xml")));
    inMemoryHttp.stub(
        server.getDownloadFormRequest(formMetadata.getKey().getId(), formMetadata.getFormFile(), token),
        ok(expectedContent)
    );

    pullOp.downloadForm(formMetadata.getKey().getId(), formMetadata.getFormFile(), token, runnerStatus, tracker);

    Path actualFormFile = formMetadata.getFormFile();
    assertThat(actualFormFile, exists());

    String actualXml = new String(readAllBytes(actualFormFile));
    assertThat(actualXml, is(expectedContent));
    assertThat(events, contains(
        "Start downloading form",
        "Form downloaded"
    ));
  }

  @Test
  public void knows_how_to_download_a_submission() {
    String instanceId = "uuid:515a13cf-d7a5-4606-a18f-84940b0944b2";
    String expectedSubmissionXml = buildSubmissionXml(instanceId);
    Path submissionFile = formMetadata.getSubmissionFile(instanceId);
    inMemoryHttp.stub(server.getDownloadSubmissionRequest(formMetadata.getKey().getId(), instanceId, submissionFile, token), ok(expectedSubmissionXml));

    pullOp.downloadSubmission(formMetadata.getKey().getId(), instanceId, formMetadata.getSubmissionFile(instanceId), token, runnerStatus, tracker, 1, 1);

    String actualSubmissionXml = new String(readAllBytes(submissionFile));
    assertThat(submissionFile, exists());
    assertThat(actualSubmissionXml, is(expectedSubmissionXml));
    // There's actually another event from the call to tracker.trackTotalSubmissions(1) in this test.
    // Since we don't really care about it, we use Matchers.hasItem() instead of Matchers.contains()
    assertThat(events, contains(
        "Start downloading submission 1 of 1",
        "Submission 1 of 1 downloaded"
    ));
  }

  @Test
  public void knows_how_to_get_form_attachments() {
    List<CentralAttachment> expectedAttachments = buildAttachments(3);

    // Stub the request to get the list of attachments
    inMemoryHttp.stub(server.getFormAttachmentListRequest(formMetadata.getKey().getId(), token), ok(jsonOfAttachments(expectedAttachments)));

    List<CentralAttachment> actualAttachments = pullOp.getFormAttachments(formMetadata.getKey().getId(), token, runnerStatus, tracker);

    assertThat(actualAttachments, hasSize(actualAttachments.size()));
    for (CentralAttachment attachment : expectedAttachments)
      assertThat(actualAttachments, hasItem(attachment));

    assertThat(events, contains(
        "Start getting form attachments",
        "Got all form attachments"
    ));
  }

  @Test
  public void knows_how_to_download_a_form_attachment() {
    List<CentralAttachment> attachments = buildAttachments(3);

    attachments.forEach(attachment -> inMemoryHttp.stub(
        server.getDownloadFormAttachmentRequest(formMetadata.getKey().getId(), attachment, formMetadata.getFormMediaFile(attachment.getName()), token),
        ok("some body")
    ));

    AtomicInteger seq = new AtomicInteger(1);
    attachments.forEach(attachment -> pullOp.downloadFormAttachment(formMetadata.getKey().getId(), formMetadata.getFormMediaFile(attachment.getName()), attachment, token, runnerStatus, tracker, seq.getAndIncrement(), 3));

    attachments.forEach(attachment -> assertThat(formMetadata.getFormMediaFile(attachment.getName()), exists()));

    assertThat(events, contains(
        "Start downloading form attachment 1 of 3",
        "Form attachment 1 of 3 downloaded",
        "Start downloading form attachment 2 of 3",
        "Form attachment 2 of 3 downloaded",
        "Start downloading form attachment 3 of 3",
        "Form attachment 3 of 3 downloaded"
    ));
  }

  @Test
  public void knows_how_to_get_submission() {
    List<String> expectedInstanceIds = IntStream.range(0, 250)
        .mapToObj(i -> "submission instanceID " + i)
        .collect(Collectors.toList());
    inMemoryHttp.stub(
        server.getInstanceIdListRequest(formMetadata.getKey().getId(), token),
        ok(jsonOfSubmissions(expectedInstanceIds))
    );
    List<String> actualInstanceIds = pullOp.getSubmissionIds(formMetadata.getKey().getId(), token, runnerStatus, tracker);
    assertThat(actualInstanceIds, hasSize(expectedInstanceIds.size()));
    for (String instanceId : actualInstanceIds)
      assertThat(expectedInstanceIds, hasItem(instanceId));

    assertThat(events, contains(
        "Start getting submission IDs",
        "Got all the submission IDs"
    ));
  }

  @Test
  public void knows_how_to_get_submission_attachments() {
    String instanceId = "uuid:515a13cf-d7a5-4606-a18f-84940b0944b2";
    List<CentralAttachment> expectedAttachments = buildAttachments(3);
    inMemoryHttp.stub(
        server.getSubmissionAttachmentListRequest(formMetadata.getKey().getId(), instanceId, token),
        ok(jsonOfAttachments(expectedAttachments))
    );

    List<CentralAttachment> actualAttachments = pullOp.getSubmissionAttachments(formMetadata.getKey().getId(), instanceId, token, runnerStatus, tracker, 1, 1);
    assertThat(actualAttachments, hasSize(expectedAttachments.size()));
    for (CentralAttachment attachment : actualAttachments)
      assertThat(expectedAttachments, hasItem(attachment));

    assertThat(events, contains(
        "Start getting attachments of submission 1 of 1",
        "Got all the attachments of submission 1 of 1"
    ));
  }

  @Test
  public void knows_how_to_download_a_submission_attachment() {
    String instanceId = "some instance id";
    List<CentralAttachment> expectedAttachments = buildAttachments(3);

    expectedAttachments.forEach(attachment -> inMemoryHttp.stub(server.getDownloadSubmissionAttachmentRequest(formMetadata.getKey().getId(), instanceId, attachment, formMetadata.getSubmissionAttachmentFile(instanceId, attachment.getName()), token), ok("some body")));

    AtomicInteger seq = new AtomicInteger(1);
    expectedAttachments.forEach(attachment -> pullOp.downloadSubmissionAttachment(formMetadata.getKey().getId(), instanceId, attachment, formMetadata.getSubmissionAttachmentFile(instanceId, attachment.getName()), token, runnerStatus, tracker, 1, 1, seq.getAndIncrement(), 3));

    expectedAttachments.forEach(attachment -> assertThat(formMetadata.getSubmissionAttachmentFile(instanceId, attachment.getName()), exists()));

    assertThat(events, contains(
        "Start downloading attachment 1 of 3 of submission 1 of 1",
        "Attachment 1 of 3 of submission 1 of 1 downloaded",
        "Start downloading attachment 2 of 3 of submission 1 of 1",
        "Attachment 2 of 3 of submission 1 of 1 downloaded",
        "Start downloading attachment 3 of 3 of submission 1 of 1",
        "Attachment 3 of 3 of submission 1 of 1 downloaded"
    ));
  }
}
