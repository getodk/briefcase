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

package org.opendatakit.briefcase.reused;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.BriefcasePreferences.AGGREGATE_1_0_URL;
import static org.opendatakit.briefcase.model.BriefcasePreferences.PASSWORD;
import static org.opendatakit.briefcase.model.BriefcasePreferences.USERNAME;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.RemoteFormDefinition;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.http.HttpException;
import org.opendatakit.briefcase.reused.http.RequestBuilder;
import org.opendatakit.briefcase.reused.http.Response;

/**
 * This class represents a remote Aggregate server and it has some methods
 * to query its state.
 */
public class RemoteServer {
  public static List<String> PREFERENCE_KEYS = Arrays.asList(AGGREGATE_1_0_URL, USERNAME, PASSWORD);
  private final URL baseUrl;
  private final Optional<Credentials> credentials;

  private RemoteServer(URL baseUrl, Optional<Credentials> credentials) {
    this.baseUrl = baseUrl;
    this.credentials = credentials;
  }

  public static RemoteServer authenticated(URL baseUrl, Credentials credentials) {
    return new RemoteServer(baseUrl, Optional.of(credentials));
  }

  public static RemoteServer normal(URL baseUrl) {
    return new RemoteServer(baseUrl, Optional.empty());
  }

  public static Optional<RemoteServer> readPreferences(BriefcasePreferences prefs) {
    if (prefs.hasKey(AGGREGATE_1_0_URL)) {
      return prefs.nullSafeGet(AGGREGATE_1_0_URL)
          .map(RemoteServer::parseUrl)
          .map(baseUrl -> new RemoteServer(
              baseUrl,
              OptionalProduct.all(
                  prefs.nullSafeGet(USERNAME),
                  prefs.nullSafeGet(PASSWORD)
              ).map(Credentials::new)
          ));
    }
    return Optional.empty();
  }

  private static URL parseUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new BriefcaseException(e);
    }
  }

  public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
    prefs.remove(AGGREGATE_1_0_URL);
    prefs.remove(USERNAME);
    prefs.remove(PASSWORD);

    // We only save the Aggregate URL if no credentials are defined or
    // if they're defined and we have the user's consent to save passwords,
    // to avoid saving a URL that won't work without credentials.
    if (!credentials.isPresent() || storePasswords)
      prefs.put(AGGREGATE_1_0_URL, getBaseUrl().toString());

    // We only save the credentials if we have the user's consent to save
    // passwords
    if (credentials.isPresent() && storePasswords) {
      prefs.put(USERNAME, credentials.get().getUsername());
      prefs.put(PASSWORD, credentials.get().getPassword());
    }
  }

  public URL getBaseUrl() {
    return baseUrl;
  }

  public ServerConnectionInfo asServerConnectionInfo() {
    return new ServerConnectionInfo(
        baseUrl.toString(),
        credentials.map(Credentials::getUsername).orElse(null),
        credentials.map(Credentials::getPassword).orElse("").toCharArray()
    );
  }

  public Response<Boolean> testPull(Http http) {
    return http.execute(RequestBuilder.get(baseUrl).withPath("/formList").withCredentials(credentials).withMapper(__ -> true).build());
  }

  public Response<Boolean> testPush(Http http) {
    return http.execute(RequestBuilder.head(baseUrl).withPath("/upload").withCredentials(credentials).withMapper(__ -> true).build());
  }

  public boolean containsForm(Http http, String formId) {
    return http.execute(RequestBuilder.get(baseUrl)
        .withPath("/formList")
        .withCredentials(credentials)
        .asText()
        .withMapper(body -> Stream.of(body.split("\n")).anyMatch(line -> line.contains("?formId=" + formId)))
        .build())
        .orElse(false);
  }

  public List<RemoteFormDefinition> getFormsList(Http http) {
    Response<List<RemoteFormDefinition>> response = http.execute(RequestBuilder.get(baseUrl)
        .asXmlElement()
        .withPath("/formList")
        .withCredentials(credentials)
        .withMapper(root -> root.findElements("xform").stream().map(e -> new RemoteFormDefinition(
            e.findElement("name").flatMap(XmlElement::maybeValue).orElseThrow(BriefcaseException::new),
            e.findElement("formID").flatMap(XmlElement::maybeValue).orElseThrow(BriefcaseException::new),
            e.findElement("version").flatMap(XmlElement::maybeValue).orElse(null),
            e.findElement("downloadUrl").flatMap(XmlElement::maybeValue).orElseThrow(BriefcaseException::new),
            e.findElement("manifestUrl").flatMap(XmlElement::maybeValue).orElse(null)
        )).collect(toList())).build());
    return response.orElseThrow(() -> new HttpException(response));
  }

  public interface Test extends Function<RemoteServer, Response<Boolean>> {
    default Response<Boolean> test(RemoteServer server) {
      return apply(server);
    }
  }
}
