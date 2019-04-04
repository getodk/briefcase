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

package org.opendatakit.briefcase.reused.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.opendatakit.briefcase.reused.http.RequestMethod.GET;
import static org.opendatakit.briefcase.reused.http.RequestMethod.HEAD;
import static org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Pair;
import org.opendatakit.briefcase.reused.UncheckedFiles;
import org.xmlpull.v1.XmlPullParserException;

public class RequestBuilder<T> {
  private final RequestMethod method;
  private final URL url;
  private final Function<InputStream, T> bodyMapper;
  private Optional<Credentials> credentials = Optional.empty();
  private Map<String, String> headers = new HashMap<>();

  private RequestBuilder(RequestMethod method, URL url, Function<InputStream, T> bodyMapper) {
    this.method = method;
    this.url = url;
    this.bodyMapper = bodyMapper;
  }

  private RequestBuilder(RequestMethod method, URL url, Optional<Credentials> credentials, Function<InputStream, T> bodyMapper, Map<String, String> headers) {
    this.method = method;
    this.url = url;
    this.credentials = credentials;
    this.bodyMapper = bodyMapper;
    this.headers = headers;
  }

  public static RequestBuilder<InputStream> get(String url) {
    return get(url(url));
  }

  public static RequestBuilder<InputStream> get(URL url) {
    return new RequestBuilder<>(GET, url, Function.identity());
  }

  public static RequestBuilder<InputStream> head(URL url) {
    return new RequestBuilder<>(HEAD, url, Function.identity());
  }

  private static String readString(InputStream is) {
    try (Scanner scanner = new Scanner(is, UTF_8.name())) {
      return scanner.useDelimiter("\\A").next();
    } catch (NoSuchElementException e) {
      return "";
    }
  }

  private static XmlElement readXmlElement(InputStream is) {
    try (InputStreamReader isr = new InputStreamReader(is)) {
      Document doc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(isr);
      parser.setFeature(FEATURE_PROCESS_NAMESPACES, true);
      doc.parse(parser);
      return XmlElement.of(doc);
    } catch (XmlPullParserException | IOException e) {
      throw new BriefcaseException(e);
    }
  }

  private static String urlEncode(String text) {
    try {
      return URLEncoder.encode(text, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new BriefcaseException(e);
    }
  }

  public static URL url(String baseUrl) {
    try {
      return new URL(baseUrl);
    } catch (MalformedURLException e) {
      throw new BriefcaseException(e);
    }
  }

  public Request<T> build() {
    return new Request<>(method, url, credentials, bodyMapper, headers);
  }

  public RequestBuilder<String> asText() {
    return new RequestBuilder<>(method, url, credentials, RequestBuilder::readString, headers);
  }

  public RequestBuilder<T> withCredentials(Optional<Credentials> maybeCredentials) {
    this.credentials = maybeCredentials;
    return this;
  }

  public RequestBuilder<T> withCredentials(Credentials credentials) {
    this.credentials = Optional.of(credentials);
    return this;
  }

  public <U> RequestBuilder<U> withMapper(Function<T, U> mapper) {
    return new RequestBuilder<>(method, url, credentials, bodyMapper.andThen(mapper), headers);
  }

  public RequestBuilder<T> resolve(String path) {
    // Normalize slashes to ensure that the resulting url
    // has exactly one slash before the input path
    String newUrl = url.toString()
        + (!url.toString().endsWith("/") ? "/" : "")
        + (path.startsWith("/") ? path.substring(1) : path);
    return new RequestBuilder<>(method, url(newUrl), credentials, bodyMapper, headers);
  }

  public RequestBuilder<XmlElement> asXmlElement() {
    return new RequestBuilder<>(method, url, credentials, RequestBuilder::readXmlElement, headers);
  }

  public RequestBuilder<Void> downloadTo(Path target) {
    return new RequestBuilder<>(method, url, credentials, in -> {
      UncheckedFiles.copy(in, target, REPLACE_EXISTING);
      return null;
    }, headers);
  }

  @SafeVarargs
  public final RequestBuilder<T> withQueryString(Pair<String, String>... keyValues) {
    String queryString = Stream.of(keyValues).map(p -> p.getLeft() + "=" + urlEncode(p.getRight())).collect(Collectors.joining("&"));
    return new RequestBuilder<>(method, url(url.toString() + "?" + queryString), credentials, bodyMapper, headers);
  }
}
