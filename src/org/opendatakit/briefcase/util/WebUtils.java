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

/*
 * Copyright (C) 2011 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

/**
 * Common utility methods for managing the credentials associated with the
 * request context and constructing http context, client and request with the
 * proper parameters and OpenRosa headers.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public final class WebUtils {
	private static final String PATTERN_DATE_TOSTRING = "EEE MMM dd HH:mm:ss zzz yyyy";
	private static final String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private static final String PATTERN_ISO8601_DATE = "yyyy-MM-ddZ";
	private static final String PATTERN_ISO8601_TIME = "HH:mm:ss.SSSZ";
	private static final String PATTERN_YYYY_MM_DD_DATE_ONLY_NO_TIME_DASH = "yyyy-MM-dd";
	private static final String PATTERN_NO_DATE_TIME_ONLY = "HH:mm:ss.SSS";

	public static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
	public static final String OPEN_ROSA_VERSION = "1.0";
	private static final String DATE_HEADER = "Date";
	
	private static final SimpleDateFormat iso8601 = new SimpleDateFormat(PATTERN_ISO8601);
	private static final SimpleDateFormat dateOnly = new SimpleDateFormat(PATTERN_ISO8601_DATE);
	private static final SimpleDateFormat timeOnly = new SimpleDateFormat(PATTERN_ISO8601_TIME);
	/**
	 * Parse a string into a datetime value.  Tries the common
	 * Http formats, the iso8601 format (used by Javarosa), the
	 * default formatting from Date.toString(), and a time-only
	 * format.
	 * 
	 * @param value
	 * @return
	 */
	public static final Date parseDate(String value) {
		Date d = null;
		if ( value != null ) {
			try {
				// try the common HTTP date formats
				d = DateUtils.parseDate(value,
						new String[] { DateUtils.PATTERN_RFC1123,
									   DateUtils.PATTERN_RFC1036,
									   DateUtils.PATTERN_ASCTIME,
									   PATTERN_ISO8601,
									   PATTERN_DATE_TOSTRING,
									   PATTERN_NO_DATE_TIME_ONLY,
									   PATTERN_YYYY_MM_DD_DATE_ONLY_NO_TIME_DASH} );
			} catch ( DateParseException e) {
				throw new IllegalArgumentException("Unparsable date: " + value, e);
			}
		}
		return d;
	}

	/**
	 * Return the ISO8601 string representation of a date.
	 * 
	 * @param d
	 * @return
	 */
	public static final String iso8601DateTime(Date d) {
		if ( d == null ) return null;
		return iso8601.format(d);
	}

	public static final String iso8601DateOnly(Date d) {
		if ( d == null ) return null;
		return dateOnly.format(d);
	}
	
	public static final String iso8601TimeOnly(Date d) {
		if ( d == null ) return null;
		return timeOnly.format(d);
	}

	public static final List<AuthScope> buildAuthScopes(String host) {
		List<AuthScope> asList = new ArrayList<AuthScope>();

		AuthScope a;
		// allow digest auth on any port...
		a = new AuthScope(host, -1, null, AuthPolicy.DIGEST);
		asList.add(a);
		// and allow basic auth on the standard TLS/SSL ports...
		a = new AuthScope(host, 443, null, AuthPolicy.BASIC);
		asList.add(a);
		a = new AuthScope(host, 8443, null, AuthPolicy.BASIC);
		asList.add(a);

		return asList;
	}

	public static final void clearAllCredentials(HttpContext localContext) {
		CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);
		if ( credsProvider != null ) {
		  credsProvider.clear();
		}
	}

	public static final boolean hasCredentials(HttpContext localContext,
			String userEmail, String host) {
		CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);

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

	public static final void addCredentials(HttpContext localContext,
			String userEmail, char[] password, String host) {
		Credentials c = new UsernamePasswordCredentials(userEmail, new String(password));
		addCredentials(localContext, c, host);
	}

	private static final void addCredentials(HttpContext localContext,
			Credentials c, String host) {
		CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);

		List<AuthScope> asList = buildAuthScopes(host);
		for (AuthScope a : asList) {
			credsProvider.setCredentials(a, c);
		}
	}

	private static final void setOpenRosaHeaders(HttpRequest req) {
		req.setHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION);
		req.setHeader(DATE_HEADER,
				DateUtils.formatDate(new Date(), DateUtils.PATTERN_RFC1036));
	}

	public static final HttpHead createOpenRosaHttpHead(URI uri) {
		HttpHead req = new HttpHead(uri);
		setOpenRosaHeaders(req);
		return req;
	}

	public static final HttpGet createOpenRosaHttpGet(URI uri) {
		HttpGet req = new HttpGet();
		setOpenRosaHeaders(req);
		req.setURI(uri);
		return req;
	}

	public static final HttpPost createOpenRosaHttpPost(URI uri) {
		HttpPost req = new HttpPost(uri);
		setOpenRosaHeaders(req);
		return req;
	}

	public static final HttpClient createHttpClient(int timeout) {
		// configure connection
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, timeout);
		HttpConnectionParams.setSoTimeout(params, timeout);
		// support redirecting to handle http: => https: transition
		HttpClientParams.setRedirecting(params, true);
		// support authenticating
		HttpClientParams.setAuthenticating(params, true);
		// if possible, bias toward digest auth (may not be in 4.0 beta 2)
		List<String> authPref = new ArrayList<String>();
		authPref.add(AuthPolicy.DIGEST);
		authPref.add(AuthPolicy.BASIC);
		// does this work in Google's 4.0 beta 2 snapshot?
		params.setParameter("http.auth-target.scheme-pref", authPref);

		// setup client
		HttpClient httpclient = new DefaultHttpClient(params);
      httpclient.getParams().setParameter(ClientPNames.MAX_REDIRECTS, 1);
      httpclient.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);

      return httpclient;
	}

	public static HttpContext createHttpContext() {
		// set up one context for all HTTP requests so that authentication
		// and cookies can be retained.
		HttpContext localContext = new SyncBasicHttpContext(
				new BasicHttpContext());

		// establish a local cookie store for this attempt at downloading...
		CookieStore cookieStore = new BasicCookieStore();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		// and establish a credentials provider...
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);

		return localContext;
	}

	public static final String createLinkWithProperties(String url,
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
						valueEncoded = URLEncoder.encode(value, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new IllegalStateException(
								"unrecognized UTF-8 encoding");
					}
					urlBuilder.append(property.getKey() + "=" + valueEncoded);
				}
			}
		}
		return urlBuilder.toString();
	}

}
