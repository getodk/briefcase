package org.opendatakit.briefcase.reused;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.BriefcaseVersionManager.getLatestUrl;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.get;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import org.junit.Test;
import org.opendatakit.briefcase.reused.http.InMemoryHttp;
import org.opendatakit.briefcase.reused.http.response.ResponseHelpers;

public class BriefcaseVersionManagerTest {
  @Test
  public void knows_the_url_to_get_the_latest_version() {
    assertThat(getLatestUrl(), is(url("https://github.com/opendatakit/briefcase/releases/latest")));
  }

  @Test
  public void knows_to_get_the_latest_available_version() {
    InMemoryHttp http = new InMemoryHttp();

    http.stub(
        get(url("https://api.github.com/repos/opendatakit/briefcase/releases/latest")).build(),
        ResponseHelpers.ok("{\"tag_name\":\"v2.0.0\"}")
    );

    BriefcaseVersionManager versionManager = new BriefcaseVersionManager(http, "v1.16.0");

    assertThat(versionManager.getLatest(), is("v2.0.0"));
  }

  @Test
  public void knows_if_we_are_up_to_date() {
    InMemoryHttp http = new InMemoryHttp();

    http.stub(
        get(url("https://api.github.com/repos/opendatakit/briefcase/releases/latest")).build(),
        ResponseHelpers.ok("{\"tag_name\":\"v2.0.0\"}")
    );

    BriefcaseVersionManager versionManager = new BriefcaseVersionManager(http, "v2.0.0");

    assertThat(versionManager.isUpToDate(), is(true));
  }

  @Test
  public void knows_if_we_are_not_up_to_date() {
    InMemoryHttp http = new InMemoryHttp();

    http.stub(
        get(url("https://api.github.com/repos/opendatakit/briefcase/releases/latest")).build(),
        ResponseHelpers.ok("{\"tag_name\":\"v2.0.0\"}")
    );

    BriefcaseVersionManager versionManager = new BriefcaseVersionManager(http, "v1.6.0");

    assertThat(versionManager.isUpToDate(), is(false));
  }
}
