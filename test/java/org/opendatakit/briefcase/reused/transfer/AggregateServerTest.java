/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.reused.transfer;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.transfer.AggregateServer.clearStoredPrefs;
import static org.opendatakit.briefcase.reused.transfer.AggregateServer.isPrefKey;
import static org.opendatakit.briefcase.reused.transfer.AggregateServer.normal;
import static org.opendatakit.briefcase.reused.transfer.AggregateServer.readFromPrefs;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusBuilder;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.http.Credentials;

public class AggregateServerTest {
  public static final Credentials TEST_CREDENTIALS = Credentials.from("some username", "some password");
  private AggregateServer server;

  @Before
  public void setUp() throws MalformedURLException {
    server = normal(new URL("https://some.server.com"));
  }

  @Test
  public void knows_how_to_build_instance_id_batch_urls() throws UnsupportedEncodingException {
    assertThat(
        server.getInstanceIdBatchRequest("some-form", 100, Cursor.empty(), true).getUrl().toString(),
        is("https://some.server.com/view/submissionList?formId=some-form&cursor=&numEntries=100&includeIncomplete=true")
    );
    Cursor cursor = Cursor.of(LocalDate.parse("2010-01-01"));
    assertThat(
        server.getInstanceIdBatchRequest("some-form", 100, cursor, false).getUrl().toString(),
        is("https://some.server.com/view/submissionList?formId=some-form&cursor=" + encode(cursor.get(), UTF_8.toString()) + "&numEntries=100&includeIncomplete=false")
    );
  }

  @Test
  public void stores_and_reads_form_pull_sources_using_prefs() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences pullPanelPrefs = new BriefcasePreferences(InMemoryPreferences.empty()); // Legacy storage

    AggregateServer noCredsServer = new AggregateServer(url("http://foo.bar"), Optional.empty());
    FormStatus aForm = FormStatusBuilder.buildFormStatus(1);
    noCredsServer.storeInPrefs(prefs, aForm, true);

    AggregateServer credsServer = new AggregateServer(url("http://fizz.baz"), Optional.of(TEST_CREDENTIALS));
    FormStatus anotherForm = FormStatusBuilder.buildFormStatus(2);
    credsServer.storeInPrefs(prefs, anotherForm, true);

    assertThat(prefs.keys(), hasSize(6));
    assertThat(readFromPrefs(prefs, pullPanelPrefs, aForm), isPresentAndIs(noCredsServer));
    assertThat(readFromPrefs(prefs, pullPanelPrefs, anotherForm), isPresentAndIs(credsServer));
  }

  @Test
  public void can_read_form_pull_sources_from_legacy_storage() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences pullPanelPrefs = new BriefcasePreferences(InMemoryPreferences.empty()); // Legacy storage

    AggregateServer noCredsServer = new AggregateServer(url("http://foo.bar"), Optional.empty());
    FormStatus aForm = FormStatusBuilder.buildFormStatus(1);
    pullPanelPrefs.put("1_pull_settings_url", noCredsServer.getBaseUrl().toString());
    pullPanelPrefs.put("1_pull_settings_username", "");
    pullPanelPrefs.put("1_pull_settings_password", "");

    AggregateServer credsServer = new AggregateServer(url("http://fizz.baz"), Optional.of(TEST_CREDENTIALS));
    FormStatus anotherForm = FormStatusBuilder.buildFormStatus(2);
    pullPanelPrefs.put("2_pull_settings_url", credsServer.getBaseUrl().toString());
    pullPanelPrefs.put("2_pull_settings_username", TEST_CREDENTIALS.getUsername());
    pullPanelPrefs.put("2_pull_settings_password", TEST_CREDENTIALS.getPassword());

    assertThat(readFromPrefs(prefs, pullPanelPrefs, aForm), isPresentAndIs(noCredsServer));
    assertThat(readFromPrefs(prefs, pullPanelPrefs, anotherForm), isPresentAndIs(credsServer));
  }

  @Test
  public void can_clear_stored_form_pull_sources() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    // Store some key/value that we don't want to clear
    prefs.put("dummy key", "dummy value");

    AggregateServer noCredsServer = new AggregateServer(url("http://foo.bar"), Optional.empty());
    FormStatus aForm = FormStatusBuilder.buildFormStatus(1);
    noCredsServer.storeInPrefs(prefs, aForm, true);

    AggregateServer credsServer = new AggregateServer(url("http://fizz.baz"), Optional.of(TEST_CREDENTIALS));
    FormStatus anotherForm = FormStatusBuilder.buildFormStatus(2);
    credsServer.storeInPrefs(prefs, anotherForm, true);

    noCredsServer.clearStoredPrefs(prefs, aForm);
    credsServer.clearStoredPrefs(prefs, anotherForm);

    assertThat(prefs.keys(), Matchers.contains("dummy key"));
  }

  @Test
  public void new_prefs_storage_has_precedence_over_legacy_storage() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences pullPanelPrefs = new BriefcasePreferences(InMemoryPreferences.empty()); // Legacy storage

    AggregateServer noCredsServer = new AggregateServer(url("http://foo.bar"), Optional.empty());
    FormStatus aForm = FormStatusBuilder.buildFormStatus(1);
    noCredsServer.storeInPrefs(prefs, aForm, true);
    pullPanelPrefs.put("1_pull_settings_url", noCredsServer.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    pullPanelPrefs.put("1_pull_settings_username", "");
    pullPanelPrefs.put("1_pull_settings_password", "");

    AggregateServer credsServer = new AggregateServer(url("http://fizz.baz"), Optional.of(TEST_CREDENTIALS));
    FormStatus anotherForm = FormStatusBuilder.buildFormStatus(2);
    credsServer.storeInPrefs(prefs, anotherForm, true);
    pullPanelPrefs.put("2_pull_settings_url", credsServer.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    pullPanelPrefs.put("2_pull_settings_username", TEST_CREDENTIALS.getUsername());
    pullPanelPrefs.put("2_pull_settings_password", TEST_CREDENTIALS.getPassword());

    assertThat(readFromPrefs(prefs, pullPanelPrefs, aForm), isPresentAndIs(noCredsServer));
    assertThat(readFromPrefs(prefs, pullPanelPrefs, anotherForm), isPresentAndIs(credsServer));
  }

  @Test
  public void moves_prefs_to_new_storage_when_reading_from_legacy_storage() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    BriefcasePreferences pullPanelPrefs = new BriefcasePreferences(InMemoryPreferences.empty()); // Legacy storage

    AggregateServer noCredsServer = new AggregateServer(url("http://foo.bar"), Optional.empty());
    FormStatus aForm = FormStatusBuilder.buildFormStatus(1);
    pullPanelPrefs.put("1_pull_settings_url", noCredsServer.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    pullPanelPrefs.put("1_pull_settings_username", "");
    pullPanelPrefs.put("1_pull_settings_password", "");

    AggregateServer credsServer = new AggregateServer(url("http://fizz.baz"), Optional.of(TEST_CREDENTIALS));
    FormStatus anotherForm = FormStatusBuilder.buildFormStatus(2);
    pullPanelPrefs.put("2_pull_settings_url", credsServer.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    pullPanelPrefs.put("2_pull_settings_username", TEST_CREDENTIALS.getUsername());
    pullPanelPrefs.put("2_pull_settings_password", TEST_CREDENTIALS.getPassword());

    // Sanity check to set the starting scenario
    assert prefs.keys().size() == 0;
    assert pullPanelPrefs.keys().size() == 6;

    // Read the servers. Other tests ensure that they're correctly read
    readFromPrefs(prefs, pullPanelPrefs, aForm);
    readFromPrefs(prefs, pullPanelPrefs, anotherForm);

    assertThat(prefs.keys(), hasSize(6));
    assertThat(pullPanelPrefs.keys(), is(empty()));
  }

  @Test
  public void stores_and_reads_pull_panel_source_without_creds_using_prefs() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.empty());
    server.storeInPrefs(prefs, true);

    assertThat(prefs.keys(), hasSize(3));
    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void no_creds_server_are_stored_regardless_of_user_consent_to_store_passwords() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.empty());
    server.storeInPrefs(prefs, false);

    assertThat(prefs.keys(), hasSize(3));
    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void stores_and_reads_pull_panel_source_with_creds_using_prefs() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.of(TEST_CREDENTIALS));
    server.storeInPrefs(prefs, true);

    assertThat(prefs.keys(), hasSize(3));
    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void creds_server_wont_be_stored_without_user_consent_to_store_password() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.of(TEST_CREDENTIALS));
    server.storeInPrefs(prefs, false);

    assertThat(prefs.keys(), is(empty()));
    assertThat(readFromPrefs(prefs), isEmpty());
  }

  @Test
  public void reads_pull_panel_source_without_creds_using_prefs_and_legacy_keys() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.empty());
    prefs.put("url_1_0", server.getBaseUrl().toString());
    prefs.put("username", "");
    prefs.put("password", "");

    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void reads_pull_panel_source_with_creds_using_prefs_and_legacy_keys() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.of(TEST_CREDENTIALS));
    prefs.put("url_1_0", server.getBaseUrl().toString());
    prefs.put("username", TEST_CREDENTIALS.getUsername());
    prefs.put("password", TEST_CREDENTIALS.getPassword());

    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void new_pref_keys_have_precedende_over_legacy_keys_of_server_without_creds() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.empty());
    server.storeInPrefs(prefs, true);
    prefs.put("url_1_0", server.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    prefs.put("username", "");
    prefs.put("password", "");
    // Sanity check to ensure both new and legacy keys are stored
    assert prefs.keys().size() == 6;

    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void new_pref_keys_have_precedende_over_legacy_keys_of_server_with_creds() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.of(TEST_CREDENTIALS));
    server.storeInPrefs(prefs, true);
    prefs.put("url_1_0", server.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    prefs.put("username", TEST_CREDENTIALS.getUsername());
    prefs.put("password", TEST_CREDENTIALS.getPassword());
    // Sanity check to ensure both new and legacy keys are stored
    assert prefs.keys().size() == 6;

    assertThat(readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void can_clear_stored_pull_panel_source_prefs() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    AggregateServer server = new AggregateServer(url("http://foo.bar"), Optional.of(TEST_CREDENTIALS));
    server.storeInPrefs(prefs, true);
    prefs.put("url_1_0", server.getBaseUrl().toString() + "?old"); // Inject a difference into the old storage
    prefs.put("username", TEST_CREDENTIALS.getUsername());
    prefs.put("password", TEST_CREDENTIALS.getPassword());

    clearStoredPrefs(prefs);

    assertThat(prefs.keys(), is(empty()));
  }

  @Test
  public void knows_whether_a_pref_key_is_related_to_aggregate_server_storage_or_not() {
    assertThat(isPrefKey("url_1_0"), is(true));
    assertThat(isPrefKey("username"), is(true));
    assertThat(isPrefKey("password"), is(true));
    assertThat(isPrefKey("some_form_pull_settings_url"), is(true));
    assertThat(isPrefKey("some_form_pull_settings_username"), is(true));
    assertThat(isPrefKey("some_form_pull_settings_password"), is(true));
    assertThat(isPrefKey("pull_source_aggregate_url"), is(true));
    assertThat(isPrefKey("pull_source_aggregate_username"), is(true));
    assertThat(isPrefKey("pull_source_aggregate_password"), is(true));
    assertThat(isPrefKey("some_form_pull_source_aggregate_url"), is(true));
    assertThat(isPrefKey("some_form_pull_source_aggregate_username"), is(true));
    assertThat(isPrefKey("some_form_pull_source_aggregate_password"), is(true));
  }
}
