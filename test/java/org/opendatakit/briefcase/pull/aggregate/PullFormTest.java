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

import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.pull.aggregate.PullTestHelpers.buildBlankFormXml;
import static org.opendatakit.briefcase.pull.aggregate.PullTestHelpers.buildSubmissionXml;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.FakeHttp;

public class PullFormTest {
  private static final RemoteServer SERVER = RemoteServer.normal(url("http://foo.bar"));
  private static final FormStatus FORM = PullTestHelpers.buildFormStatus(SERVER.getBaseUrl().toString());
  private final Path briefcaseDir = createTempDirectory("briefcase-test-");
  private FakeHttp http = new FakeHttp();
  private List<String> events;
  private PullTracker tracker;

  @Before
  public void init() {
    http = new FakeHttp();
    events = new ArrayList<>();
    tracker = new PullTracker(FORM, e -> events.add(e.getStatusString()));
  }

  @After
  public void tearDown() {
    deleteRecursive(briefcaseDir);
  }

  @Test
  public void knows_how_to_download_a_blank_form() throws IOException {
    String expectedContent = "form content - won't be parsed";
    http.stub(
        SERVER.getDownloadFormRequest(FORM.getFormId()),
        ok(expectedContent)
    );

    PullForm pf = new PullForm(http, SERVER, briefcaseDir, true);
    pf.downloadForm(FORM, tracker);

    String sanitizedFormName = stripIllegalChars(FORM.getFormName());
    String actualXml = new String(readAllBytes(briefcaseDir
        .resolve("forms")
        .resolve(sanitizedFormName)
        .resolve(sanitizedFormName + ".xml")));
    assertThat(actualXml, is(expectedContent));
    assertThat(events, contains("Downloaded form some form"));
  }

  @Test
  public void knows_how_to_download_a_blank_forms_media_attachments() {
    Pair<String, List<Pair<String, String>>> manifestUrlsAndNames = PullTestHelpers.buildManifest(
        SERVER.getBaseUrl().toString(),
        3
    );
    String manifest = manifestUrlsAndNames.getLeft();
    List<Pair<String, String>> urlsAndNames = manifestUrlsAndNames.getRight();

    // Stub the manifest request
    http.stub(get(SERVER.getBaseUrl()).build(), ok(manifest));
    // Stub the individual media attachment requests
    urlsAndNames.forEach(pair -> http.stub(get(pair.getRight()).build(), ok("file 1")));

    PullForm pf = new PullForm(http, SERVER, briefcaseDir, true);
    pf.downloadFormAttachments(FORM, tracker);

    assertThat(urlsAndNames, hasSize(3));
    urlsAndNames.forEach(pair -> assertThat(
        exists(FORM.getFormMediaDir(briefcaseDir).resolve(pair.getLeft())),
        is(true)
    ));
    assertThat(events, contains("Downloaded 3 attachments"));
  }

  @Test
  public void knows_how_to_download_a_submission() {
    String instanceId = "uuid:515a13cf-d7a5-4606-a18f-84940b0944b2";
    String expectedContent = buildSubmissionXml(instanceId, 2);
    SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(buildBlankFormXml("some-form", "2010010101", "instance-name"));
    String key = subKeyGen.buildKey(instanceId);
    http.stub(
        SERVER.getDownloadSubmissionRequest(key),
        ok(expectedContent)
    );
    http.stub(
        get("http://foo.bar").build(),
        ok("media file contents")
    );

    PullForm pf = new PullForm(http, SERVER, briefcaseDir, true);
    pf.downloadSubmissionAndMedia(FORM, tracker, instanceId, subKeyGen);

    Path submissionDir = FORM.getSubmissionDir(briefcaseDir, instanceId);
    assertThat(exists(submissionDir.resolve("submission.xml")), is(true));
    assertThat(exists(submissionDir.resolve("some-filename-0.txt")), is(true));
    assertThat(exists(submissionDir.resolve("some-filename-1.txt")), is(true));

    assertThat(events, contains(
        "Downloaded 2 attachments",
        "Downloaded submission 1 of 0" // it's "of 0" because this test doesn't follow the whole pull process that initializes the total number of submissions
    ));
  }

}
