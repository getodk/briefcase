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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bushe.swing.event.EventBus;
import org.javarosa.core.model.instance.TreeElement;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransformProgressEvent;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TransformToCsv implements ITransformFormAction {

   private static final String MEDIA_DIR = "media";

static final Logger log = Logger.getLogger(TransformToCsv.class.getName());

	File outputDir;
	File outputMediaDir;
	LocalFormDefinition lfd;
	TerminationFuture terminationFuture;
	Map<TreeElement, OutputStreamWriter > fileMap = new HashMap<TreeElement, OutputStreamWriter >();
	

	TransformToCsv(File outputDir, LocalFormDefinition lfd,
			TerminationFuture terminationFuture) {
		this.outputDir = outputDir;
		this.outputMediaDir = new File(outputDir, MEDIA_DIR);
		this.lfd = lfd;
		this.terminationFuture = terminationFuture;
	}

	@Override
	public boolean doAction() {
		boolean allSuccessful = true;
		File formsFolder = 
				FileSystemUtils.getFormsFolder(new File(BriefcasePreferences.getBriefcaseDirectoryProperty()));
		
		File instancesDir;
		try {
			instancesDir = FileSystemUtils.getFormInstancesDirectory(formsFolder, lfd.getFormName());
		} catch (FileSystemException e) {
			// emit status change...
	        EventBus.publish(new TransformProgressEvent("Unable to access instances directory of form"));
	        e.printStackTrace();
			return false;
		}

		if ( !outputDir.exists() ) {
			if ( !outputDir.mkdir() ) {
		        EventBus.publish(new TransformProgressEvent("Unable to create destination directory"));
				return false;
			}
		}
		
		if ( !outputMediaDir.exists() ) {
			if ( !outputMediaDir.mkdir() ) {
		        EventBus.publish(new TransformProgressEvent("Unable to create destination media directory"));
				return false;
			}
		}
		
		if ( !processFormDefinition() ) {
			// weren't able to initialize the csv file...
			return false;
		}
		
		File[] instances = instancesDir.listFiles();
		
		for ( File instanceDir : instances ) {
			if ( instanceDir.getName().startsWith(".") ) continue; // Mac OSX
			allSuccessful = allSuccessful && processInstance(instanceDir);
		}
		
		for ( OutputStreamWriter w : fileMap.values() ) {
			try {
				w.flush();
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
		        EventBus.publish(new TransformProgressEvent("Error flushing csv file"));
				allSuccessful = false;
			}
		}
		
		return allSuccessful;
	}

	private void emitString( OutputStreamWriter osw, boolean first, String string) throws IOException {
		osw.append(first ? "" : ",");
		if ( string == null ) return;
		if ( string.length() == 0 || string.contains("\n") || string.contains("\"") || string.contains(",") ) {
			string = string.replace("\"", "\"\"");
			string = "\"" + string + "\"";
		}
		osw.append(string);
	}
	
	private String getFullName(TreeElement e ) {
		List<String> names = new ArrayList<String>();
		while ( e != null && e != lfd.getSubmissionElement() ) {
			names.add(e.getName());
			e = e.getParent();
		}
		StringBuilder b = new StringBuilder();
		Collections.reverse(names);
		boolean first = true;
		for ( String s : names ) {
			if ( !first ) {
				b.append("/");
			}
			first = false;
			b.append(s);
		}
		
		return b.toString();
	}
	
	private Element findElement(Element submissionElement, String name) {
		int maxChildren = submissionElement.getChildCount();
		for ( int i = 0 ; i < maxChildren ; i++ ) {
			if ( submissionElement.getType(i) == Node.ELEMENT ) {
				Element e = submissionElement.getElement(i);
				if ( name.equals(e.getName()) ) {
					return e;
				}
			}
		}
		return null;
	}
	
	private String getSubmissionValue(Element element) {
		// could not find element, return null
		if (element == null) {
			return null;
		}

		StringBuilder b = new StringBuilder();
		
		int maxChildren = element.getChildCount();
		for ( int i = 0 ; i < maxChildren ; i++) {
			if ( element.getType(i) == Node.TEXT ) {
				b.append(element.getText(i));
			}
		}
		return b.toString();
	}

	private boolean emitSubmissionCsv( OutputStreamWriter osw, Element submissionElement, TreeElement treeElement, boolean first, String uniquePath, File instanceDir ) throws IOException {
	      // OK -- group with at least one element -- assume no value...
	      // TreeElement list has the begin and end tags for the nested groups.
	      // Swallow the end tag by looking to see if the prior and current
	      // field names are the same.
	      TreeElement prior = null;
	      int trueOrdinal = 1;
	      for (int i = 0; i < treeElement.getNumChildren(); ++i) {
	    	  TreeElement current = (TreeElement) treeElement.getChildAt(i);
	    	  // TODO: make this pay attention to namespace of the tag...
	    	  if ( (prior != null) && 
	    		   (prior.getName().equals(current.getName())) ) {
	    		  // it is the end-group tag... seems to happen with two adjacent repeat groups
	    		  log.info("repeating tag at " + i + " skipping " + current.getName());
	    		  prior = current;
	    	  } else {
			      Element ec = findElement(submissionElement, current.getName());
	    		  switch ( current.dataType ) {
	    		    case org.javarosa.core.model.Constants.DATATYPE_TEXT:/**
	    		         * Text question type.
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_INTEGER:/**
	    		         * Numeric question
	    		         * type. These are numbers without decimal points
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_DECIMAL:/**
	    		         * Decimal question
	    		         * type. These are numbers with decimals
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_DATE:/**
	    		         * Date question type.
	    		         * This has only date component without time.
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_TIME:/**
	    		         * Time question type.
	    		         * This has only time element without date
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_DATE_TIME:/**
	    		         * Date and Time
	    		         * question type. This has both the date and time components
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_CHOICE:/**
	    		         * This is a question
	    		         * with alist of options where not more than one option can be selected at
	    		         * a time.
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_CHOICE_LIST:/**
	    		         * This is a
	    		         * question with alist of options where more than one option can be
	    		         * selected at a time.
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_BOOLEAN:/**
	    		         * Question with
	    		         * true and false answers.
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_GEOPOINT:/**
	    		         * Question with
	    		         * location answer.
	    		         */
	    		      case org.javarosa.core.model.Constants.DATATYPE_BARCODE:/**
	    		         * Question with
	    		         * barcode string answer.
	    		         */
	    		      default:
	    		      case org.javarosa.core.model.Constants.DATATYPE_UNSUPPORTED:
	    		    	if ( ec == null ) {
	    		    		emitString( osw, first, null);
	    		    	} else {
	    		    		emitString( osw, first, getSubmissionValue(ec));
	    		    	}
	    		    	first = false;
	    		    	break;
	    		      case org.javarosa.core.model.Constants.DATATYPE_BINARY:/**
		    		     * Question with
		    		     * external binary answer.
		    		     */
	    		    	String binaryFilename = getSubmissionValue(ec);
	    		    	if ( binaryFilename == null || binaryFilename.length() == 0 ) {
	    		    		emitString( osw, first, null);
			    		    first = false;
	    		    	} else {
	    		    		int dotIndex = binaryFilename.lastIndexOf(".");
	    		    		String namePart = (dotIndex == -1) ? binaryFilename : binaryFilename.substring(0,dotIndex);
	    		    		String extPart = (dotIndex == -1) ? "" : binaryFilename.substring(dotIndex);
	    		    		
		    		    	File binaryFile = new File(instanceDir, binaryFilename);
		    		    	String destBinaryFilename = binaryFilename;
		    		    	int version = 1;
		    		    	File destFile = new File(outputMediaDir, destBinaryFilename);
		    		    	while ( destFile.exists() ) {
		    		    		destBinaryFilename = namePart + "-" + (++version) + extPart; 
		    		    		destFile = new File(outputMediaDir, destBinaryFilename);
			    		    }
		    		    	FileUtils.copyFile(binaryFile, destFile);
	    		    		emitString( osw, first, MEDIA_DIR + File.separator + destFile.getName());
			    		    first = false;
	    		    	}
		    		    break;
	    		      case org.javarosa.core.model.Constants.DATATYPE_NULL: /*
	    		                                                             * for nodes that have
	    		                                                             * no data, or data
	    		                                                             * type otherwise
	    		                                                             * unknown
	    		                                                             */
	    		        if (current.repeatable) {
	    		      	    // repeatable group...
	    		        	// TODO: do the right thing here...
	        		    	emitString(osw, first, uniquePath + "/" + getFullName(current));
		    		    	first = false;
		    		    	if ( prior != null && current.getName().equals(prior.getName()) ) {
		    		    		// we are repeating this group...
		    		    		++trueOrdinal;
		    		    	} else {
		    		    		// we are starting a new group...
		    		    		trueOrdinal = 1;
		    		    	}
		    		    	emitRepeatingGroupCsv(ec, current, uniquePath + "/" + getFullName(current), uniquePath + "/" + trueOrdinal, instanceDir);
	    		        } else if (current.getNumChildren() == 0 && current != lfd.getSubmissionElement()) {
	    		          // assume fields that don't have children are string fields.
	    		        	emitString(osw, first, getSubmissionValue(ec));
	        		    	first = false;
	    		        } else {
	    		        	/* one or more children -- this is a non-repeating group */
	    		        	first = emitSubmissionCsv(osw, ec, current, first, uniquePath, instanceDir);
	    		        }
	    		        break;
	    		  }
	    		  prior = current;
	    	  }
	      }
	      return first;
	}

	private void emitRepeatingGroupCsv(Element groupElement, TreeElement group, String uniqueGroupPath, String uniquePath, File instanceDir) throws IOException {
		OutputStreamWriter osw = fileMap.get(group);
    	emitString(osw, true, uniqueGroupPath);
    	emitString(osw, false, uniquePath);
		emitSubmissionCsv( osw, groupElement, group, false, uniquePath, instanceDir);
		osw.append("\n");
	}
	
	private boolean emitCsvHeaders(OutputStreamWriter osw, TreeElement treeElement, boolean first) throws IOException {
      // OK -- group with at least one element -- assume no value...
      // TreeElement list has the begin and end tags for the nested groups.
      // Swallow the end tag by looking to see if the prior and current
      // field names are the same.
      TreeElement prior = null;
      int trueOrdinal = 0;
      for (int i = 0; i < treeElement.getNumChildren(); ++i) {
    	  TreeElement current = (TreeElement) treeElement.getChildAt(i);
    	  // TODO: make this pay attention to namespace of the tag...
    	  if ( (prior != null) && 
    		   (prior.getName().equals(current.getName())) ) {
    		  // it is the end-group tag... seems to happen with two adjacent repeat groups
    		  log.info("repeating tag at " + i + " skipping " + current.getName());
    		  prior = current;
    	  } else {
    		  switch ( current.dataType ) {
    		    case org.javarosa.core.model.Constants.DATATYPE_TEXT:/**
    		         * Text question type.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_INTEGER:/**
    		         * Numeric question
    		         * type. These are numbers without decimal points
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_DECIMAL:/**
    		         * Decimal question
    		         * type. These are numbers with decimals
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_DATE:/**
    		         * Date question type.
    		         * This has only date component without time.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_TIME:/**
    		         * Time question type.
    		         * This has only time element without date
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_DATE_TIME:/**
    		         * Date and Time
    		         * question type. This has both the date and time components
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_CHOICE:/**
    		         * This is a question
    		         * with alist of options where not more than one option can be selected at
    		         * a time.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_CHOICE_LIST:/**
    		         * This is a
    		         * question with alist of options where more than one option can be
    		         * selected at a time.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_BOOLEAN:/**
    		         * Question with
    		         * true and false answers.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_GEOPOINT:/**
    		         * Question with
    		         * location answer.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_BARCODE:/**
    		         * Question with
    		         * barcode string answer.
    		         */
    		      case org.javarosa.core.model.Constants.DATATYPE_BINARY:/**
    		         * Question with
    		         * external binary answer.
    		         */
    		      default:
    		      case org.javarosa.core.model.Constants.DATATYPE_UNSUPPORTED:
    		    	emitString(osw, first, getFullName(current));
    		    	first = false;
    		    	break;
    		      case org.javarosa.core.model.Constants.DATATYPE_NULL: /*
    		                                                             * for nodes that have
    		                                                             * no data, or data
    		                                                             * type otherwise
    		                                                             * unknown
    		                                                             */
    		        if (current.repeatable) {
    		      	// repeatable group...
        		    	emitString(osw, first, getFullName(current));
	    		    	first = false;
	    		    	processRepeatingGroupDefinition(current);
    		        } else if (current.getNumChildren() == 0 && current != lfd.getSubmissionElement()) {
    		          // assume fields that don't have children are string fields.
        		    	emitString(osw, first, getFullName(current));
        		    	first = false;
    		        } else {
    		        	/* one or more children -- this is a non-repeating group */
    		        	first = emitCsvHeaders(osw, current, first);
    		        }
    		        break;
    		  }
    		  prior = current;
    	  }
      }
      return first;
	}
	

	private void processRepeatingGroupDefinition(TreeElement group) throws IOException {
		String formName = lfd.getFormName() + "-" + getFullName(group);
		File topLevelCsv = new File( outputDir, formName + ".csv");
		FileOutputStream os = new FileOutputStream(topLevelCsv);
		OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
		fileMap.put(group, osw);
		emitString(osw, true, group.getName() + "-set");
		emitString(osw, false, "PrimaryKey");
		emitCsvHeaders( osw, group, false);
		osw.append("\n");
	}
	
	private boolean processFormDefinition() {
		
		TreeElement submission = lfd.getSubmissionElement();
		
		String formName = lfd.getFormName();
		File topLevelCsv = new File( outputDir, formName + ".csv");
		FileOutputStream os;
		try {
			os = new FileOutputStream(topLevelCsv);
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
			fileMap.put(submission, osw);
			emitString(osw, true, "PrimaryKey");
			emitCsvHeaders( osw, submission, false);
			osw.append("\n");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
	        EventBus.publish(new TransformProgressEvent("Unable to create csv file: " + topLevelCsv.getPath()));
	        for ( OutputStreamWriter w : fileMap.values()) {
	        	try {
					w.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	        }
	        fileMap.clear();
			return false;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
	        EventBus.publish(new TransformProgressEvent("Unable to create csv file: " + topLevelCsv.getPath()));
	        for ( OutputStreamWriter w : fileMap.values()) {
	        	try {
					w.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	        }
	        fileMap.clear();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
	        EventBus.publish(new TransformProgressEvent("Unable to create csv file: " + topLevelCsv.getPath()));
	        for ( OutputStreamWriter w : fileMap.values()) {
	        	try {
					w.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	        }
	        fileMap.clear();
			return false;
		}
		return true;
	}
	
	private boolean processInstance(File instanceDir) {
		File submission = new File(instanceDir, "submission.xml");
		if ( !submission.exists() || !submission.isFile()) {
	        EventBus.publish(new TransformProgressEvent("Submission not found for instance directory: " + instanceDir.getPath()));
	        return false;
		}
        EventBus.publish(new TransformProgressEvent("Processing instance: " + instanceDir.getName()));

        // parse the xml document...
        Document doc = null;
        try {
          InputStream is = null;
          InputStreamReader isr = null;
          try {
            is = new FileInputStream(submission);
            isr = new InputStreamReader(is, "UTF-8");
            doc = new Document();
            KXmlParser parser = new KXmlParser();
            parser.setInput(isr);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            doc.parse(parser);
            isr.close();
          } finally {
            if (isr != null) {
              try {
                isr.close();
              } catch (Exception e) {
                // no-op
              }
            }
            if (is != null) {
              try {
                is.close();
              } catch (Exception e) {
                // no-op
              }
            }
          }
        } catch (XmlPullParserException e) {
			e.printStackTrace();
	        EventBus.publish(new TransformProgressEvent("Failed during parsing of submission Xml: " + e.getMessage()));
	        return false;
        } catch ( IOException e ) {
        	e.printStackTrace();
	        EventBus.publish(new TransformProgressEvent("Failed while reading submission xml: " + e.getMessage()));
	        return false;
        }

        if ( lfd.isEncryptedForm() ) {
        	// TODO: parse this Xml and reconstruct the images, etc.
        	
        	// create a temporary instanceDir and decrypt the files to that location...
        }
        
        try {
            // and now, we have the Xml Document and the Form definition, emit the csv...
        	OutputStreamWriter osw = fileMap.get(lfd.getSubmissionElement());
        	Element rootElement = doc.getRootElement();
        	String instanceId = rootElement.getAttributeValue(null,"instanceID");
        	if ( instanceId == null || instanceId.length() == 0 ) {
        		instanceId = "" + WebUtils.iso8601Date(new Date(submission.lastModified())) + "-" + Long.toString(submission.length());
        	}
        	emitString( osw, true, instanceId );
        	emitSubmissionCsv( osw, doc.getRootElement(), lfd.getSubmissionElement(), false, instanceId, instanceDir);
			osw.append("\n");
        	return true;
       	
        } catch (IOException e) {
			e.printStackTrace();
	        EventBus.publish(new TransformProgressEvent("Failed writing csv: " + e.getMessage()));
	        return false;
		} finally {
        	if ( lfd.isEncryptedForm() ) {
        		// destroy the temp directory and its contents...
        	}
        }
	}
	
	@Override
	public LocalFormDefinition getFormDefinition() {
		return lfd;
	}
}
