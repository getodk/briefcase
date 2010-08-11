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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Test cases for the simpler bits of the CsvDownload class.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class CsvDownloadTest {

	static File scratchBase = null;

	private static boolean deleteDirectoryTree(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(File f : files) {
	         if(f.isDirectory()) {
	        	 deleteDirectoryTree(f);
	         }
	         else {
	           f.delete();
	         }
	      }
	      return( path.delete() );
	    }
	    return true;
	}

	@BeforeClass
	public static void Initialize() {
		assertNotNull( "TEMP enviornment variable not defined", System.getenv("TEMP"));
		File scratchTemp = new File(System.getenv("TEMP"));
		assertTrue("TEMP must be writable: " + scratchTemp.getPath(), scratchTemp.canWrite());
		scratchBase = new File(scratchTemp, "briefcase-unit-test");
		assertTrue("Unable to delete old scratch directory: " + scratchBase.getPath(),
					deleteDirectoryTree(scratchBase));
		assertTrue("Unable to create scratch directory: " + scratchBase.getPath(),scratchBase.mkdir());
	}
	
	@AfterClass
	public static void Cleanup() {
		// deleteDirectoryTree(scratchBase);
	}
	
	@Test
	public void testClassifyUrl() {
		assertEquals(CsvDownload.UrlType.UNKNOWN_URL,CsvDownload.classifyUrl("http://localhost:8888"));
		assertEquals(CsvDownload.UrlType.UNKNOWN_URL,CsvDownload.classifyUrl("htp:///-"));
		assertEquals(CsvDownload.UrlType.UNKNOWN_URL,CsvDownload.classifyUrl("mailto:msundt@cs.washington.edu"));
		assertEquals(CsvDownload.UrlType.UNKNOWN_URL,CsvDownload.classifyUrl("888"));
		assertEquals(CsvDownload.UrlType.UNKNOWN_URL,CsvDownload.classifyUrl("8.12"));
		assertEquals(CsvDownload.UrlType.BINARY_DATA_URL,CsvDownload.classifyUrl("http://localhost:8888/binaryData?blobKey=agtvcGVuZGF0YWtpdHIQCxIKYmxvYl9zdG9yZRgDDA"));
		assertEquals(CsvDownload.UrlType.BINARY_DATA_URL,CsvDownload.classifyUrl("http://aggregate.org/binaryData?blobKey=agtvcGVuZGF0YWtpdHIQCxIKYmxvYl9zdG9yZRgDDA"));
		assertEquals(CsvDownload.UrlType.BINARY_DATA_URL,CsvDownload.classifyUrl("http://aggregate.org/MyApp/binaryData?blobKey=agtvcGVuZGF0YWtpdHIQCxIKYmxvYl9zdG9yZRgDDA"));
		assertEquals(CsvDownload.UrlType.CSV_FRAGMENT_URL,CsvDownload.classifyUrl("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold"));
		assertEquals(CsvDownload.UrlType.CSV_FRAGMENT_URL,CsvDownload.classifyUrl("http://aggregate.org/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold"));
		assertEquals(CsvDownload.UrlType.CSV_FRAGMENT_URL,CsvDownload.classifyUrl("http://aggregate.org/MyApp/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold"));
	}
	
	@Test
	public void testExtractCursor() throws MalformedURLException, UnsupportedEncodingException {
		assertNull( CsvDownload.extractCursor("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold"));
		assertNull( CsvDownload.extractCursor("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000"));
		assertEquals("abcd", CsvDownload.extractCursor("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd"));
		assertEquals("abcd", CsvDownload.extractCursor("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&cursor=abcd&numEntries=1000"));
		assertEquals("abcd", CsvDownload.extractCursor("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testExtractCursor_exception1() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractCursor("http://localhost:8888/csvFragment?cursor=abcd&cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExtractCursor_exception2() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractCursor("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&cursor=abcd&numEntries=1000");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testExtractCursor_exception3() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractCursor("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd");
	}
	
	@Test(expected=MalformedURLException.class)
	public void testExtractCursor_exception4() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractCursor("httpss://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd");
	}

	@Test
	public void testExtractKeyReference() throws MalformedURLException, UnsupportedEncodingException {
		String cleanedUpKeyPath0 = "HouseholdSurvey1/HouseholdSurvey";
		String cleanedUpKeyPath1 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold";
		String cleanedUpKeyPath3 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold/ToysOfChildren";

		assertEquals(cleanedUpKeyPath0, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey&numEntries=1000&cursor=abcd"));
		assertEquals(cleanedUpKeyPath1, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold&cursor=abcd&numEntries=1000"));
		assertEquals(cleanedUpKeyPath3, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%2FToysOfChildren&cursor=abcd&numEntries=1000"));

		String cleanedUpKey0 = "HouseholdSurvey1/HouseholdSurvey[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]";
		String cleanedUpKey1 = "HouseholdSurvey1/HouseholdSurvey[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]/ChildrenOfHousehold";
		String cleanedUpKey2 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]";
		String cleanedUpKey3 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold/ToysOfChildren[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]";
		String cleanedUpKey4 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]/ToysOfChildren";

		assertEquals(cleanedUpKey0, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&numEntries=1000&cursor=abcd"));
		assertEquals(cleanedUpKey0, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&cursor=abcd&numEntries=1000"));
		assertEquals(cleanedUpKey0, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&numEntries=1000"));

		assertEquals(cleanedUpKey1, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd"));
		assertEquals(cleanedUpKey1, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&cursor=abcd&numEntries=1000"));
		assertEquals(cleanedUpKey1, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000"));

		assertEquals(cleanedUpKey2, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&numEntries=1000&cursor=abcd"));
		assertEquals(cleanedUpKey2, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&cursor=abcd&numEntries=1000"));
		assertEquals(cleanedUpKey2, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&numEntries=1000"));

		assertEquals(cleanedUpKey3, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%2FToysOfChildren%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&numEntries=1000&cursor=abcd"));
		assertEquals(cleanedUpKey3, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%2FToysOfChildren%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&cursor=abcd&numEntries=1000"));
		assertEquals(cleanedUpKey3, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%2FToysOfChildren%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D&numEntries=1000"));

		assertEquals(cleanedUpKey4, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FToysOfChildren&numEntries=1000&cursor=abcd"));
		assertEquals(cleanedUpKey4, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FToysOfChildren&cursor=abcd&numEntries=1000"));
		assertEquals(cleanedUpKey4, CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%2FChildrenOfHousehold%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FToysOfChildren&numEntries=1000"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testExtractKeyReference_exception1() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&numEntries=1000&cursor=abcd");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExtractKeyReference_exception2() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?odkId=foo&cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExtractKeyReference_exception3() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractKeyReference("http://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd&odkId=fooo");
	}

	@Test(expected=MalformedURLException.class)
	public void testExtractKeyReference_exception4() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractKeyReference("httpss://localhost:8888/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd");
	}

	@Test(expected=MalformedURLException.class)
	public void testExtractKeyReference_exception5() throws MalformedURLException, UnsupportedEncodingException {
		CsvDownload.extractKeyReference("http://localhost:aaa/csvFragment?cursor=abcd&odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold&numEntries=1000&cursor=abcd");
	}
	
	@Test
	public void testFlattenKeyReferenceToKeyPath() {
		String cleanedUpKey0 = "HouseholdSurvey1/HouseholdSurvey[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]";
		String cleanedUpPath0 = "HouseholdSurvey1/HouseholdSurvey";
		String cleanedUpKey1 = "HouseholdSurvey1/HouseholdSurvey[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]/ChildrenOfHousehold";
		String cleanedUpPath1 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold";
		String cleanedUpKey2 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]";
		String cleanedUpPath2 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold";
		String cleanedUpKey3 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold/ToysOfChildren[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]";
		String cleanedUpPath3 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold/ToysOfChildren";
		String cleanedUpKey4 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold[@key=\"agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA\"]/ToysOfChildren";
		String cleanedUpPath4 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold/ToysOfChildren";
		
		assertEquals(cleanedUpPath0,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpPath0));
		assertEquals(cleanedUpPath1,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpPath1));
		assertEquals(cleanedUpPath2,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpPath2));
		assertEquals(cleanedUpPath3,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpPath3));
		assertEquals(cleanedUpPath4,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpPath4));

		assertEquals(cleanedUpPath0,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpKey0));
		assertEquals(cleanedUpPath1,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpKey1));
		assertEquals(cleanedUpPath2,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpKey2));
		assertEquals(cleanedUpPath3,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpKey3));
		assertEquals(cleanedUpPath4,CsvDownload.flattenKeyReferenceToKeyPath(cleanedUpKey4));
	}
	
	@Test
	public void testCsvBlend() {
		Integer [] columns = {1,3,3,3,3,2,4,3,4};
		String [] csvLines = {
			"", // 1
			",,", // 3
			",,\"a\"", //3
			"\"a\",,", //3
			",\"a\",", //3
			"\"\",\"\"",//2
			"\"a\",\"a\"\"\",\"\"\"b\",\"aaaa\"\"bbbb\"\"ccc\"", //4
			"\",,,,\",,", //3
			",\",,,,\",\",\",", //4
			"\"SubmissionDate\",\"StartTime\",\"EndTime\",\"DeviceID\","+
			"\"SubscriberID\",\"SurveyorName\",\"SurveyorID\",\"SurveyorCode\","+
			"\"HouseholdLocation-Latitude\",\"HouseholdLocation-Longitude\","+
			"\"HouseholdLocation-Altitude\",\"HouseholdLocation-Accuracy\","+
			"\"HouseholdImage\",\"HouseholdAudio\",\"HouseholdVideo\","+
			"\"HeadOfHouseholdName\",\"HeadOfHouseholdAge\",\"HeadOfHouseholdGender\"," +
			"\"HeadOfHouseholdConfirmation\",\"ChildrenOfHousehold\",\"SurveyorNotes\",\"KEY\"",
			"\"Wednesday, July 28, 2010 10:45:55 PM UTC\",\"Wed Jul 28 22:42:12 UTC 2010\"," +
			"\"Wed Jul 28 22:45:16 UTC 2010\",\"000000000000000\",\"310260000000000\"," +
			"\"&#32780;&#25105;&#29609;&#20799;&#20320;\",,\"ad22\",,,,,\"http://localhost:8888/" +
			"binaryData?blobKey=agtvcGVuZGF0YWtpdHIQCxIKYmxvYl9zdG9yZRgDDA\",,,\"rrry\",\"56\"," +
			"\"m\",\"Yes\",\"http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey" +
			"%5B%40key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D%2FChildrenOfHousehold\"" +
			",\"Funkyd\",\"http://localhost:8888/csvFragment?odkId=HouseholdSurvey1%2FHouseholdSurvey%5B%40" +
			"key%3D%22agtvcGVuZGF0YWtpdHIWCxIQSG91c2Vob2xkU3VydmV5MRgCDA%22%5D\""
		};
		for ( String csvLine : csvLines ) {
			assertEquals(csvLine, CsvDownload.generateCommaSeparatedRow(CsvDownload.splitCsvRow(csvLine)));
		}
		for ( int i = 0 ; i < columns.length ; ++i ) {
			assertTrue( columns[i] == CsvDownload.splitCsvRow(csvLines[i]).size());
		}
	}
	
	@Test(expected=IllegalStateException.class)
	public void testSplitCvsRow_exception1() {
		CsvDownload.splitCsvRow("a");
	}

	@Test(expected=IllegalStateException.class)
	public void testSplitCvsRow_exception2() {
		CsvDownload.splitCsvRow(",a");
	}

	@Test(expected=IllegalStateException.class)
	public void testSplitCvsRow_exception3() {
		CsvDownload.splitCsvRow(",\"a");
	}

	@Test(expected=IllegalStateException.class)
	public void testSplitCvsRow_exception4() {
		CsvDownload.splitCsvRow("\",a");
	}

	@Test
	public void testFileNameFromKeyPath() {
		String cleanedUpPath0 = "HouseholdSurvey1/HouseholdSurvey";
		String csvFilename0 = "HouseholdSurvey.HouseholdSurvey1.csv";
		String cleanedUpPath1 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold";
		String csvFilename1 = "ChildrenOfHousehold.HouseholdSurvey.HouseholdSurvey1.csv";
		String cleanedUpPath3 = "HouseholdSurvey1/HouseholdSurvey/ChildrenOfHousehold/ToysOfChildren";
		String csvFilename3 = "ToysOfChildren.ChildrenOfHousehold.HouseholdSurvey.HouseholdSurvey1.csv";
		
		assertEquals(csvFilename0,CsvDownload.fileNameFromKeyPath(cleanedUpPath0));
		assertEquals(csvFilename1,CsvDownload.fileNameFromKeyPath(cleanedUpPath1));
		assertEquals(csvFilename3,CsvDownload.fileNameFromKeyPath(cleanedUpPath3));
	}
	
	@Test
	public void testDofExceptionString() {
		String format = "This file: {0} is messed up!";
		File f = new File("//foof//f/ff/f");
		String unixFormat = "This file: //foof/f/ff/f is messed up!";
		String windowsFormat = unixFormat.replace("/", "\\");
		String result = CsvDownload.dofExceptionString(format, f);
		assertTrue("result: "+result+" does not equal " + windowsFormat + " or " + unixFormat, (result.equals(unixFormat) || result.equals(windowsFormat)) );

		f = new File("/");
		unixFormat = "This file: / is messed up!";
		windowsFormat = "This file: C:\\ is messed up!";
		result = CsvDownload.dofExceptionString(format, f);
		assertTrue("result: "+result+" does not equal " + windowsFormat + " or " + unixFormat, (result.equals(unixFormat) || result.equals(windowsFormat)) );
	}
	
	@Test
	public void testStartUp1() {
		File testDir = new File(scratchBase, "testStartUp1");
		CsvDownload app = new CsvDownload("http://localhost:8888", testDir.getAbsolutePath(),
				new CsvDownload.ActionListener() {
					@Override
					public void beforeFetchUrl(String url, int tries, int count) {
						// do nothing
					}
				});
		
		app.closeAllFilesAndManifest(true);
		
		File binaryDir = new File(testDir, CsvDownload.BINARY_DATA_DIRECTORY);
		assertTrue( binaryDir.exists());
		assertTrue( binaryDir.isDirectory());

		File csvDir = new File(testDir, CsvDownload.CSV_DATA_DIRECTORY);
		assertTrue( csvDir.exists());
		assertTrue( csvDir.isDirectory());
		
		File manifest = new File(testDir, CsvDownload.MANIFEST_FILE_NAME);
		assertTrue( manifest.exists());
		assertTrue( manifest.isFile());
	}	
}

