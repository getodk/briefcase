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

package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.readAllLines;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBeenCalled;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.buildAggregateSubmissionDownloadXml;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.buildBlankFormXml;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.buildManifestXml;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.buildMediaFiles;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.generatePages;
import static org.opendatakit.briefcase.reused.model.transfer.TransferTestHelpers.getResourcePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.matchers.PathMatchers;
import org.opendatakit.briefcase.reused.Workspace;
import org.opendatakit.briefcase.reused.WorkspaceHelper;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.http.RequestSpy;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormKey;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.preferences.BriefcasePreferences;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;

public class PullFromAggregateTest {
  private static final String BASE_URL = "http://foo.bar";
  private Path tmpDir = createTempDirectory("briefcase-test-");
  private Path workspaceLocation = tmpDir.resolve(BriefcasePreferences.BRIEFCASE_DIR);
  private InMemoryHttp inMemoryHttp;
  private AggregateServer server = AggregateServer.normal(url(BASE_URL));
  private PullFromAggregate pullOp;
  private TestRunnerStatus runnerStatus;
  private PullFromAggregateTracker tracker;
  private List<String> events;
  private boolean includeIncomplete = true;
  private FormMetadata formMetadata;

  @Before
  public void init() throws IOException {
    Files.createDirectories(workspaceLocation);
    Workspace workspace = WorkspaceHelper.inMemory();
    events = new ArrayList<>();
    inMemoryHttp = (InMemoryHttp) workspace.http;
    pullOp = new PullFromAggregate(workspace, server, includeIncomplete, e -> { });
    runnerStatus = new TestRunnerStatus(false);
    formMetadata = FormMetadata.empty(FormKey.of("simple-form"))
        .withFormFile(workspaceLocation.resolve("forms/some-form/some-form.xml"))
        .withUrls(Optional.of(RequestBuilder.url(BASE_URL + "/manifest")), Optional.empty());
    tracker = new PullFromAggregateTracker(formMetadata.getKey(), e -> events.add(e.getMessage()));
  }

  @After
  public void tearDown() {
    deleteRecursive(workspaceLocation);
  }

  @Test
  public void knows_how_to_download_a_form() throws IOException {
    String expectedContent = String.join("\n", readAllLines(getResourcePath("some-form.xml")));
    inMemoryHttp.stub(
        server.getDownloadFormRequest(formMetadata.getKey().getId()),
        ok(expectedContent)
    );

    pullOp.downloadForm(formMetadata, formMetadata.getFormFile(), runnerStatus, tracker);

    Path actualFormFile = formMetadata.getFormFile();
    assertThat(actualFormFile, PathMatchers.exists());

    String actualXml = new String(readAllBytes(actualFormFile));
    assertThat(actualXml, is(expectedContent));
    assertThat(events, contains(
        "Start downloading form",
        "Form downloaded"
    ));
  }

  @Test
  public void knows_how_to_get_form_attachments() {
    List<AggregateAttachment> expectedAttachments = buildMediaFiles(server.getBaseUrl().toString(), 3);

    // Stub the manifest request
    inMemoryHttp.stub(get(server.getBaseUrl()).withPath("/manifest").build(), ok(buildManifestXml(expectedAttachments)));

    List<AggregateAttachment> actualAttachments = pullOp.getFormAttachments(formMetadata, runnerStatus, tracker);

    assertThat(actualAttachments, hasSize(actualAttachments.size()));
    for (AggregateAttachment attachment : expectedAttachments)
      assertThat(actualAttachments, hasItem(attachment));

    assertThat(events, contains(
        "Start getting form manifest",
        "Got the form manifest"
    ));
  }

  @Test
  public void knows_how_to_download_a_form_attachment() {
    List<AggregateAttachment> attachments = buildMediaFiles(server.getBaseUrl().toString(), 3);

    attachments.forEach(attachment -> inMemoryHttp.stub(get(attachment.getDownloadUrl()).build(), ok("some body")));

    AtomicInteger seq = new AtomicInteger(1);
    attachments.forEach(attachment -> pullOp.downloadFormAttachment(attachment, formMetadata.getFormMediaFile(attachment.getFilename()), runnerStatus, tracker, seq.getAndIncrement(), 3));

    attachments.forEach(attachment -> assertThat(formMetadata.getFormMediaFile(attachment.getFilename()), PathMatchers.exists()));

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
  public void knows_how_to_download_a_submission() {
    String instanceId = "uuid:515a13cf-d7a5-4606-a18f-84940b0944b2";
    String expectedContent = buildAggregateSubmissionDownloadXml(instanceId, 2);
    SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(buildBlankFormXml("some-form", "2010010101", "instance-name"));
    String key = subKeyGen.buildKey(instanceId);
    inMemoryHttp.stub(server.getDownloadSubmissionRequest(key), ok(expectedContent));

    DownloadedSubmission actualSubmission = pullOp.downloadSubmission(instanceId, formMetadata.getSubmissionFile(instanceId), subKeyGen, runnerStatus, tracker, 1, 1);

    assertThat(formMetadata.getSubmissionFile(actualSubmission.getInstanceId()), PathMatchers.exists());
    // There's no easy way to assert the submission's contents because the document we stub
    // is not the submission, but an XML document that has the submission and other information.
    // Briefcase has to parse the XML, extract the submission part, and then serialize it back to XML.
    // That's why we have the following hardcoded XML, which makes this test kind of brittle.
    assertThat(actualSubmission.getXml(), is("" +
        "<some-form id=\"some-form\" instanceID=\"uuid:515a13cf-d7a5-4606-a18f-84940b0944b2\" submissionDate=\"2018-07-19T10:36:50.779Z\" isComplete=\"true\" markedAsCompleteDate=\"2018-07-19T10:36:50.779Z\" xmlns=\"http://opendatakit.org/submissions\">\n" +
        "      <n0:meta xmlns:n0=\"http://openrosa.org/xforms\">\n" +
        "        <n0:instanceID>uuid:515a13cf-d7a5-4606-a18f-84940b0944b2</n0:instanceID>\n" +
        "      </n0:meta>\n" +
        "      <some-field>some value</some-field>\n" +
        "    </some-form>"));
    // There's actually another event from the call to tracker.trackTotalSubmissions(1) in this test.
    // Since we don't really care about it, we use Matchers.hasItem() instead of Matchers.contains()
    assertThat(events, contains(
        "Start downloading submission 1 of 1",
        "Submission 1 of 1 downloaded"
    ));
  }

  @Test
  public void knows_how_to_download_a_submission_attachment() {
    String instanceId = "some instance id";
    List<AggregateAttachment> attachments = buildMediaFiles(server.getBaseUrl().toString(), 3);
    DownloadedSubmission submission = new DownloadedSubmission("some xml", instanceId, attachments, Optional.empty());

    attachments.forEach(attachment -> inMemoryHttp.stub(get(attachment.getDownloadUrl()).build(), ok("some body")));

    AtomicInteger seq = new AtomicInteger(1);
    attachments.forEach(attachment -> pullOp.downloadSubmissionAttachment(attachment, formMetadata.getSubmissionAttachmentFile(submission.getInstanceId(), attachment.getFilename()), runnerStatus, tracker, 1, 1, seq.getAndIncrement(), 3));

    attachments.forEach(attachment -> assertThat(formMetadata.getSubmissionAttachmentFile(instanceId, attachment.getFilename()), PathMatchers.exists()));

    assertThat(events, contains(
        "Start downloading attachment 1 of 3 of submission 1 of 1",
        "Attachment 1 of 3 of submission 1 of 1 downloaded",
        "Start downloading attachment 2 of 3 of submission 1 of 1",
        "Attachment 2 of 3 of submission 1 of 1 downloaded",
        "Start downloading attachment 3 of 3 of submission 1 of 1",
        "Attachment 3 of 3 of submission 1 of 1 downloaded"
    ));
  }

  @Test
  public void knows_how_to_get_a_forms_submissions_using_a_cursor() {
    List<Pair<String, Cursor>> pages = generatePages(100, 100);

    // This is the request (without cursor) we shouldn't see cause we're providing one
    RequestSpy<XmlElement> request1Spy = inMemoryHttp.spyOn(
        server.getInstanceIdBatchRequest(formMetadata.getKey().getId(), 100, Cursor.empty(), includeIncomplete),
        ok(pages.get(0).getLeft())
    );

    // This is the one we should see
    Cursor cursor = Cursor.of(LocalDate.of(2010, 1, 5));
    RequestSpy<XmlElement> request2Spy = inMemoryHttp.spyOn(
        server.getInstanceIdBatchRequest(formMetadata.getKey().getId(), 100, cursor, includeIncomplete),
        ok(pages.get(0).getLeft())
    );

    // This is the last request that ends the process of getting batch instanceIds.
    inMemoryHttp.stub(
        server.getInstanceIdBatchRequest(formMetadata.getKey().getId(), 100, pages.get(0).getRight(), includeIncomplete),
        ok(pages.get(1).getLeft())
    );

    pullOp.getSubmissionIds(formMetadata.getKey().getId(), cursor, runnerStatus, tracker);
    assertThat(request1Spy, not(hasBeenCalled()));
    assertThat(request2Spy, hasBeenCalled());
  }

}
