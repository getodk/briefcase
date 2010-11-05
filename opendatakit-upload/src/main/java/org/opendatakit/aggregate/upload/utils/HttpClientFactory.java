/*
 * Copyright (C) 2010 University of Washington.
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
package org.opendatakit.aggregate.upload.utils;

import java.net.URL;
import java.util.logging.Logger;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpClientFactory {

	/**
	 * Return an http client that is able to communicate with the protocol and
	 * port contained in the given url (http and 80 are assumed if none found).
	 * Set up to be threadsafe and have a 30 second timeout.
	 * 
	 * @param url
	 *            an example url the HttpClient should be able to communicate
	 *            with
	 * @param logger the Logger to use to log messages. May be null.
	 * @return the HttpClient
	 */
	public static HttpClient getHttpClient(URL url, Logger logger)
	{
		// configure connection
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 30000);
        HttpConnectionParams.setSoTimeout(params, 30000);
        HttpClientParams.setRedirecting(params, false);
        // setup client
        SchemeRegistry registry = new SchemeRegistry();
        String scheme = url.getProtocol();
        if(scheme == null)
        {
        	scheme = "http";
        	if (logger != null)
        		logger.info("Assuming protocol to be http.");
        }
        int port = url.getPort();
        if (port == -1)
        {
        	port = 80;
        	if (logger != null)
        		logger.info("Assuming port to be 80.");
        }
        registry.register(new Scheme(scheme, PlainSocketFactory.getSocketFactory(), port));
        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
        return new DefaultHttpClient(manager, params);
	}
}
