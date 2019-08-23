package org.opendatakit.briefcase.util;

import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;

import java.net.URL;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

public class BriefcaseVersionManager {
  private final Http http;
  private final String current;

  public BriefcaseVersionManager(Http http, String current) {
    this.http = http;
    this.current = current.contains("-") ? current.substring(0, current.indexOf("-")) : current;
  }

  public boolean isUpToDate() {
    return getCurrent().equals(getLatest());
  }

  public String getLatest() {
    return http.execute(RequestBuilder.get("https://api.github.com/repos/opendatakit/briefcase/releases/latest")
        .asJsonMap()
        .withResponseMapper(json -> json.get("tag_name").asText())
        .build())
        .get();
  }

  public String getCurrent() {
    return current;
  }

  public static URL getLatestUrl() {
    return url("https://github.com/opendatakit/briefcase/releases/latest");
  }
}
