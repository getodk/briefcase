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

package org.opendatakit.briefcase.reused.http;

import static com.github.dreamhead.moco.HttpMethod.GET;
import static com.github.dreamhead.moco.HttpMethod.HEAD;
import static com.github.dreamhead.moco.Moco.and;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.contain;
import static com.github.dreamhead.moco.Moco.exist;
import static com.github.dreamhead.moco.Moco.header;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.log;
import static com.github.dreamhead.moco.Moco.match;
import static com.github.dreamhead.moco.Moco.method;
import static com.github.dreamhead.moco.Moco.not;
import static com.github.dreamhead.moco.Moco.status;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Runner.running;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.github.dreamhead.moco.HttpServer;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class CommonsHttpTest {
  private static final URL BASE_URL = getUrl("http://localhost:12306");
  private HttpServer server;
  private Http http;

  @Before
  public void setUp() {
    server = httpServer(12306, log());
    http = new CommonsHttp();
  }

  @Test
  public void can_execute_a_GET_request() throws Exception {
    server.request(and(by(uri("/")), by(method(GET)))).response("foo");
    running(server, () -> assertThat(
        http.execute(Request.get(BASE_URL)).orElseThrow(BriefcaseException::new),
        containsString("foo")
    ));
  }

  @Test
  public void can_execute_a_HEAD_request() throws Exception {
    server.request(and(by(uri("/")), by(method(HEAD)))).response("foo");
    running(server, () -> assertThat(
        http.execute(Request.head(BASE_URL)),
        instanceOf(Response.Success.class)
    ));
  }

  @Test
  public void can_handle_authorization() throws Exception {
    // We'll respond 401 if no Authorization header is received
    server.request(and(
        by(uri("/")),
        by(method(GET)),
        not(exist(header("Authorization")))
    )).response(
        status(401),
        // We include a Digest challenge header on the response
        header("WWW-Authenticate", "Digest realm=\"Aggregate ODK Aggregate\", qop=\"auth\", nonce=\"MTUzNjA1NzY1MjI2NDoxNjIwYTZmMmJkYTNiNGRkMTA2MjI4ZjM4YjE0ZDIyMA==\"")
    );
    // We'll respond 200 if we get a valid Authorization header
    server.request(and(
        by(uri("/")),
        by(method(GET)),
        // The Authorization header should include the expected username
        contain(header("Authorization"), "username=\"username\""),
        // The Authorization header should include the expected realm (from the challenge)
        contain(header("Authorization"), "realm=\"Aggregate ODK Aggregate\""),
        // The Authorization header should include the expected nonce (from the challenge)
        contain(header("Authorization"), "nonce=\"MTUzNjA1NzY1MjI2NDoxNjIwYTZmMmJkYTNiNGRkMTA2MjI4ZjM4YjE0ZDIyMA==\""),
        // The Authorization header should include the challenge response
        match(header("Authorization"), ".+response=\"[a-z0-9]+\".+")
    )).response("foo");

    running(server, () -> assertThat(
        http.execute(Request.get(BASE_URL, Credentials.from("username", "password"))),
        instanceOf(Response.Success.class)
    ));
  }

  @Test
  public void can_handle_5xx_errors() throws Exception {
    server.request(and(by(uri("/")), by(method(GET)))).response(status(500));
    running(server, () -> assertThat(
        http.execute(Request.get(BASE_URL)),
        instanceOf(Response.ServerError.class)
    ));
  }

  @Test
  public void can_handle_4xx_errors() throws Exception {
    server.request(and(by(uri("/")), by(method(GET)))).response(status(400));
    running(server, () -> assertThat(
        http.execute(Request.get(BASE_URL)),
        instanceOf(Response.ClientError.class)
    ));
  }

  @Test
  public void can_handle_3xx_errors() throws Exception {
    server.request(and(by(uri("/")), by(method(GET)))).response(status(302));
    running(server, () -> assertThat(
        http.execute(Request.get(BASE_URL)),
        instanceOf(Response.Redirection.class)
    ));
  }

  private static URL getUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}