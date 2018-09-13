/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.client.config.CookieSpecs.STANDARD;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ServerConnectionInfo;


/**
 * Common utility methods for managing the credentials associated with the
 * request context and constructing http context, client and request with the
 * proper parameters and OpenRosa headers.
 *
 * @author mitchellsundt@gmail.com
 */
final class WebUtils {

  private static final int SERVER_CONNECTION_TIMEOUT = 60000;

  private static final ThreadLocal<HttpClientContext> threadSafeContext = new ThreadLocal<>();

  static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
  static final String OPEN_ROSA_VERSION = "1.0";
  private static final String DATE_HEADER = "Date";

  static final int MAX_CONNECTIONS_PER_ROUTE = Runtime.getRuntime().availableProcessors() * 3;

  private static final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

  static {
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
  }

  /**
   * Get the {@link HttpClientContext} bound to the current thread.
   *
   * @return http context bound to the current thread, or if none exists, a newly created one.
   */
  static HttpClientContext getHttpContext() {
    HttpClientContext httpContext = threadSafeContext.get();
    if (httpContext == null) {
      httpContext = createHttpContext();
      threadSafeContext.set(httpContext);
    }
    return httpContext;
  }

  /**
   * Resets the context clearing cookies and aith parameters and residual information
   * from previous requests
   */
  static void resetHttpContext() {
    getHttpContext().getCookieStore().clear();
    getHttpContext().getTargetAuthState().reset();
    Optional.ofNullable(getHttpContext().getAuthCache()).ifPresent(AuthCache::clear);
    getHttpContext().getCredentialsProvider().clear();
  }

  /**
   * Convenience method for {@link #setCredentials(HttpClientContext, ServerConnectionInfo, URI, boolean)}.
   */
  static void setCredentials(HttpClientContext httpContext, ServerConnectionInfo info, URI uri) {
    setCredentials(httpContext, info, uri, false);
  }

  /**
   * Sets up the authentication credentials for the specified http context.
   *
   * @param httpContext the context to set credentials on
   * @param info        supplies the credentials, if any
   * @param uri         the uri to supply credentials for (uses hostname)
   * @param alwaysReset replace context's creds every time when 'true', otherwise only if not already present
   */
  static void setCredentials(HttpClientContext httpContext, ServerConnectionInfo info, URI uri, boolean alwaysReset) {
    String hostname = uri.getHost();
    if (info.hasCredentials()) {
      if (alwaysReset || !hasCredentials(httpContext, hostname)) {
        clearAllCredentials(httpContext);
        addCredentials(httpContext, info.getUsername(), info.getPassword(), hostname);
      }
    } else {
      clearAllCredentials(httpContext);
    }
  }

  private static List<AuthScope> buildAuthScopes(String host) {
    List<AuthScope> asList = new ArrayList<>();

    AuthScope a;
    // allow digest auth on any port...
    a = new AuthScope(host, -1, null, AuthSchemes.DIGEST);
    asList.add(a);
    // and allow basic auth on the standard TLS/SSL ports...
    a = new AuthScope(host, 443, null, AuthSchemes.BASIC);
    asList.add(a);
    a = new AuthScope(host, 8443, null, AuthSchemes.BASIC);
    asList.add(a);

    return asList;
  }

  private static void clearAllCredentials(HttpClientContext localContext) {
    CredentialsProvider credsProvider = localContext.getCredentialsProvider();
    if (credsProvider != null) {
      credsProvider.clear();
    }
  }

  private static boolean hasCredentials(HttpClientContext localContext, String host) {
    CredentialsProvider credsProvider = localContext.getCredentialsProvider();

    List<AuthScope> asList = buildAuthScopes(host);
    boolean hasCreds = true;
    for (AuthScope a : asList) {
      Credentials c = credsProvider.getCredentials(a);
      if (c == null) {
        hasCreds = false;
        continue;
      }
    }
    return hasCreds;
  }

  private static void addCredentials(HttpClientContext localContext, String userEmail, char[] password, String host) {
    Credentials c = new UsernamePasswordCredentials(userEmail, new String(password));
    addCredentials(localContext, c, host);
  }

  private static void addCredentials(HttpClientContext localContext, Credentials c, String host) {
    CredentialsProvider credsProvider = localContext.getCredentialsProvider();

    List<AuthScope> asList = buildAuthScopes(host);
    for (AuthScope a : asList) {
      credsProvider.setCredentials(a, c);
    }
  }

  private static void setOpenRosaHeaders(HttpRequest req) {
    req.setHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION);
    req.setHeader(DATE_HEADER,
        org.apache.http.client.utils.DateUtils.formatDate(new Date(), org.apache.http.client.utils.DateUtils.PATTERN_RFC1036));
  }

  static HttpGet createOpenRosaHttpGet(URI uri) {
    HttpGet req = new HttpGet();
    setOpenRosaHeaders(req);
    req.setURI(uri);
    return req;
  }

  static HttpPost createOpenRosaHttpPost(URI uri) {
    HttpPost req = new HttpPost(uri);
    setOpenRosaHeaders(req);
    return req;
  }

  static HttpClient createHttpClient() {
    // configure connection
    SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(SERVER_CONNECTION_TIMEOUT).build();

    // if possible, bias toward digest auth (may not be in 4.0 beta 2)
    List<String> targetPreferredAuthSchemes = new ArrayList<>();
    targetPreferredAuthSchemes.add(AuthSchemes.DIGEST);
    targetPreferredAuthSchemes.add(AuthSchemes.BASIC);

    RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
        .setConnectTimeout(SERVER_CONNECTION_TIMEOUT)
        // support authenticating
        .setAuthenticationEnabled(true)
        // support redirecting to handle http: => https: transition
        .setRedirectsEnabled(true)
        .setMaxRedirects(1)
        .setCircularRedirectsAllowed(true)
        .setTargetPreferredAuthSchemes(targetPreferredAuthSchemes)
        .setCookieSpec(STANDARD)
        .build();

    HttpClientBuilder clientBuilder = HttpClientBuilder.create()
        .setConnectionManager(connectionManager)
        .setConnectionManagerShared(true)
        .setDefaultSocketConfig(socketConfig)
        .setDefaultRequestConfig(requestConfig);

    HttpHost proxy = BriefcasePreferences.getBriefCaseProxyConnection();
    if (proxy != null) {
      clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
    }

    return clientBuilder.build();
  }

  private static HttpClientContext createHttpContext() {
    // set up one context for all HTTP requests so that authentication
    // and cookies can be retained.
    HttpClientContext localContext = HttpClientContext.create();

    // establish a local cookie store for this attempt at downloading...
    CookieStore cookieStore = new BasicCookieStore();
    localContext.setCookieStore(cookieStore);

    // and establish a credentials provider...
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    localContext.setCredentialsProvider(credsProvider);

    return localContext;
  }

  static String createLinkWithProperties(String url,
                                         Map<String, String> properties) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(url);
    if (properties != null) {
      Set<Map.Entry<String, String>> propSet = properties.entrySet();
      if (!propSet.isEmpty()) {
        urlBuilder.append("?");
        boolean firstParam = true;
        for (Map.Entry<String, String> property : propSet) {
          if (firstParam) {
            firstParam = false;
          } else {
            urlBuilder.append("&");
          }

          String value = property.getValue();
          if (value == null) {
            value = "NULL";
          }

          String valueEncoded;
          try {
            valueEncoded = URLEncoder.encode(value, UTF_8.name());
          } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                "unrecognized UTF-8 encoding");
          }
          urlBuilder.append(property.getKey()).append("=").append(valueEncoded);
        }
      }
    }
    return urlBuilder.toString();
  }

}
