package org.opendatakit.briefcase.reused.transfer;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.transfer.CentralServer.cleanUrl;
import static org.opendatakit.briefcase.reused.transfer.CentralServer.isPrefKey;

import org.junit.Test;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.InMemoryPreferences;
import org.opendatakit.briefcase.reused.http.Credentials;

public class CentralServerTest {

  public static final Credentials TEST_CREDENTIALS = Credentials.from("some username", "some password");

  @Test
  public void stores_and_reads_pull_panel_source_prefs() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    CentralServer server = CentralServer.of(url("http://foo.bar"), 1, TEST_CREDENTIALS);

    server.storeInPrefs(prefs, true);

    assertThat(CentralServer.readFromPrefs(prefs), isPresentAndIs(server));
  }

  @Test
  public void server_wont_be_stored_without_user_consent_to_store_passwords() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());
    CentralServer server = CentralServer.of(url("http://foo.bar"), 1, TEST_CREDENTIALS);

    server.storeInPrefs(prefs, false);

    assertThat(prefs.keys(), is(empty()));

    assertThat(CentralServer.readFromPrefs(prefs), isEmpty());
  }

  @Test
  public void can_clear_stored_pull_panel_source_prefs() {
    BriefcasePreferences prefs = new BriefcasePreferences(InMemoryPreferences.empty());

    CentralServer server = CentralServer.of(url("http://foo.bar"), 1, TEST_CREDENTIALS);
    server.storeInPrefs(prefs, true);

    CentralServer.clearStoredPrefs(prefs);

    assertThat(prefs.keys(), is(empty()));
  }

  @Test
  public void knows_whether_a_pref_key_is_related_to_aggregate_server_storage_or_not() {
    assertThat(isPrefKey("pull_source_central_url"), is(true));
    assertThat(isPrefKey("pull_source_central_project_id"), is(true));
    assertThat(isPrefKey("pull_source_central_username"), is(true));
    assertThat(isPrefKey("pull_source_central_password"), is(true));
    assertThat(isPrefKey("some_form_pull_source_central_url"), is(true));
    assertThat(isPrefKey("some_form_pull_source_central_project_id"), is(true));
    assertThat(isPrefKey("some_form_pull_source_central_username"), is(true));
    assertThat(isPrefKey("some_form_pull_source_central_password"), is(true));
  }

  @Test
  public void knows_how_to_clean_copied_and_pasted_Aggregate_URLs_from_a_browser() {
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org/"),
        is("https://sandbox.central.opendatakit.org/")
    );
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org"),
        is("https://sandbox.central.opendatakit.org")
    );
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org/#/"),
        is("https://sandbox.central.opendatakit.org")
    );
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org/#/projects/8"),
        is("https://sandbox.central.opendatakit.org")
    );
  }
}
