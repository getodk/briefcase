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

package org.opendatakit.briefcase.push.aggregate;


import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasBeenCalled;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.hasPart;
import static org.opendatakit.briefcase.reused.http.RequestSpyMatchers.isMultipart;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.buildFormStatus;
import static org.opendatakit.briefcase.reused.transfer.TransferTestHelpers.listOfFormsResponseFromAggregate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.http.FakeHttp;
import org.opendatakit.briefcase.reused.http.RequestSpy;
import org.opendatakit.briefcase.reused.job.TestRunnerStatus;
import org.opendatakit.briefcase.reused.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.transfer.TransferTestHelpers;

public class PushToAggregateTest {
  private AggregateServer server = AggregateServer.normal(url("http://foo.bar"));
  private FormStatus formStatus = buildFormStatus("push-form-test", server.getBaseUrl().toString());
  private FakeHttp http;
  private Path briefcaseDir;
  private List<String> events;
  private TestRunnerStatus runnerStatus;
  private PushToAggregateTracker tracker;
  private Path form;
  private Path formAttachment;
  private String instanceId = "uuid:520e7b86-1572-45b1-a89e-7da26ad1624e";
  private Path submissionAttachment;
  private Path submission;

  @Before
  public void setUp() throws IOException {
    http = new FakeHttp();
    briefcaseDir = createTempDirectory("briefcase-test-");
    events = new ArrayList<>();
    runnerStatus = new TestRunnerStatus(false);
    tracker = new PushToAggregateTracker(formStatus, this::onEvent);
    form = TransferTestHelpers.installForm(formStatus, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/push/aggregate/push-form-test.xml"), briefcaseDir);
    formAttachment = TransferTestHelpers.installFormAttachment(formStatus, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/push/aggregate/sparrow.png"), briefcaseDir);
    submission = TransferTestHelpers.installSubmission(formStatus, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/push/aggregate/submission.xml"), briefcaseDir);
    submissionAttachment = TransferTestHelpers.installSubmissionAttachment(formStatus, TransferTestHelpers.getResourcePath("/org/opendatakit/briefcase/push/aggregate/1556532531101.jpg"), briefcaseDir, instanceId);
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  private void onEvent(FormStatusEvent e) {
    events.add(e.getStatusString());
  }

  @Test
  public void knows_how_to_check_if_the_form_already_exists_in_Central() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, this::onEvent);

    RequestSpy<?> requestSpy = http.spyOn(
        server.getFormExistsRequest(formStatus.getFormId()),
        ok(listOfFormsResponseFromAggregate(formStatus))
    );

    boolean exists = pushOp.checkFormExists(formStatus.getFormId(), runnerStatus, tracker);

    assertThat(requestSpy, allOf(hasBeenCalled(), not(isMultipart())));
    assertThat(exists, is(true));
  }

  @Test
  public void knows_how_to_check_if_the_form_does_not_exist_in_Central() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, this::onEvent);

    http.stub(
        server.getFormExistsRequest(formStatus.getFormId()),
        ok(listOfFormsResponseFromAggregate())
    );

    assertThat(pushOp.checkFormExists(formStatus.getFormId(), runnerStatus, tracker), is(false));
  }

  @Test
  public void knows_how_to_push_forms_and_their_attachments_to_Aggregate() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, this::onEvent);

    RequestSpy<?> requestSpy = http.spyOn(server.getPushFormRequest(form, singletonList(formAttachment)));

    pushOp.pushFormAndAttachments(formStatus, singletonList(formAttachment), runnerStatus, tracker);

    assertThat(requestSpy, allOf(
        hasBeenCalled(),
        isMultipart(),
        hasPart("form_def_file", "application/xml", "push_form_test.xml"),
        hasPart("sparrow.png", "application/octet-stream", "sparrow.png")
    ));
  }

  @Test
  public void knows_how_to_push_submissions_and_their_attachments_to_Aggregate() {
    // Low-level test that drives an individual step of the push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, this::onEvent);

    RequestSpy<?> requestSpy = http.spyOn(
        server.getPushSubmissionRequest(submission, singletonList(submissionAttachment)),
        ok("<root/>")
    );

    pushOp.pushSubmissionAndAttachments(formStatus, instanceId, singletonList(submissionAttachment), runnerStatus, tracker);

    assertThat(requestSpy, allOf(
        hasBeenCalled(),
        isMultipart(),
        hasPart("xml_submission_file", "application/xml", "submission.xml"),
        hasPart("1556532531101.jpg", "image/jpeg", "1556532531101.jpg")
    ));

    assertThat(events, hasItem("Pushed submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e with 1 attachments"));
  }

  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_doesn_exist_in_Aggregate() {
    // High-level test that drives the public push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, this::onEvent);

    http.stub(server.getFormExistsRequest(formStatus.getFormId()), ok(listOfFormsResponseFromAggregate()));
    http.stub(server.getPushFormRequest(form, singletonList(formAttachment)), ok("<root/>"));
    http.stub(server.getPushSubmissionRequest(submission, singletonList(submissionAttachment)), ok("<root/>"));

    TransferTestHelpers.launchJob(pushOp.push(formStatus));

    assertThat(events, allOf(
        hasItem("Form doesn't exist in Aggregate"),
        hasItem("Pushed form with 1 attachments"),
        hasItem("Pushed submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e with 1 attachments")
    ));
  }

  @Test
  public void knows_how_to_push_completely_a_form_when_the_form_exists_in_Aggregate() {
    // High-level test that drives the public push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, false, this::onEvent);

    http.stub(server.getFormExistsRequest(formStatus.getFormId()), ok(listOfFormsResponseFromAggregate(formStatus)));
    http.stub(server.getPushFormRequest(form, singletonList(formAttachment)), ok("<root/>"));
    http.stub(server.getPushSubmissionRequest(submission, singletonList(submissionAttachment)), ok("<root/>"));

    TransferTestHelpers.launchJob(pushOp.push(formStatus));

    assertThat(events, allOf(
        hasItem("Form already exists in Aggregate"),
        not(hasItem("Pushed form with 1 attachments")),
        hasItem("Pushed submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e with 1 attachments")
    ));
  }

  @Test
  public void can_force_send_a_form_even_when_the_form_exists_in_Aggregate() {
    // High-level test that drives the public push operation
    PushToAggregate pushOp = new PushToAggregate(http, server, briefcaseDir, true, this::onEvent);

    http.stub(server.getFormExistsRequest(formStatus.getFormId()), ok(listOfFormsResponseFromAggregate(formStatus)));
    http.stub(server.getPushFormRequest(form, singletonList(formAttachment)), ok("<root/>"));
    http.stub(server.getPushSubmissionRequest(submission, singletonList(submissionAttachment)), ok("<root/>"));

    TransferTestHelpers.launchJob(pushOp.push(formStatus));

    assertThat(events, allOf(
        hasItem("Forcing push of form and attachments"),
        hasItem("Pushed form with 1 attachments"),
        hasItem("Pushed submission uuid:520e7b86-1572-45b1-a89e-7da26ad1624e with 1 attachments")
    ));
  }

}

