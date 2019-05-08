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

package org.opendatakit.briefcase.pull.aggregate;

import static java.nio.file.Files.readAllBytes;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBeenCalled;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildAggregateSubmissionDownloadXml;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildBlankFormXml;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildFormStatus;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildManifestXml;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildMediaFiles;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.generatePages;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.matchers.PathMatchers;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.http.FakeHttp;
import org.opendatakit.briefcase.reused.http.RequestSpy;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;

public class PullFromAggregateTest {
  private FakeHttp http;
  private Path briefcaseDir = createTempDirectory("briefcase-test-");
  private AggregateServer server = AggregateServer.normal(url("http://foo.bar"));
  private FormStatus form = buildFormStatus("some-form", server.getBaseUrl().toString());
  private PullFromAggregate pullOp;
  private TestRunnerStatus runnerStatus;
  private PullFromAggregateTracker tracker;
  private List<String> events;
  private boolean includeIncomplete = true;

  @Before
  public void init() {
    http = new FakeHttp();
    events = new ArrayList<>();
    tracker = new PullFromAggregateTracker(form, e -> events.add(e.getStatusString()));
    pullOp = new PullFromAggregate(http, server, briefcaseDir, includeIncomplete, e -> {});
    runnerStatus = new TestRunnerStatus(false);
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  @Test
  public void knows_how_to_download_a_form() throws IOException {
    String expectedContent = "form content - won't be parsed";
    http.stub(
        server.getDownloadFormRequest(form.getFormId()),
        ok(expectedContent)
    );

    pullOp.downloadForm(form, runnerStatus, tracker);

    Path actualFormFile = form.getFormFile(briefcaseDir);
    assertThat(actualFormFile, PathMatchers.exists());

    String actualXml = new String(readAllBytes(actualFormFile));
    assertThat(actualXml, is(expectedContent));
    assertThat(events, contains("Downloaded form some-form"));
  }

  @Test
  public void knows_how_to_get_form_attachments() {
    List<MediaFile> expectedAttachments = buildMediaFiles(server.getBaseUrl().toString(), 3);

    // Stub the manifest request
    http.stub(get(server.getBaseUrl()).build(), ok(buildManifestXml(expectedAttachments)));

    List<MediaFile> actualAttachments = pullOp.getFormAttachments(form, runnerStatus, tracker);

    assertThat(actualAttachments, hasSize(actualAttachments.size()));
    for (MediaFile attachment : expectedAttachments)
      assertThat(actualAttachments, hasItem(attachment));

    assertThat(events, contains("Downloading 3 form attachments"));
  }

  @Test
  public void knows_how_to_download_a_form_attachment() {
    List<MediaFile> attachments = buildMediaFiles(server.getBaseUrl().toString(), 3);

    attachments.forEach(attachment -> http.stub(get(attachment.getDownloadUrl()).build(), ok("some body")));

    attachments.forEach(attachment -> pullOp.downloadFormAttachment(form, attachment, runnerStatus, tracker));

    attachments.forEach(attachment -> assertThat(form.getFormMediaFile(briefcaseDir, attachment.getFilename()), PathMatchers.exists()));

    assertThat(events, allOf(
        hasItem("Downloaded form attachment some-file-0.txt"),
        hasItem("Downloaded form attachment some-file-1.txt"),
        hasItem("Downloaded form attachment some-file-2.txt")
    ));
  }

  @Test
  public void knows_how_to_download_a_submission() {
    String instanceId = "uuid:515a13cf-d7a5-4606-a18f-84940b0944b2";
    String expectedContent = buildAggregateSubmissionDownloadXml(instanceId, 2);
    SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(buildBlankFormXml("some-form", "2010010101", "instance-name"));
    String key = subKeyGen.buildKey(instanceId);
    http.stub(server.getDownloadSubmissionRequest(key), ok(expectedContent));
    tracker.trackTotalSubmissions(1);

    DownloadedSubmission actualSubmission = pullOp.downloadSubmission(form, instanceId, subKeyGen, runnerStatus, tracker);

    assertThat(form.getSubmissionFile(briefcaseDir, actualSubmission.getInstanceId()), PathMatchers.exists());
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
    assertThat(events, hasItem("Downloaded submission 1 of 1"));
  }

  @Test
  public void knows_how_to_download_a_submission_attachment() {
    String instanceId = "some instance id";
    List<MediaFile> attachments = buildMediaFiles(server.getBaseUrl().toString(), 3);
    DownloadedSubmission submission = new DownloadedSubmission("some xml", instanceId, attachments);

    attachments.forEach(attachment -> http.stub(get(attachment.getDownloadUrl()).build(), ok("some body")));

    attachments.forEach(attachment -> pullOp.downloadSubmissionAttachment(form, submission, attachment, runnerStatus, tracker));

    attachments.forEach(attachment -> assertThat(form.getSubmissionMediaFile(briefcaseDir, instanceId, attachment.getFilename()), PathMatchers.exists()));

    assertThat(events, allOf(
        hasItem("Downloaded attachment some-file-0.txt of submission some instance id"),
        hasItem("Downloaded attachment some-file-1.txt of submission some instance id"),
        hasItem("Downloaded attachment some-file-2.txt of submission some instance id")
    ));
  }

  @Test
  public void knows_how_to_get_a_forms_submissions_using_a_cursor() {
    List<Pair<String, Cursor>> pages = generatePages(100, 100);

    // This is the request (without cursor) we shouldn't see cause we're providing one
    RequestSpy<XmlElement> request1Spy = http.spyOn(
        server.getInstanceIdBatchRequest(form.getFormId(), 100, Cursor.empty(), includeIncomplete),
        ok(pages.get(0).getLeft())
    );

    // This is the one we should see
    Cursor cursor = Cursor.of(LocalDate.of(2010, 1, 5));
    RequestSpy<XmlElement> request2Spy = http.spyOn(
        server.getInstanceIdBatchRequest(form.getFormId(), 100, cursor, includeIncomplete),
        ok(pages.get(0).getLeft())
    );

    // This is the last request that ends the process of getting batch instanceIds.
    http.stub(
        server.getInstanceIdBatchRequest(form.getFormId(), 100, pages.get(0).getRight(), includeIncomplete),
        ok(pages.get(1).getLeft())
    );

    pullOp.getSubmissions(form, cursor, runnerStatus, tracker);
    assertThat(request1Spy, not(hasBeenCalled()));
    assertThat(request2Spy, hasBeenCalled());
  }

}
