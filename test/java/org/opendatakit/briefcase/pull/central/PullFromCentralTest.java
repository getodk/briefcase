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

package org.opendatakit.briefcase.pull.central;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.PathMatchers.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.readAllBytes;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildFormStatus;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildSubmissionXml;

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
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.FakeHttp;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.transfer.CentralAttachment;
import org.opendatakit.briefcase.reused.transfer.CentralServer;
import org.opendatakit.briefcase.ui.export.InMemoryFormMetadataAdapter;

public class PullFromCentralTest {
  private static final CentralServer server = CentralServer.of(url("http://foo.bar"), 1, Credentials.from("username", "password"));
  private static final FormStatus form = buildFormStatus("some-form", server.getBaseUrl().toString());
  private static final String token = "some token";
  private final Path briefcaseDir = createTempDirectory("briefcase-test-");
  private FakeHttp http = new FakeHttp();
  private List<String> events;
  private PullFromCentralTracker tracker;
  private PullFromCentral pullOp;
  private TestRunnerStatus runnerStatus;

  @Before
  public void init() {
    http = new FakeHttp();
    events = new ArrayList<>();
    tracker = new PullFromCentralTracker(form, e -> events.add(e.getStatusString()));
    pullOp = new PullFromCentral(http, server, briefcaseDir, token, e -> { }, new InMemoryFormMetadataAdapter());
    runnerStatus = new TestRunnerStatus(false);
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
    String expectedContent = "form content - won't be parsed";
    http.stub(
        server.getDownloadFormRequest(form.getFormId(), form.getFormFile(briefcaseDir), token),
        ok(expectedContent)
    );

    pullOp.downloadForm(form, token, runnerStatus, tracker);

    Path actualFormFile = form.getFormFile(briefcaseDir);
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
    Path submissionFile = form.getSubmissionFile(briefcaseDir, instanceId);
    http.stub(server.getDownloadSubmissionRequest(form.getFormId(), instanceId, submissionFile, token), ok(expectedSubmissionXml));

    pullOp.downloadSubmission(form, instanceId, token, runnerStatus, tracker, 1, 1);

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
    http.stub(server.getFormAttachmentListRequest(form.getFormId(), token), ok(jsonOfAttachments(expectedAttachments)));

    List<CentralAttachment> actualAttachments = pullOp.getFormAttachments(form, token, runnerStatus, tracker);

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

    attachments.forEach(attachment -> http.stub(
        server.getDownloadFormAttachmentRequest(form.getFormId(), attachment, form.getFormMediaFile(briefcaseDir, attachment.getName()), token),
        ok("some body")
    ));

    AtomicInteger seq = new AtomicInteger(1);
    attachments.forEach(attachment -> pullOp.downloadFormAttachment(form, attachment, token, runnerStatus, tracker, seq.getAndIncrement(), 3));

    attachments.forEach(attachment -> assertThat(form.getFormMediaFile(briefcaseDir, attachment.getName()), exists()));

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
    http.stub(
        server.getInstanceIdListRequest(form.getFormId(), token),
        ok(jsonOfSubmissions(expectedInstanceIds))
    );
    List<String> actualInstanceIds = pullOp.getSubmissionIds(form, token, runnerStatus, tracker);
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
    http.stub(
        server.getSubmissionAttachmentListRequest(form.getFormId(), instanceId, token),
        ok(jsonOfAttachments(expectedAttachments))
    );

    List<CentralAttachment> actualAttachments = pullOp.getSubmissionAttachments(form, instanceId, token, runnerStatus, tracker, 1, 1);
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

    expectedAttachments.forEach(attachment -> http.stub(server.getDownloadSubmissionAttachmentRequest(form.getFormId(), instanceId, attachment, form.getSubmissionMediaFile(briefcaseDir, instanceId, attachment.getName()), token), ok("some body")));

    AtomicInteger seq = new AtomicInteger(1);
    expectedAttachments.forEach(attachment -> pullOp.downloadSubmissionAttachment(form, instanceId, attachment, token, runnerStatus, tracker, 1, 1, seq.getAndIncrement(), 3));

    expectedAttachments.forEach(attachment -> assertThat(form.getSubmissionMediaFile(briefcaseDir, instanceId, attachment.getName()), exists()));

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
