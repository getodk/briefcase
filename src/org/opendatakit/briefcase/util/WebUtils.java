<<<<<<< HEAD
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.javarosa.core.model.utils.DateUtils;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ProxyConnection;

/**
 * Common utility methods for managing the credentials associated with the
 * request context and constructing http context, client and request with the
 * proper parameters and OpenRosa headers.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public final class WebUtils {
	  /**
	   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
	   * copied from apache.commons.lang.DateUtils
	   */
	  private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	  /**
	   * Date format pattern used to parse HTTP date headers in RFC 1036 format.
	   * copied from apache.commons.lang.DateUtils
	   */
	  private static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

	  /**
	   * Date format pattern used to parse HTTP date headers in ANSI C 
	   * <code>asctime()</code> format.
	   * copied from apache.commons.lang.DateUtils
	   */
	  private static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
	  private static final String PATTERN_DATE_TOSTRING = "EEE MMM dd HH:mm:ss zzz yyyy";
	  private static final String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	  private static final String PATTERN_ISO8601_WITHOUT_ZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	  private static final String PATTERN_ISO8601_DATE = "yyyy-MM-ddZ";
	  private static final String PATTERN_ISO8601_TIME = "HH:mm:ss.SSSZ";
	  private static final String PATTERN_YYYY_MM_DD_DATE_ONLY_NO_TIME_DASH = "yyyy-MM-dd";
	  private static final String PATTERN_NO_DATE_TIME_ONLY = "HH:mm:ss.SSS";

	public static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
	public static final String OPEN_ROSA_VERSION = "1.0";
	private static final String DATE_HEADER = "Date";
	

	  private static final Date parseDateSubset( String value, String[] parsePatterns, Locale l, TimeZone tz) {
	    // borrowed from apache.commons.lang.DateUtils...
	    Date d = null;
	    SimpleDateFormat parser = null;
	    ParsePosition pos = new ParsePosition(0);
	    for (int i = 0; i < parsePatterns.length; i++) {
	      if (i == 0) {
	        if ( l == null ) {
	          parser = new SimpleDateFormat(parsePatterns[0]);
	        } else {
	          parser = new SimpleDateFormat(parsePatterns[0], l);
	        }
	      } else {
	        parser.applyPattern(parsePatterns[i]);
	      }
	      parser.setTimeZone(tz); // enforce UTC for formats without timezones
	      pos.setIndex(0);
	      d = parser.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    return d;
	  }
	  /**
	   * Parse a string into a datetime value. Tries the common Http formats, the
	   * iso8601 format (used by Javarosa), the default formatting from
	   * Date.toString(), and a time-only format.
	   * 
	   * @param value
	   * @return
	   */
	  public static final Date parseDate(String value) {
	    if ( value == null || value.length() == 0 ) return null;

	    String[] iso8601Pattern = new String[] {
	    		PATTERN_ISO8601 };
	    
	    String[] localizedParsePatterns = new String[] {
	        // try the common HTTP date formats that have time zones
	        PATTERN_RFC1123, 
	        PATTERN_RFC1036, 
	        PATTERN_DATE_TOSTRING };

	    String[] localizedNoTzParsePatterns = new String[] {
	        // ones without timezones... (will assume UTC)
	        PATTERN_ASCTIME }; 
	    
	    String[] tzParsePatterns = new String[] {
	        PATTERN_ISO8601,
	        PATTERN_ISO8601_DATE, 
	        PATTERN_ISO8601_TIME };
	    
	    String[] noTzParsePatterns = new String[] {
	        // ones without timezones... (will assume UTC)
	        PATTERN_ISO8601_WITHOUT_ZONE, 
	        PATTERN_NO_DATE_TIME_ONLY,
	        PATTERN_YYYY_MM_DD_DATE_ONLY_NO_TIME_DASH };

	    Date d = null;
	    // iso8601 parsing is sometimes off-by-one when JR does it...
	    d = parseDateSubset(value, iso8601Pattern, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    // try to parse with the JavaRosa parsers
	    d = DateUtils.parseDateTime(value);
	    if ( d != null ) return d;
	    d = DateUtils.parseDate(value);
	    if ( d != null ) return d;
	    d = DateUtils.parseTime(value);
	    if ( d != null ) return d;
	    // try localized and english text parsers (for Web headers and interactive filter spec.)
	    d = parseDateSubset(value, localizedParsePatterns, Locale.ENGLISH, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, localizedParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, localizedNoTzParsePatterns, Locale.ENGLISH, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, localizedNoTzParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    // try other common patterns that might not quite match JavaRosa parsers
	    d = parseDateSubset(value, tzParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, noTzParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    // try the locale- and timezone- specific parsers
	    {
	      DateFormat formatter = DateFormat.getDateTimeInstance();
	      ParsePosition pos = new ParsePosition(0);
	      d = formatter.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    {
	      DateFormat formatter = DateFormat.getDateInstance();
	      ParsePosition pos = new ParsePosition(0);
	      d = formatter.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    {
	      DateFormat formatter = DateFormat.getTimeInstance();
	      ParsePosition pos = new ParsePosition(0);
	      d = formatter.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    throw new IllegalArgumentException("Unable to parse the date: " + value);
	  }

	  public static final String asSubmissionDateTimeString(Date d) {
	    if (d == null)
	      return null;
	    return DateUtils.formatDateTime(d, DateUtils.FORMAT_ISO8601);
	  }

	  public static final String asSubmissionDateOnlyString(Date d) {
	    if (d == null)
	      return null;
	    return DateUtils.formatDate(d, DateUtils.FORMAT_ISO8601);
	  }

	  public static final String asSubmissionTimeOnlyString(Date d) {
	    if (d == null)
	      return null;
	    return DateUtils.formatTime(d, DateUtils.FORMAT_ISO8601);
	  }

	public static final List<AuthScope> buildAuthScopes(String host) {
		List<AuthScope> asList = new ArrayList<AuthScope>();

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

	public static final void clearAllCredentials(HttpClientContext localContext) {
		CredentialsProvider credsProvider = localContext.getCredentialsProvider();
		if ( credsProvider != null ) {
		  credsProvider.clear();
		}
	}

	public static final boolean hasCredentials(HttpClientContext localContext,
			String userEmail, String host) {
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

	public static final void addCredentials(HttpClientContext localContext,
			String userEmail, char[] password, String host) {
		Credentials c = new UsernamePasswordCredentials(userEmail, new String(password));
		addCredentials(localContext, c, host);
	}

	private static final void addCredentials(HttpClientContext localContext,
			Credentials c, String host) {
		CredentialsProvider credsProvider = localContext.getCredentialsProvider();

		List<AuthScope> asList = buildAuthScopes(host);
		for (AuthScope a : asList) {
			credsProvider.setCredentials(a, c);
		}
	}

	private static final void setOpenRosaHeaders(HttpRequest req) {
		req.setHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION);
		req.setHeader(DATE_HEADER,
				org.apache.http.client.utils.DateUtils.formatDate(new Date(), org.apache.http.client.utils.DateUtils.PATTERN_RFC1036));
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
	  SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(timeout).build();
	  
     // if possible, bias toward digest auth (may not be in 4.0 beta 2)
     List<String> targetPreferredAuthSchemes = new ArrayList<String>();
     targetPreferredAuthSchemes.add(AuthSchemes.DIGEST);
     targetPreferredAuthSchemes.add(AuthSchemes.BASIC);

     RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
	      .setConnectTimeout(timeout)
	      // support authenticating
	      .setAuthenticationEnabled(true)
	      // support redirecting to handle http: => https: transition
	      .setRedirectsEnabled(true)
	      .setMaxRedirects(1)
	      .setCircularRedirectsAllowed(true)
	      .setTargetPreferredAuthSchemes(targetPreferredAuthSchemes)
	      .build();
     
     CloseableHttpClient httpClient;

     if (BriefcasePreferences.getBriefCaseProxyType().equals(ProxyConnection.ProxyType.NO_PROXY.toString())) {
    	 httpClient = HttpClientBuilder.create()
    			 .setDefaultSocketConfig(socketConfig)
    			 .setDefaultRequestConfig(requestConfig).build();
     } else {
    	 HttpHost proxy = new HttpHost(BriefcasePreferences.getBriefCaseProxyHost(),
    			 Integer.parseInt(BriefcasePreferences.getBriefCaseProxyPort()),
    			 BriefcasePreferences.getBriefCaseProxyType().toLowerCase());
    	 DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
    	 httpClient = HttpClientBuilder.create()
    			 .setDefaultSocketConfig(socketConfig)
    			 .setDefaultRequestConfig(requestConfig)
    			 .setRoutePlanner(routePlanner).build();
     }
      
      return httpClient;
	}

	public static HttpClientContext createHttpContext() {
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.javarosa.core.model.utils.DateUtils;

/**
 * Common utility methods for managing the credentials associated with the
 * request context and constructing http context, client and request with the
 * proper parameters and OpenRosa headers.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public final class WebUtils {
	  /**
	   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
	   * copied from apache.commons.lang.DateUtils
	   */
	  private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	  /**
	   * Date format pattern used to parse HTTP date headers in RFC 1036 format.
	   * copied from apache.commons.lang.DateUtils
	   */
	  private static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

	  /**
	   * Date format pattern used to parse HTTP date headers in ANSI C 
	   * <code>asctime()</code> format.
	   * copied from apache.commons.lang.DateUtils
	   */
	  private static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
	  private static final String PATTERN_DATE_TOSTRING = "EEE MMM dd HH:mm:ss zzz yyyy";
	  private static final String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	  private static final String PATTERN_ISO8601_WITHOUT_ZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	  private static final String PATTERN_ISO8601_DATE = "yyyy-MM-ddZ";
	  private static final String PATTERN_ISO8601_TIME = "HH:mm:ss.SSSZ";
	  private static final String PATTERN_YYYY_MM_DD_DATE_ONLY_NO_TIME_DASH = "yyyy-MM-dd";
	  private static final String PATTERN_NO_DATE_TIME_ONLY = "HH:mm:ss.SSS";

	public static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
	public static final String OPEN_ROSA_VERSION = "1.0";
	private static final String DATE_HEADER = "Date";
	

	  private static final Date parseDateSubset( String value, String[] parsePatterns, Locale l, TimeZone tz) {
	    // borrowed from apache.commons.lang.DateUtils...
	    Date d = null;
	    SimpleDateFormat parser = null;
	    ParsePosition pos = new ParsePosition(0);
	    for (int i = 0; i < parsePatterns.length; i++) {
	      if (i == 0) {
	        if ( l == null ) {
	          parser = new SimpleDateFormat(parsePatterns[0]);
	        } else {
	          parser = new SimpleDateFormat(parsePatterns[0], l);
	        }
	      } else {
	        parser.applyPattern(parsePatterns[i]);
	      }
	      parser.setTimeZone(tz); // enforce UTC for formats without timezones
	      pos.setIndex(0);
	      d = parser.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    return d;
	  }
	  /**
	   * Parse a string into a datetime value. Tries the common Http formats, the
	   * iso8601 format (used by Javarosa), the default formatting from
	   * Date.toString(), and a time-only format.
	   * 
	   * @param value
	   * @return
	   */
	  public static final Date parseDate(String value) {
	    if ( value == null || value.length() == 0 ) return null;

	    String[] iso8601Pattern = new String[] {
	    		PATTERN_ISO8601 };
	    
	    String[] localizedParsePatterns = new String[] {
	        // try the common HTTP date formats that have time zones
	        PATTERN_RFC1123, 
	        PATTERN_RFC1036, 
	        PATTERN_DATE_TOSTRING };

	    String[] localizedNoTzParsePatterns = new String[] {
	        // ones without timezones... (will assume UTC)
	        PATTERN_ASCTIME }; 
	    
	    String[] tzParsePatterns = new String[] {
	        PATTERN_ISO8601,
	        PATTERN_ISO8601_DATE, 
	        PATTERN_ISO8601_TIME };
	    
	    String[] noTzParsePatterns = new String[] {
	        // ones without timezones... (will assume UTC)
	        PATTERN_ISO8601_WITHOUT_ZONE, 
	        PATTERN_NO_DATE_TIME_ONLY,
	        PATTERN_YYYY_MM_DD_DATE_ONLY_NO_TIME_DASH };

	    Date d = null;
	    // iso8601 parsing is sometimes off-by-one when JR does it...
	    d = parseDateSubset(value, iso8601Pattern, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    // try to parse with the JavaRosa parsers
	    d = DateUtils.parseDateTime(value);
	    if ( d != null ) return d;
	    d = DateUtils.parseDate(value);
	    if ( d != null ) return d;
	    d = DateUtils.parseTime(value);
	    if ( d != null ) return d;
	    // try localized and english text parsers (for Web headers and interactive filter spec.)
	    d = parseDateSubset(value, localizedParsePatterns, Locale.ENGLISH, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, localizedParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, localizedNoTzParsePatterns, Locale.ENGLISH, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, localizedNoTzParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    // try other common patterns that might not quite match JavaRosa parsers
	    d = parseDateSubset(value, tzParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    d = parseDateSubset(value, noTzParsePatterns, null, TimeZone.getTimeZone("GMT"));
	    if ( d != null ) return d;
	    // try the locale- and timezone- specific parsers
	    {
	      DateFormat formatter = DateFormat.getDateTimeInstance();
	      ParsePosition pos = new ParsePosition(0);
	      d = formatter.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    {
	      DateFormat formatter = DateFormat.getDateInstance();
	      ParsePosition pos = new ParsePosition(0);
	      d = formatter.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    {
	      DateFormat formatter = DateFormat.getTimeInstance();
	      ParsePosition pos = new ParsePosition(0);
	      d = formatter.parse(value, pos);
	      if (d != null && pos.getIndex() == value.length()) {
	        return d;
	      }
	    }
	    throw new IllegalArgumentException("Unable to parse the date: " + value);
	  }

	  public static final String asSubmissionDateTimeString(Date d) {
	    if (d == null)
	      return null;
	    return DateUtils.formatDateTime(d, DateUtils.FORMAT_ISO8601);
	  }

	  public static final String asSubmissionDateOnlyString(Date d) {
	    if (d == null)
	      return null;
	    return DateUtils.formatDate(d, DateUtils.FORMAT_ISO8601);
	  }

	  public static final String asSubmissionTimeOnlyString(Date d) {
	    if (d == null)
	      return null;
	    return DateUtils.formatTime(d, DateUtils.FORMAT_ISO8601);
	  }

	public static final List<AuthScope> buildAuthScopes(String host) {
		List<AuthScope> asList = new ArrayList<AuthScope>();

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

	public static final void clearAllCredentials(HttpClientContext localContext) {
		CredentialsProvider credsProvider = localContext.getCredentialsProvider();
		if ( credsProvider != null ) {
		  credsProvider.clear();
		}
	}

	public static final boolean hasCredentials(HttpClientContext localContext,
			String userEmail, String host) {
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

	public static final void addCredentials(HttpClientContext localContext,
			String userEmail, char[] password, String host) {
		Credentials c = new UsernamePasswordCredentials(userEmail, new String(password));
		addCredentials(localContext, c, host);
	}

	private static final void addCredentials(HttpClientContext localContext,
			Credentials c, String host) {
		CredentialsProvider credsProvider = localContext.getCredentialsProvider();

		List<AuthScope> asList = buildAuthScopes(host);
		for (AuthScope a : asList) {
			credsProvider.setCredentials(a, c);
		}
	}

	private static final void setOpenRosaHeaders(HttpRequest req) {
		req.setHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION);
		req.setHeader(DATE_HEADER,
				org.apache.http.client.utils.DateUtils.formatDate(new Date(), org.apache.http.client.utils.DateUtils.PATTERN_RFC1036));
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
	  SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(timeout).build();
	  
     // if possible, bias toward digest auth (may not be in 4.0 beta 2)
     List<String> targetPreferredAuthSchemes = new ArrayList<String>();
     targetPreferredAuthSchemes.add(AuthSchemes.DIGEST);
     targetPreferredAuthSchemes.add(AuthSchemes.BASIC);

     RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
	      .setConnectTimeout(timeout)
	      // support authenticating
	      .setAuthenticationEnabled(true)
	      // support redirecting to handle http: => https: transition
	      .setRedirectsEnabled(true)
	      .setMaxRedirects(1)
	      .setCircularRedirectsAllowed(true)
	      .setTargetPreferredAuthSchemes(targetPreferredAuthSchemes)
	      .build();
	
      CloseableHttpClient httpClient = HttpClientBuilder.create()
          .setDefaultSocketConfig(socketConfig)
          .setDefaultRequestConfig(requestConfig).build();

      return httpClient;
	}

	public static HttpClientContext createHttpContext() {
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
