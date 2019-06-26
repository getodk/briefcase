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
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.http.RequestMethod.GET;
import static org.opendatakit.briefcase.reused.http.RequestMethod.HEAD;
import static org.opendatakit.briefcase.reused.http.RequestMethod.POST;
import static org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Pair;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Builder class used to produce instances of {@link Request}.
 * <p>
 * Use the {@link #get(String)}, {@link #get(URL)}, {@link #post(URL)} )},
 * and {@link #head(URL)} factories to get an instance of this class.
 */
public class RequestBuilder<T> {
  private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {};
  private final RequestMethod method;
  private final URL baseUrl;
  private final Function<InputStream, T> responseMapper;
  private final Optional<Credentials> credentials;
  private final Map<String, String> headers;
  private final Optional<InputStream> body;
  private final List<MultipartMessage> multipartMessages;

  RequestBuilder(RequestMethod method, URL baseUrl, Function<InputStream, T> responseMapper, Optional<Credentials> credentials, Map<String, String> headers, Optional<InputStream> body, List<MultipartMessage> multipartMessages) {
    this.method = method;
    this.baseUrl = baseUrl;
    this.credentials = credentials;
    this.responseMapper = responseMapper;
    this.headers = headers;
    this.body = body;
    this.multipartMessages = multipartMessages;
  }

  public static RequestBuilder<InputStream> get(String baseUrl) {
    return new RequestBuilder<>(GET, url(stripTrailingSlash(baseUrl)), Function.identity(), empty(), new HashMap<>(), empty(), emptyList());
  }

  public static RequestBuilder<InputStream> get(URL baseUrl) {
    return new RequestBuilder<>(GET, url(stripTrailingSlash(baseUrl)), Function.identity(), empty(), new HashMap<>(), empty(), emptyList());
  }

  public static RequestBuilder<InputStream> post(URL baseUrl) {
    return new RequestBuilder<>(POST, url(stripTrailingSlash(baseUrl)), Function.identity(), empty(), new HashMap<>(), empty(), emptyList());
  }

  public static RequestBuilder<InputStream> head(URL baseUrl) {
    return new RequestBuilder<>(HEAD, url(stripTrailingSlash(baseUrl)), Function.identity(), empty(), new HashMap<>(), empty(), emptyList());
  }

  private static String readString(InputStream in) {
    try (Scanner scanner = new Scanner(in, UTF_8.name())) {
      return scanner.useDelimiter("\\A").next();
    } catch (NoSuchElementException e) {
      return "";
    }
  }

  private static XmlElement readXmlElement(InputStream in) {
    try (InputStreamReader ir = new InputStreamReader(in)) {
      Document doc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(ir);
      parser.setFeature(FEATURE_PROCESS_NAMESPACES, true);
      doc.parse(parser);
      return XmlElement.of(doc);
    } catch (XmlPullParserException | IOException e) {
      throw new BriefcaseException(e);
    }
  }

  private static Map<String, Object> readJsonMap(InputStream in) {
    try (InputStream inHandle = in) {
      return JSON_OBJECT_MAPPER.readValue(inHandle, JSON_MAP_TYPE_REF);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static <U> Function<InputStream, List<U>> readJsonList(Class<U> mappingClass) {
    return in -> {
      try (InputStream inHandle = in) {
        return JSON_OBJECT_MAPPER.readValue(
            inHandle,
            JSON_OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, mappingClass)
        );
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
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

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/")
        ? url.substring(0, url.length() - 1)
        : url;
  }

  private static String stripTrailingSlash(URL url) {
    return stripTrailingSlash(url.toString());
  }

  public static boolean isUri(String candidate) {
    try {
      new URL(candidate).toURI();
      return true;
    } catch (URISyntaxException | MalformedURLException e) {
      return false;
    }
  }

  public Request<T> build() {
    return new Request<>(method, baseUrl, credentials, responseMapper, headers, body, multipartMessages);
  }

  public RequestBuilder<String> asText() {
    return new RequestBuilder<>(method, baseUrl, RequestBuilder::readString, credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<XmlElement> asXmlElement() {
    return new RequestBuilder<>(method, baseUrl, RequestBuilder::readXmlElement, credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<Map<String, Object>> asJsonMap() {
    return new RequestBuilder<>(method, baseUrl, RequestBuilder::readJsonMap, credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<List<Map>> asJsonList() {
    return new RequestBuilder<>(method, baseUrl, RequestBuilder.readJsonList(Map.class), credentials, headers, body, multipartMessages);
  }

  public <U> RequestBuilder<List<U>> asJsonList(Class<U> mappingClass) {
    return new RequestBuilder<>(method, baseUrl, RequestBuilder.readJsonList(mappingClass), credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<Void> downloadTo(Path target) {
    return new RequestBuilder<>(method, baseUrl, in -> {
      copy(in, target, REPLACE_EXISTING);
      return null;
    }, credentials, headers, body, multipartMessages);
  }

  public <U> RequestBuilder<U> withResponseMapper(Function<T, U> mapper) {
    return new RequestBuilder<>(method, baseUrl, responseMapper.andThen(mapper), credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<T> withCredentials(Optional<Credentials> credentials) {
    return new RequestBuilder<>(method, baseUrl, responseMapper, credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<T> withCredentials(Credentials credentials) {
    return new RequestBuilder<>(method, baseUrl, responseMapper, Optional.of(credentials), headers, body, multipartMessages);
  }

  public RequestBuilder<T> withPath(String path) {
    String cleanPath = path.startsWith("/") && path.endsWith("/")
        ? path.substring(1, path.length() - 1)
        : path.startsWith("/")
        ? path.substring(1)
        : path.endsWith("/")
        ? path.substring(0, path.length() - 1)
        : path;
    URL newBaseUrl = url(baseUrl + "/" + cleanPath);
    return new RequestBuilder<>(method, newBaseUrl, responseMapper, credentials, headers, body, multipartMessages);
  }

  @SafeVarargs
  public final RequestBuilder<T> withQuery(Pair<String, String>... keyValues) {
    if (baseUrl.getQuery() != null)
      throw new BriefcaseException("Can't apply withQuery() twice");
    String queryString = Stream.of(keyValues).map(p -> p.getLeft() + "=" + urlEncode(p.getRight())).collect(joining("&"));
    URL newBaseUrl = url(baseUrl.toString() + "?" + queryString);
    return new RequestBuilder<>(method, newBaseUrl, responseMapper, credentials, headers, body, multipartMessages);
  }

  public RequestBuilder<T> withBody(String bodyContents) {
    Optional<InputStream> newBody = Optional.of(new ByteArrayInputStream(bodyContents.getBytes(UTF_8)));
    return new RequestBuilder<>(method, baseUrl, responseMapper, credentials, headers, newBody, multipartMessages);
  }

  public RequestBuilder<T> withBody(InputStream body) {
    return new RequestBuilder<>(method, baseUrl, responseMapper, credentials, headers, Optional.of(body), multipartMessages);
  }

  public RequestBuilder<T> withHeader(String name, String value) {
    Map<String, String> newHeaders = new HashMap<>(headers);
    newHeaders.put(name, value);
    return new RequestBuilder<>(method, baseUrl, responseMapper, credentials, newHeaders, body, multipartMessages);
  }

  public RequestBuilder<T> withMultipartMessage(String name, String contentType, String attachmentName, InputStream messageBody) {
    List<MultipartMessage> newMultipartMessages = new ArrayList<>(multipartMessages);
    newMultipartMessages.add(new MultipartMessage(name, contentType, attachmentName, messageBody));
    return new RequestBuilder<>(method, baseUrl, responseMapper, credentials, headers, body, newMultipartMessages);
  }
}
