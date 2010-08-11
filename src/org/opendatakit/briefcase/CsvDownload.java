/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.briefcase;

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
// import java.util.zip.*;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The implementation of the Briefcase functionality which uses ODK
 * Aggregate's /csvFragment and /binaryData interfaces to 
 * extract data from Aggregate into one or more local csv files and 
 * multiple local binary files.
 * 
 * @author mitchellsundt@gmail.com
 */
public class CsvDownload {
	
	/**
	 * Notification interface to inform our creator what 
	 * URL we are currently fetching data from. 
	 */
	public interface ActionListener {
		/**
		 * Called just before a URL connection is attempted.
		 * 
		 * @param url what we are attempting to connect to.
		 * @param tries number of times we have retried connection.
		 * @param count number of cursor fetches for this odkId.
		 */
		public void beforeFetchUrl( String url, int tries, int count ); 
	}

	/** version of this application */
	public static final String APP_VERSION = "1.0";

	/** content type for html page -- returned if not logged in */
	private static final String TEXT_HTML_CONTENT_TYPE = "text/html";
	/** content type for xml data */
	private static final String TEXT_XML_CONTENT_TYPE = "text/xml";
	/** content encoding expected for xml data */
	private static final String UTF_8 = "UTF-8";

	/**
	 * Directory names created underneath the destination directory. Constants
	 * below should be substituted to obtain the actual directory names. The
	 * abstract structure is:
	 * 
	 * destinationDir/CSV_DATA_DIRECTORY destinationDir/BINARY_DATA_DIRECTORY
	 * destinationDir/MANIFEST_FILE_NAME
	 * 
	 */
	/** csv data file directory */
	static final String CSV_DATA_DIRECTORY = "csvData";
	/** binary data file directory */
	static final String BINARY_DATA_DIRECTORY = "binaryData";
	static final String RELATIVE_PATH_PARENT_DIR = "../";
	static final String DOT = ".";
	static final String MANIFEST_FILE_NAME = "Manifest.txt";

	/** mapping of content type for binary data to file extensions */
	private static final Map<String, String> mediaExtensions;
	static {
		mediaExtensions = new HashMap<String, String>();
		mediaExtensions.put("audio/3gpp", ".3gpp");
		mediaExtensions.put("video/3gpp", ".3gpp");
		mediaExtensions.put("image/jpeg", ".jpeg");
	}

	/** final path element for accessing csv Fragments on ODK Aggregate */
	private static final String CSV_FRAGMENT_URL_PATH = "csvFragment";
	/** final path element for accessing binary data on ODK Aggregate */
	private static final String BINARY_DATA_URL_PATH = "binaryData";

	/**
	 * Enum values returned when analyzing content to determine if it contains a
	 * valid ODK Aggregate URL.
	 */
	enum UrlType {
		UNKNOWN_URL, BINARY_DATA_URL, CSV_FRAGMENT_URL
	};

	/**
	 * Enum values of the parsing states when extracting the list of string
	 * values from a csv-encoded string.
	 */
	enum ParseState {
		EXPECT_START, IN_BODY, EXPECT_COMMA_OR_EOS
	};

	/**
	 * Parameters on a csvFragment request
	 */

	/** parameter to specify the key set reference to fetch */
	public static final String ODK_ID = "odkId";
	/** parameter to specify how many records to fetch at one time */
	public static final String NUM_ENTRIES = "numEntries";
	/** parameter to specify a cursor to resume fetching from */
	public static final String CURSOR = "cursor";

	/** default number of entries to fetch at one time */
	private static final int DEFAULT_NUM_ENTRIES = 500;

	/**
	 * Pieces of a HTTP request
	 */
	/** slash */
	private static final String SLASH = "/";
	private static final String PARAM_DELIMITER = "&";
	private static final String BEGIN_PARAM = "?";
	private static final String EMPTY_STRING = "";
	private static final String EQUALS = "=";

	/**
	 * XML namespace, tags, etc. for csvFragment
	 * 
	 * <pre>
	 * @code
	 * <entries>
	 *    <cursor>...</cursor> <!-- only present if additional records may be fetched -->
	 *    <header>...</header> <!-- csv -- property names -->
	 *    <result>...</result> <!-- csv -- values -- repeats 0 or more times -->
	 * </entries>
	 * }
	 * </pre>
	 */
	/** namespace is empty for the xml returned from ODK Aggregate */
	private static final String XML_TAG_NAMESPACE = EMPTY_STRING;
	private static final String XML_TAG_ENTRIES = "entries";
	private static final String XML_TAG_CURSOR = "cursor";
	private static final String XML_TAG_HEADER = "header";
	private static final String XML_TAG_RESULT = "result";

	/** csv constants */
	/** comma */
	private static final String COMMA = ",";
	private static final String QUOTE_QUOTE = "\"\"";
	private static final String QUOTE = "\"";
	private static final char COMMA_CHAR = ',';
	private static final char QUOTE_CHAR = '\"';

	/** special result field names */
	/** self-key */
	private static final String KEY = "KEY";
	/** parent-key */
	private static final String PARENT_KEY = "PARENT_KEY";

	/**
	 * Enumeration for ways to handle the binary data URL
	 */
	public static enum BinaryDataTreatment {
		DOWNLOAD_BINARY_DATA,
		REPLACE_WITH_LOCAL_FILENAME,
		RETAIN_BINARY_DATA_URL
	}
	
	/***************************************************************************
	 * Member variables
	 **************************************************************************/

	/**
	 * Simple struct for data relating to an open file (manifest or csv)
	 */
	private static final class ActiveFileInfo {
		/** csv or manifest file */
		File file;
		/** writer to the file */
		OutputStreamWriter writer;
		/** the last "nextCursor" value for this data (null if not csv) */
		String lastCursor = null;
		/** the last "KEY" value that was successfully retrieved (null if not csv) */
		String lastKey = null;
	};

	/**
	 * Map of all the currently-open csv files. This is keyed off of the
	 * extractKeyPath(key url)
	 */
	private Map<String, ActiveFileInfo> openCsvFiles = new HashMap<String, ActiveFileInfo>();

	/**
	 * Constants across all fetch actions.
	 */

	/** the base URL of the server. e.g., http://localhost:8888/App/ */
	private final String serverUrl;
	/** common base directory for binary, csv and manifest files */
	private final File baseDir;
	/** the directory where binary data is placed */
	private final File binaryDataDir;
	/** the directory where the various csv files are placed */
	private final File csvDataDir;
	/** the manifest file information -- updated at the end of the fetch */
	private final ActiveFileInfo manifest = new ActiveFileInfo();
	/** callback for reporting activity to the ui layer */
	private final ActionListener uiNotify;
	
	/*************************************************************************
	 * (static) methods that don't depend upon member variables *
	 **********************************************************************/

	/**
	 * Classification function to determine if a URL is for a binary data item,
	 * a repeated group, or an unknown datum.
	 * 
	 * @param unknownUrl
	 * @return
	 */
	static UrlType classifyUrl(String unknownUrl) {
		try {
			URL url = new URL(unknownUrl);
			String path = url.getPath();
			int idx = path.lastIndexOf('/');
			while (idx != -1 && idx == path.length() - 1) {
				// trailing slash -- remove it
				path = path.substring(0, idx);
				idx = path.lastIndexOf('/');
			}
			if (idx != -1) {
				path = path.substring(idx + 1);
			}
			// path is now the last portion of the path url.
			if (BINARY_DATA_URL_PATH.equals(path))
				return UrlType.BINARY_DATA_URL;
			if (CSV_FRAGMENT_URL_PATH.equals(path))
				return UrlType.CSV_FRAGMENT_URL;
		} catch (Exception e) {
		}
		return UrlType.UNKNOWN_URL;
	}

	/**
	 * Given a URL for a form or repeated group, return the cursor or null.
	 * 
	 * @param keyUrl
	 * @return
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	static String extractCursor(String keyUrl) throws MalformedURLException,
			UnsupportedEncodingException {

		URL url = new URL(keyUrl);
		String query = url.getQuery();
		String cursor = null;
		for (String kvPair : query.split(PARAM_DELIMITER)) {
			String[] pair = kvPair.split(EQUALS);
			String key = URLDecoder.decode(pair[0], UTF_8);
			String value = URLDecoder.decode(pair[1], UTF_8);
			if (key.equals(CURSOR)) {
				if (cursor != null) {
					throw new IllegalArgumentException(CURSOR
							+ " occurs more than once in supplied url: "
							+ keyUrl);
				}
				cursor = value;
			}
		}
		return cursor;
	}

	/**
	 * Given a URL for a form or repeated group, return the key reference.
	 * 
	 * @param keyUrl
	 * @return
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	static String extractKeyReference(String keyUrl)
			throws MalformedURLException, UnsupportedEncodingException {

		URL url = new URL(keyUrl);
		String query = url.getQuery();
		String odkId = null;
		for (String kvPair : query.split(PARAM_DELIMITER)) {
			String[] pair = kvPair.split(EQUALS);
			String key = URLDecoder.decode(pair[0], UTF_8);
			String value = URLDecoder.decode(pair[1], UTF_8);
			if (key.equals(ODK_ID)) {
				if (odkId != null) {
					throw new IllegalArgumentException(ODK_ID
							+ " occurs more than once in supplied url: "
							+ keyUrl);
				}
				odkId = value;
			}
		}
		if (odkId == null) {
			throw new IllegalArgumentException(ODK_ID
					+ " not found in supplied url: " + keyUrl);
		}
		return odkId;
	}

	/**
	 * Given a key reference, flatten it to a key path.
	 * 
	 * @param keyReference
	 * @return
	 */
	static String flattenKeyReferenceToKeyPath(String keyReference) {
		// OK we have the value for the keyReference. Now we need to
		// reduce it to its path elements, remove any [@key="..."]
		// filters and reassemble it.
		StringBuilder b = new StringBuilder();
		boolean firstTime = true;
		for (String element : keyReference.split(SLASH)) {
			// emit slash on non-first times
			if (!firstTime)
				b.append(SLASH);
			firstTime = false;

			// examine element and remove any brackets
			int idx = element.indexOf('[');
			if (idx != -1) {
				element = element.substring(0, idx);
			}
			b.append(element);
		}
		return b.toString();
	}

	/**
	 * Given a csv-encoded string, return the list of String objects that it
	 * encodes. Note that the list may contain nulls.
	 * 
	 * @param csv
	 * @return List<String> of values
	 */
	static List<String> splitCsvRow(String csv) {
		List<String> values = new ArrayList<String>();
		ParseState state = ParseState.EXPECT_START;
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < csv.length(); ++i) {
			char ch = csv.charAt(i);
			switch (state) {
			case EXPECT_START:
				if (ch == QUOTE_CHAR) {
					state = ParseState.IN_BODY;
				} else if (ch == COMMA_CHAR) {
					// value is null
					values.add(null);
					state = ParseState.EXPECT_START;
				} else {
					throw new IllegalStateException(
							"Expected double quote while parsing csv; got "
									+ ch);
				}
				break;
			case IN_BODY:
				if (ch == QUOTE_CHAR) {
					if ((i + 1) < csv.length()
							&& csv.charAt(i + 1) == QUOTE_CHAR) {
						// emit this character -- embedded double quote.
						// advance beyond the double-occurrence of this embedded
						// double quote.
						b.append(ch);
						++i;
					} else {
						// we are at end of entry.
						// save string in values[]
						// reset builder.
						// change state
						values.add(b.toString());
						b.setLength(0);
						state = ParseState.EXPECT_COMMA_OR_EOS;
					}
				} else {
					// just a regular character
					b.append(ch);
				}
				break;
			case EXPECT_COMMA_OR_EOS:
				if (ch == COMMA_CHAR) {
					state = ParseState.EXPECT_START;
				} else {
					throw new IllegalStateException(
							"Expected comma while parsing csv; got " + ch);
				}
			}
		}
		// if we end in a EXPECT_START parse state, 
		// then the last field of the csv line is a null.
		if ( state == ParseState.EXPECT_START) {
			values.add(null);
		} else if (state != ParseState.EXPECT_COMMA_OR_EOS) {
			throw new IllegalStateException(
					"Reached end of csv line but parser state is invalid");
		}
		return values;
	}

	/**
	 * Create the csv row with proper doubling of embedded quotes.
	 * 
	 * @param values
	 *            string values to be separated by commas
	 * @return string containing comma separated values
	 */
	static String generateCommaSeparatedRow(List<String> values) {
		StringBuilder row = new StringBuilder();
		boolean first = true;
		for (String original : values) {
			// if not the first time through, prepend a comma
			if (!first)
				row.append(COMMA);
			first = false;
			// replace all quotes in the string with doubled-quotes
			// then wrap the whole thing with quotes. Nulls are
			// distinguished from empty strings by the lack of a
			// value in that position (e.g., ,, vs ,"",)
			if (original != null) {
				row.append(QUOTE);
				row.append(original.replace(QUOTE, QUOTE_QUOTE));
				row.append(QUOTE);
			}
		}
		return row.toString();
	}

	/**
	 * Convert a key path to a proper csv filename.
	 * 
	 * @param normalizedSelfKeyPath
	 * @return
	 */
	static String fileNameFromKeyPath(String normalizedSelfKeyPath) {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		List<String> elements = new ArrayList<String>(Arrays.asList(normalizedSelfKeyPath.split(SLASH)));
		Collections.reverse(elements);
		
		for (String s : elements) {
			if (!first)
				b.append(".");
			first = false;
			b.append(s);
		}
		b.append(".csv");
		return b.toString();
	}

	/**
	 * Formatter for directory-of-file-related messages.
	 * 
	 * @param format
	 *            valid MessageFormat string where {0} will be substituted with
	 *            the path of the dof.
	 * @param dof
	 *            File object of the directory-or-file
	 * @return formatted string
	 */
	static String dofExceptionString(String format, File dof) {
		String path = null;
		try {
			path = dof.getCanonicalPath();
		} catch (IOException eIgnore) {
			path = dof.getPath();
		}
		Object[] args = { path };
		return MessageFormat.format(format, args);
	}

	/**
	 * Constructs the name of a binary data file on the local file system. The
	 * name is relative to the csv directory. The name is constructed such that
	 * a given url will always return the same filename.
	 * 
	 * @param columnName
	 * @param url
	 * @param contentType
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	static String getBinaryCsvRelativeFileName(String columnName, String url,
			String contentType) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		String extension = mediaExtensions.get(contentType);

		if (extension == null) {
			throw new IllegalStateException(
					"Unrecognized binary content type: " + contentType);
		}

		StringBuilder b = new StringBuilder();
		b.append(RELATIVE_PATH_PARENT_DIR);
		b.append(BINARY_DATA_DIRECTORY);
		b.append(SLASH);
		b.append(columnName);
		b.append(DOT);

		// construct an md5 hash of the url; use that as part of the filename
		MessageDigest m = MessageDigest.getInstance("MD5");
		byte[] bytesOfUrl = url.getBytes(UTF_8);
		BigInteger bi = new BigInteger(1, m.digest(bytesOfUrl));
		b.append(String.format("%1$032x", bi));

		b.append(extension);
		return b.toString();
	}

	/**
	 * Constructor.
	 * 
	 * Throws an exception if the directory structure rooted at the
	 * destinationDirectoryName cannot be created or accessed or if the manifest
	 * file within that directory structure exists or cannot be created.
	 * 
	 * @param serverUrl
	 * @param destinationDirectoryName
	 * @param uiUpdate a listener for reporting what we are currently working on...
	 * @throws IllegalArgumentException
	 */
	public CsvDownload(final String serverUrlArg,
			final String destinationDirectoryName,
			ActionListener uiNotifyArg ) {

		uiNotify = uiNotifyArg; 
		
		// ensure that serverUrl looks like a http string and ends in slash
		if (!serverUrlArg.endsWith(SLASH)) {
			serverUrl = serverUrlArg + SLASH;
		} else {
			serverUrl = serverUrlArg;
		}

		try {
			URL u = new URL(serverUrl);
			if (!u.getProtocol().equals("http")
					&& !u.getProtocol().equals("https")) {
				throw new IllegalArgumentException("server URL (" + serverUrl
						+ ") must begin either http: or https:");
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("server URL (" + serverUrl
					+ " is invalid", e);
		}

		// ensure that the destination directory name exists.
		baseDir = new File(destinationDirectoryName);

		if (baseDir.exists()) {
			if (!baseDir.isDirectory()) {
				throw new IllegalArgumentException(dofExceptionString(
						"destination directory ({0}) is not a directory",
						baseDir));
			}
		} else {
			if (!baseDir.mkdirs()) {
				throw new IllegalArgumentException(dofExceptionString(
						"destination directory ({0}) could not be created",
						baseDir));
			}
		}

		// ensure that there is a binaryData directory. This will hold all
		// binary data
		// for the retrieved rows.
		binaryDataDir = new File(baseDir, BINARY_DATA_DIRECTORY);
		if (binaryDataDir.exists()) {
			if (!binaryDataDir.isDirectory()) {
				throw new IllegalArgumentException(dofExceptionString(
						"{0} exists but is not a directory", binaryDataDir));
			}
		} else {
			if (!binaryDataDir.mkdir()) {
				throw new IllegalArgumentException(dofExceptionString(
						"{0} could not be created", binaryDataDir));
			}
		}

		csvDataDir = new File(baseDir, CSV_DATA_DIRECTORY);
		if (csvDataDir.exists()) {
			if (!csvDataDir.isDirectory()) {
				throw new IllegalArgumentException(dofExceptionString(
						"{0} exists but is not a directory", csvDataDir));
			}
		} else {
			if (!csvDataDir.mkdir()) {
				throw new IllegalArgumentException(dofExceptionString(
						"{0} could not be created", csvDataDir));
			}
		}

		// Ensure that a manifest file does not exist in the local directory.
		// The program will write information in this file to aid other programs
		// in
		// manipulating the csv data.
		this.manifest.file = new File(baseDir, MANIFEST_FILE_NAME);
		if (this.manifest.file.exists()) {
			throw new IllegalArgumentException(
					dofExceptionString(
							MANIFEST_FILE_NAME
									+ " already exists in the destination directory ({0})",
							baseDir));

		}

		// Begin writing to the manifest file.
		// Keep this open until the end of the fetch.
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(this.manifest.file, false);
			this.manifest.writer = new OutputStreamWriter(fs, UTF_8);
			Calendar aCalendar = Calendar.getInstance(Locale.getDefault());
			DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
			Object[] args = { APP_VERSION, df.format(aCalendar.getTime()), serverUrl };
			this.manifest.writer.write(MessageFormat.format(
					"BriefcaseVersion: {0}\nRunDate: {1}\nServerUrl: {2}\n",
					args));
		} catch (IOException e) {
			try {
				this.manifest.writer.close();
			} catch (IOException eIgnore) {
			}
			;
			try {
				fs.close();
			} catch (IOException eIgnore) {
			}
			;

			throw new IllegalArgumentException(dofExceptionString(
					"{0} could not be written", this.manifest.file), e);
		}
	}

	/**
	 * Called after fetch is complete. Writes the odkId and fileName mappings to
	 * the manifest and closes all open files (including the manifest itself).
	 */
	public void closeAllFilesAndManifest(boolean success) {
		int i = 1;
		// write the manifest file...
		try {
			int baseLen = baseDir.getCanonicalPath().length();

			// the keys into openCsvFiles[] are normalized key paths.
			// the shortest of these must have been the initial query.
			int shortestKeyLength = Integer.MAX_VALUE;
			for (Map.Entry<String, ActiveFileInfo> ei : openCsvFiles.entrySet()) {
				if ( ei.getKey().length() < shortestKeyLength ) {
					shortestKeyLength = ei.getKey().length();
				}
			}
			
			for (Map.Entry<String, ActiveFileInfo> ei : openCsvFiles.entrySet()) {
				ActiveFileInfo fi = ei.getValue();
				
				Object[] args = {
						Integer.valueOf(i),
						ei.getKey(),
						fi.file.getCanonicalPath().substring(baseLen) };
				
				manifest.writer.write(MessageFormat.format(
						"OdkId-{0}: {1}\nCsvFileName-{0}: {2}\n", args));
				
				if ( ei.getKey().length() == shortestKeyLength ) {
					// output the LastCursor and LastKEY values to support restarts
					Object[] tlArgs = {
						Integer.valueOf(i),
						fi.lastCursor,
						fi.lastKey};
					
					manifest.writer.write(MessageFormat.format(
						"LastCursor-{0}: {1}\nLastKEY-{0}: {2}\n", tlArgs));
				}
				++i;
			}
			manifest.writer.write("Completion-Status: " + (success ? "Success" : "Failure") + "\n");
			manifest.writer.close();
		} catch (IOException eIgnore) {
		}

		// close all the open csv files...
		for (Map.Entry<String, ActiveFileInfo> ei : openCsvFiles.entrySet()) {
			try {
				ei.getValue().writer.close();
			} catch (IOException eIgnore) {
			}
		}
	}

	/**
	 * Constructs the URL for accessing a csv fragment from the ODK Aggregate
	 * server identified in the constructor to this object.
	 * 
	 * @param properties
	 *            parameters for the request
	 * @return request URL to the ODK Aggregate instance
	 */
	public String createCsvFragmentLinkWithProperties(
			Map<String, String> properties) {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(serverUrl); // already ends in slash...
		urlBuilder.append(CSV_FRAGMENT_URL_PATH);
		if (properties != null) {
			Set<Map.Entry<String, String>> propSet = properties.entrySet();
			boolean firstParam = true;
			for (Map.Entry<String, String> property : propSet) {
				String value = property.getValue();
				if (value != null) {
					if (firstParam) {
						urlBuilder.append(BEGIN_PARAM);
						firstParam = false;
					} else {
						urlBuilder.append(PARAM_DELIMITER);
					}

					String valueEncoded;
					try {
						valueEncoded = URLEncoder.encode(value, UTF_8);
					} catch (UnsupportedEncodingException e) {
						valueEncoded = EMPTY_STRING;
					}
					urlBuilder
							.append(property.getKey() + EQUALS + valueEncoded);
				}
			}
		}
		return urlBuilder.toString();
	}

	/**
	 * Main entry point.  Initiates the retrieval of data from the 
	 * given url. 
	 * 
	 * @param urlArg the full url for the data fetch.  This may have a cursor specified.
	 * @param fetchBinaryData action to take when binary data URLs are encountered.
	 * @param fetchRecursively true if repeated groups should be fetched (recursively).
	 * @param skipBeforeKey
	 * 			if non-null, skip over records prior or equal to this KEY.  Then 
	 * 			begin normal processing.
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	public void fetchCsvRecursively(String urlArg, BinaryDataTreatment fetchBinaryData,
			boolean fetchRecursively, String skipBeforeKey) throws IOException, NoSuchAlgorithmException {
		//
		// if we are pulling a form with this odkId structure of repeat groups:
		// HouseholdSurvey/HouseholdSurvey1
		// HouseholdSurvey/HouseholdSurvey1/ChildrenOfHousehold
		// HouseholdSurvey/HouseholdSurvey1/ChildrenOfHousehold/ToysOfChild
		// 
		// The resulting file structure is:
		//
		// destinationDirectory/
		// destinationDirectory/README.txt
		// destinationDirectory/binaryData/
		// destinationDirectory/csvData/HouseholdSurvey.HouseholdSurvey1.csv
		// destinationDirectory/csvData/ChildrenOfHousehold.HouseholdSurvey.HouseholdSurvey1.csv
		// destinationDirectory/csvData/ToysOfChild.ChildrenOfHousehold.HouseholdSurvey.HouseholdSurvey1.csv
		// 
		// Note that all the repeating groups are separate csv files in a
		// single csvData directory. Their names are a concatenation of the
		// repeating group names and the top-level form name (e.g.,
		// 'HouseholdSurvey1') in reverse order. The manifest file will
		// identify the correspondence to the repeat groups so that if
		// a repeat group name contains a period, we can still reconstruct
		// the odkId path.
		//
		// Note that all binary data from all nested repeated groups is
		// collapsed into the single binaryData directory. The csv files
		// will contain a relative directory reference to these files: e.g.,
		// "../binaryData/binaryFileName.jpeg"
		// 
		// The KEY and PARENT_KEY fields are not patched in these files, but
		// refer to the original data in the Aggregate instance for
		// traceability.

		// If the odkId does not include a slash, perform a fetch of a
		// single record to determine the actual odkId. This is needed
		// to maintain the directory structure in our local tree.
		String odkId = extractKeyReference(urlArg);
		String nextCursor = extractCursor(urlArg);

		int count = 0;
		int numEntries = DEFAULT_NUM_ENTRIES;

		do {
			String fullUrl;
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put(ODK_ID, odkId);
				params.put(NUM_ENTRIES, Integer.toString(numEntries));
				params.put(CURSOR, nextCursor);

				fullUrl = createCsvFragmentLinkWithProperties(params);
			}

			URL url = new URL(fullUrl);
			uiNotify.beforeFetchUrl(fullUrl, 1, ++count);
			// assume http for now...
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = null;
			int outcome = conn.getResponseCode();
			String contentType = conn.getContentType();
			if (contentType.startsWith(TEXT_HTML_CONTENT_TYPE)) {
				InputStreamReader ir = new InputStreamReader(conn.getInputStream());
				final int MAX_CHARS = 1024;
				char[] data = new char[MAX_CHARS];
				int nChar = ir.read(data);
				ir.close();
				String str = new String(data, 0, nChar);
				throw new IllegalStateException(
						"Html received (" + Integer.toString(outcome) +
						") - expected Xml.  Are you logged in?  Is odkId correct?\nBody:\n"+ str);
			} else if (!contentType.startsWith(TEXT_XML_CONTENT_TYPE)) {
				throw new IllegalStateException(
						"Unexpected non-xml content: " + contentType + " received ("+
						Integer.toString(outcome) + ")");
			} else {
				KXmlParser p = new KXmlParser();
				is = conn.getInputStream();
				try {
					p.setInput(is, UTF_8);

					String headerRow;
					String dataRow;
					List<String> headerValues = null;

					p.nextTag();
					p.require(XmlPullParser.START_TAG, XML_TAG_NAMESPACE,
							XML_TAG_ENTRIES);
					p.nextTag();
					if (XML_TAG_CURSOR.equals(p.getName())) {
						p.require(XmlPullParser.START_TAG, XML_TAG_NAMESPACE,
								XML_TAG_CURSOR);
						if ( XmlPullParser.TEXT != p.next()) {
							throw new IllegalStateException(
									"Expected cursor value at line " + p.getLineNumber() );
						}
						nextCursor = p.getText();
						p.next();
						p.require(XmlPullParser.END_TAG, XML_TAG_NAMESPACE,
								XML_TAG_CURSOR);
						p.nextTag();
					} else {
						nextCursor = null;
					}
					
					if (XML_TAG_HEADER.equals(p.getName())) {
						p.require(XmlPullParser.START_TAG, XML_TAG_NAMESPACE,
								XML_TAG_HEADER);
						if ( XmlPullParser.TEXT != p.next()) {
							throw new IllegalStateException(
									"Expected header csv at line " + p.getLineNumber() );
						}
						headerRow = p.getText();
						headerValues = splitCsvRow(headerRow);
						p.next();
						p.require(XmlPullParser.END_TAG, XML_TAG_NAMESPACE,
								XML_TAG_HEADER);
						
					} else {
						throw new IllegalStateException("Expected "
								+ XML_TAG_HEADER + " but found " + p.getName());
					}

					while ( p.nextTag() != XmlPullParser.END_TAG) {
						p.require(XmlPullParser.START_TAG, XML_TAG_NAMESPACE,
								XML_TAG_RESULT);
						if ( XmlPullParser.TEXT != p.next()) {
							throw new IllegalStateException(
									"Expected result csv at line " + p.getLineNumber() );
						}
						dataRow = p.getText();
						p.next();
						p.require(XmlPullParser.END_TAG, XML_TAG_NAMESPACE,
								XML_TAG_RESULT);

						/**
						 * If skipBeforeKey is non-null, processCsvRow(...) is just 
						 * a predicate function returning false until the current row
						 * KEY matches the skipBeforeKey.  When that occurs, we set
						 * the skipBeforeKey to null so that subsequent rows can be 
						 * processed normally.
						 */
						boolean keyMatch = processCsvRow(headerValues, splitCsvRow(dataRow),
								nextCursor, fetchBinaryData, fetchRecursively, skipBeforeKey);
						if ( keyMatch ) skipBeforeKey = null;
					}

					p.require(XmlPullParser.END_TAG, XML_TAG_NAMESPACE,
							XML_TAG_ENTRIES);
					while (p.next() == XmlPullParser.IGNORABLE_WHITESPACE)
						;
					p.require(XmlPullParser.END_DOCUMENT, null, null );

				} catch (XmlPullParserException e) {
					throw new IllegalStateException(
							"Unexpected xml parsing error", e);
				}
				is.close();
			}
		} while (nextCursor != null);
	}

	/**
	 * Attempts to access the binary data from the given url. If successful,
	 * the filename where the binary data should be locally stored is returned.
	 * This filename is relative to the csv file directory, making it suitable
	 * for direct inclusion into the csv file itself.  We need to initiate 
	 * access to the stored data through the url in order to determine the
	 * contentType of the data and map that to a filetype.
	 * <p>
	 * If requested, the binary data itself is downloaded and stored to this 
	 * file and the File object is added to the list of binary files that 
	 * should be deleted if the processing of a data row fails.
	 * 
	 * @param columnName
	 *            referencing the binary data.
	 * @param url
	 * 				full url of the binaryData on the Aggregate server.
	 * @param doFetch
	 * 				true if the fetch of the binary data file should be done
	 * 				otherwise, no fetch occurs.
	 * @param binaryFiles
	 *            list of downloaded binary files. Maintained for failure
	 *            clean-up purposes.
	 * @return the relative pathname of the binary data file (relative to csv
	 *         file directory).
	 * @throws NoSuchAlgorithmException
	 */
	private String fetchBinaryUrl(String columnName, String url, boolean doFetch,
			List<File> binaryFiles) throws NoSuchAlgorithmException {

		String binaryFileName = null;
		File f = null;
		FileOutputStream fos = null;
		InputStream in = null;

		try {
			URL mediaUrl = new URL(url);
			uiNotify.beforeFetchUrl(url, 1, 1);
			URLConnection uc = mediaUrl.openConnection();
			String contentType = uc.getContentType();
			int contentLength = uc.getContentLength();

			binaryFileName = getBinaryCsvRelativeFileName(columnName, url,
					contentType);

			if ( !doFetch ) {
				// we should still close the stream...
				uc.getInputStream().close();
				return binaryFileName;
			}
			
			f = new File(csvDataDir, binaryFileName);

			if (f.exists()) {
				if (!f.delete())
					throw new IllegalStateException(
							"Binary data file already exists: "
									+ f.getCanonicalPath());
			}

			fos = new FileOutputStream(f);

			// grab the media from the url
			in = new BufferedInputStream(uc.getInputStream());

			int expectedLength = uc.getContentLength();

			final int MAX_BYTES = 4096;
			byte[] data = new byte[MAX_BYTES];

			int bytesRead = 0;
			int totalBytesRead = 0;
			while ((contentLength == -1) || (totalBytesRead < expectedLength)) {
				bytesRead = in.read(data, 0, MAX_BYTES);
				if (bytesRead != -1) {
					fos.write(data, 0, bytesRead);
					totalBytesRead += bytesRead;
				} else {
					break;
				}
			}

			if ((expectedLength != -1) && (totalBytesRead != expectedLength)) {
				throw new IllegalStateException(
						"Unexpected loop exit while fetching binary data");
			}

			in.close();
			fos.close();
		} catch (IOException e) {
			// delete binary file if it was partially written...
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e1) {
					// ignore
				}
			}
			f.delete();
			f = null;
			try {
				in.close();
			} catch (IOException e1) {
				// ignore
			}
		}
		if (f != null) {
			binaryFiles.add(f);
		}
		return binaryFileName;
	}

	/**
	 * Processes a csv row.  This may entail fetching binary data
	 * or recursively fetching repeated groups defined within the row.
	 * The row is appended to the csv file.
	 * 
	 * @param tableHeader list of column names (Xform tag names)
	 * @param tableContents list of column values (values of these Xform tags)
	 * @param nextCursor the most recent nextCursor value
	 * @param fetchBinaryData action to take when binary data links are encountered.
	 * @param fetchRecursively true if repeated groups should be fetched (recursively).
	 * @param skipBeforeKey
	 * 			if non-null, all processing of the row is skipped
	 * 			and this routine acts as a predicate function, returning
	 *          true iff the current KEY matches the skipBeforeKey.
	 * @return true iff skipBeforeKey is non-null and matches the current KEY
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private boolean processCsvRow(List<String> tableHeader,
			List<String> tableContents, String nextCursor,
			BinaryDataTreatment fetchBinaryData, boolean fetchRecursively,
			String skipBeforeKey)
			throws IOException, NoSuchAlgorithmException {

		if (tableHeader.size() != tableContents.size()) {
			throw new IllegalArgumentException(
					"csv header and data rows are not the same length");
		}

		// track the binary data files in case we need to delete them
		List<File> binaryDataFiles = new ArrayList<File>();
		try {
			// iLast will become the last field we examine for csvFragment and
			// binaryData urls.
			int iLast = tableContents.size() - 1; // should be the KEY field
			assert (KEY.equals(tableHeader.get(iLast)));// confirm that this is
														// the KEY field.
			final String rawKEY = tableContents.get(iLast);
			if (skipBeforeKey != null) {
				return skipBeforeKey.equals(rawKEY);
			}
			
			// figure out the normalized self key path
			final String normalizedSelfKeyPath = flattenKeyReferenceToKeyPath(extractKeyReference(rawKEY));

			if (normalizedSelfKeyPath == null) {
				throw new IllegalStateException(
						"csv row did not contain a valid " + KEY + " column.");
			}

			// we expect a PARENT_KEY field if there are two or more slashes in
			// the normalized self key path
			final boolean hasParent = (normalizedSelfKeyPath.indexOf(SLASH) != normalizedSelfKeyPath
					.lastIndexOf(SLASH));
			// if we have a parent key, we don't want to fetch csv data for it.
			if (hasParent) {
				--iLast;
				assert (PARENT_KEY.equals(tableHeader.get(iLast)));// confirm
																	// that this
																	// is the
																	// PARENT_KEY
																	// field.
			}

			// fetch all binaryData
			// fetch all csv data recursively (excluding KEY and PARENT_KEY
			// fields)
			for (int i = 0; i < iLast; ++i) {
				final String value = tableContents.get(i);

				switch (classifyUrl(value)) {
				case BINARY_DATA_URL:
					String binaryRelative;
					switch (fetchBinaryData) {
					case DOWNLOAD_BINARY_DATA:
						// fetch the binary data
						// update row value to reference that data file.
						binaryRelative = fetchBinaryUrl(tableHeader
								.get(i), value, true, binaryDataFiles);
						tableContents.set(i, binaryRelative);
						break;
					case REPLACE_WITH_LOCAL_FILENAME:
						// fetch the binary data
						// update row value to reference that data file.
						binaryRelative = fetchBinaryUrl(tableHeader
								.get(i), value, false, binaryDataFiles);
						tableContents.set(i, binaryRelative);
						break;
					case RETAIN_BINARY_DATA_URL: // no-op
						break;
					}
					break;
				case CSV_FRAGMENT_URL:
					// this must be a repeated group - track it.
					if (fetchRecursively) {
						fetchCsvRecursively(value,
								fetchBinaryData, fetchRecursively, null);
					}
					break;
				case UNKNOWN_URL:
					break;
				}
			}

			// OK. Reassemble the value row (since it may have binary
			// attachments that redefine the tableContents value)
			// and emit it to the file associated with the normalized key path.
			final String tableRow = generateCommaSeparatedRow(tableContents);

			ActiveFileInfo fi = openCsvFiles.get(normalizedSelfKeyPath);
			if (fi == null) {
				fi = new ActiveFileInfo();
				fi.file = new File(csvDataDir, fileNameFromKeyPath(normalizedSelfKeyPath));
				fi.writer = new OutputStreamWriter(new FileOutputStream(fi.file),UTF_8);
				final String tableColumns = generateCommaSeparatedRow(tableHeader);
				fi.writer.write(tableColumns);
				fi.writer.append('\n');
				fi.lastCursor = null; // will be populated below...
				fi.lastKey = null; // will be populated below...
				openCsvFiles.put(normalizedSelfKeyPath, fi);
			}

			fi.writer.write(tableRow);
			fi.writer.append('\n');
			
			// record the key we're at -- all data up to this point has been 
			// successfully retrieved.  There is a small timing window for the
			// flush of the csv writers (above and for nested groups).  But we
			// let that slide as it is unlikely to cause data loss unless the 
			// disk is full, in which case the writing of the manifest would 
			// fail, and we'd need to restart anyways.
			fi.lastKey = rawKEY;
			
			// update next cursor if it isn't null...
			if (nextCursor != null)
				fi.lastCursor = nextCursor;
			
		} catch (IllegalStateException e) {
			// Something failed.
			// Remove all the binary files we just fetched before we continue.
			// Since we are not writing this row to the file, these binary
			// files would be orphaned if we didn't do this.
			for (File f : binaryDataFiles) {
				f.delete();
			}
			throw e;
		} catch (IOException e) {
			// Something failed.
			// Remove all the binary files we just fetched before we continue.
			// Since we are not writing this row to the file, these binary
			// files would be orphaned if we didn't do this.
			for (File f : binaryDataFiles) {
				f.delete();
			}
			throw e;
		}
		return false;
	}

	/**
	 * TODO: implement doZipMe()
	 * Makes a zip file containing all the files in allFiles.
	 * This is currently not used, and is here primarily for 
	 * future reference.
	 * 
//	private void doZipMe() {
//		Calendar aCalendar = Calendar.getInstance(Locale.getDefault());
//		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
//		df.format(aCalendar);
//		String zipFileName = baseDir.getName()
//				+ Calendar.getInstance(Locale.getDefault()) + ".zip";
//
//		File zipFile = new File(baseDir.getParentFile(), zipFileName);
//
//		// TODO: allFiles is the recursive traversal of the directory
//		// structure...
//		List<File> allFiles = new ArrayList<File>();
//
//		// TODO: Ugh. This should be rewritten to not require everything to fit
//		// in memory.
//
//		try {
//			FileOutputStream fos = new FileOutputStream(zipFile);
//			ZipOutputStream zos = new ZipOutputStream(fos);
//
//			for (int i = 0; i < allFiles.size(); i++) {
//				File currFile = allFiles.get(i);
//				FileInputStream fis = new FileInputStream(currFile);
//
//				long fileLen = currFile.length();
//				if (fileLen > Integer.MAX_VALUE) {
//					// TODO: do some checks on whether there's too many bytes in
//					// this file
//					// since an array cannot be larger than Integer.Max_val
//					// and handle it. (though the chance of a file being > 4GB
//					// is small)
//				}
//				// read the file in to b
//				byte[] b = new byte[(int) fileLen];
//				fis.read(b);
//				fis.close();
//
//				// put the file in to the zip output stream
//				ZipEntry ze = new ZipEntry(currFile.getPath());
//				zos.putNextEntry(ze);
//				zos.write(b);
//			}
//			zos.close();
//		} catch (FileNotFoundException f) {
//			System.err.println("file not found exception thrown in zipFiles");
//			f.printStackTrace();
//		} catch (IOException ioe) {
//			System.err.println("ioe exception in zipFiles");
//			ioe.printStackTrace();
//		}
//	}
	 */
}
