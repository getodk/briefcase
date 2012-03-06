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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
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
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.XmlManipulationUtils.FormInstanceMetadata;

public class ExportToCsv implements ITransformFormAction {

  private static final String MEDIA_DIR = "media";

  static final Logger log = Logger.getLogger(ExportToCsv.class.getName());

  File outputDir;
  File outputMediaDir;
  LocalFormDefinition lfd;
  TerminationFuture terminationFuture;
  Map<TreeElement, OutputStreamWriter> fileMap = new HashMap<TreeElement, OutputStreamWriter>();

  ExportToCsv(File outputDir, LocalFormDefinition lfd, TerminationFuture terminationFuture) {
    this.outputDir = outputDir;
    this.outputMediaDir = new File(outputDir, MEDIA_DIR);
    this.lfd = lfd;
    this.terminationFuture = terminationFuture;
  }

  @Override
  public boolean doAction() {
    boolean allSuccessful = true;
    File instancesDir;
    try {
      instancesDir = FileSystemUtils.getFormInstancesDirectory(lfd.getFormName());
    } catch (FileSystemException e) {
      // emit status change...
      EventBus.publish(new ExportProgressEvent("Unable to access instances directory of form"));
      e.printStackTrace();
      return false;
    }

    if (!outputDir.exists()) {
      if (!outputDir.mkdir()) {
        EventBus.publish(new ExportProgressEvent("Unable to create destination directory"));
        return false;
      }
    }

    if (!outputMediaDir.exists()) {
      if (!outputMediaDir.mkdir()) {
        EventBus
            .publish(new ExportProgressEvent("Unable to create destination media directory"));
        return false;
      }
    }

    if (!processFormDefinition()) {
      // weren't able to initialize the csv file...
      return false;
    }

    File[] instances = instancesDir.listFiles();

    for (File instanceDir : instances) {
      if ( terminationFuture.isCancelled() ) {
        EventBus.publish(new ExportProgressEvent("ABORTED"));
        allSuccessful = false;
        break;
      }
      if (instanceDir.getName().startsWith("."))
        continue; // Mac OSX
      allSuccessful = allSuccessful && processInstance(instanceDir);
    }

    for (OutputStreamWriter w : fileMap.values()) {
      try {
        w.flush();
        w.close();
      } catch (IOException e) {
        e.printStackTrace();
        EventBus.publish(new ExportProgressEvent("Error flushing csv file"));
        allSuccessful = false;
      }
    }

    return allSuccessful;
  }

  private void emitString(OutputStreamWriter osw, boolean first, String string) throws IOException {
    osw.append(first ? "" : ",");
    if (string == null)
      return;
    if (string.length() == 0 || string.contains("\n") || string.contains("\"")
        || string.contains(",")) {
      string = string.replace("\"", "\"\"");
      string = "\"" + string + "\"";
    }
    osw.append(string);
  }

  private String getFullName(TreeElement e, TreeElement group) {
    List<String> names = new ArrayList<String>();
    while (e != null && e != group) {
      names.add(e.getName());
      e = e.getParent();
    }
    StringBuilder b = new StringBuilder();
    Collections.reverse(names);
    boolean first = true;
    for (String s : names) {
      if (!first) {
        b.append("-");
      }
      first = false;
      b.append(s);
    }

    return b.toString();
  }

  private Element findElement(Element submissionElement, String name) {
    if ( submissionElement == null ) {
      return null;
    }
    int maxChildren = submissionElement.getChildCount();
    for (int i = 0; i < maxChildren; i++) {
      if (submissionElement.getType(i) == Node.ELEMENT) {
        Element e = submissionElement.getElement(i);
        if (name.equals(e.getName())) {
          return e;
        }
      }
    }
    return null;
  }

  private List<Element> findElementList(Element submissionElement, String name) {
    List<Element> ecl = new ArrayList<Element>();
    int maxChildren = submissionElement.getChildCount();
    for (int i = 0; i < maxChildren; i++) {
      if (submissionElement.getType(i) == Node.ELEMENT) {
        Element e = submissionElement.getElement(i);
        if (name.equals(e.getName())) {
          ecl.add(e);
        }
      }
    }
    return ecl;
  }

  private String getSubmissionValue(Element element) {
    // could not find element, return null
    if (element == null) {
      return null;
    }

    StringBuilder b = new StringBuilder();

    int maxChildren = element.getChildCount();
    for (int i = 0; i < maxChildren; i++) {
      if (element.getType(i) == Node.TEXT) {
        b.append(element.getText(i));
      }
    }
    return b.toString();
  }

  private boolean emitSubmissionCsv(OutputStreamWriter osw, Element submissionElement,
      TreeElement primarySet, TreeElement treeElement, boolean first, String uniquePath,
      File instanceDir) throws IOException {
    // OK -- group with at least one element -- assume no value...
    // TreeElement list has the begin and end tags for the nested groups.
    // Swallow the end tag by looking to see if the prior and current
    // field names are the same.
    TreeElement prior = null;
    for (int i = 0; i < treeElement.getNumChildren(); ++i) {
      TreeElement current = (TreeElement) treeElement.getChildAt(i);
      log.fine(" element name: " + current.getName());
      if ((prior != null) && (prior.getName().equals(current.getName()))) {
        // it is the end-group tag... seems to happen with two adjacent repeat
        // groups
        log.info("repeating tag at " + i + " skipping " + current.getName());
        prior = current;
      } else {
        Element ec = findElement(submissionElement, current.getName());
        switch (current.dataType) {
        case org.javarosa.core.model.Constants.DATATYPE_TEXT:/**
           * Text question
           * type.
           */
        case org.javarosa.core.model.Constants.DATATYPE_INTEGER:/**
           * Numeric
           * question type. These are numbers without decimal points
           */
        case org.javarosa.core.model.Constants.DATATYPE_DECIMAL:/**
           * Decimal
           * question type. These are numbers with decimals
           */
        case org.javarosa.core.model.Constants.DATATYPE_CHOICE:/**
           * This is a
           * question with alist of options where not more than one option can
           * be selected at a time.
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
        case org.javarosa.core.model.Constants.DATATYPE_BARCODE:/**
           * Question with
           * barcode string answer.
           */
        default:
        case org.javarosa.core.model.Constants.DATATYPE_UNSUPPORTED:
          if (ec == null) {
            emitString(osw, first, null);
          } else {
            emitString(osw, first, getSubmissionValue(ec));
          }
          first = false;
          break;
        case org.javarosa.core.model.Constants.DATATYPE_DATE:
          /**
           * Date question type. This has only date component without time.
           */
          if (ec == null) {
            emitString(osw, first, null);
          } else {
            String value = getSubmissionValue(ec);
            if (value == null || value.length() == 0) {
              emitString(osw, first, null);
            } else {
              Date date = WebUtils.parseDate(value);
              DateFormat formatter = DateFormat.getDateInstance();
              emitString(osw, first, formatter.format(date));
            }
          }
          first = false;
          break;
        case org.javarosa.core.model.Constants.DATATYPE_TIME:
          /**
           * Time question type. This has only time element without date
           */
          if (ec == null) {
            emitString(osw, first, null);
          } else {
            String value = getSubmissionValue(ec);
            if (value == null || value.length() == 0) {
              emitString(osw, first, null);
            } else {
              Date date = WebUtils.parseDate(value);
              DateFormat formatter = DateFormat.getTimeInstance();
              emitString(osw, first, formatter.format(date));
            }
          }
          first = false;
          break;
        case org.javarosa.core.model.Constants.DATATYPE_DATE_TIME:
          /**
           * Date and Time question type. This has both the date and time
           * components
           */
          if (ec == null) {
            emitString(osw, first, null);
          } else {
            String value = getSubmissionValue(ec);
            if (value == null || value.length() == 0) {
              emitString(osw, first, null);
            } else {
              Date date = WebUtils.parseDate(value);
              DateFormat formatter = DateFormat.getDateTimeInstance();
              emitString(osw, first, formatter.format(date));
            }
          }
          first = false;
          break;
        case org.javarosa.core.model.Constants.DATATYPE_GEOPOINT:
          /**
           * Question with location answer.
           */
          String compositeValue = (ec == null) ? null : getSubmissionValue(ec);
          compositeValue = (compositeValue == null) ? null : compositeValue.trim();

          // emit separate lat, long, alt, acc columns...
          if (compositeValue == null || compositeValue.length() == 0) {
            for (int count = 0; count < 4; ++count) {
              emitString(osw, first, null);
              first = false;
            }
          } else {
            String[] values = compositeValue.split(" ");
            for (String value : values) {
              emitString(osw, first, value);
              first = false;
            }
            for (int count = values.length; count < 4; ++count) {
              emitString(osw, first, null);
              first = false;
            }
          }
          break;
        case org.javarosa.core.model.Constants.DATATYPE_BINARY:
          /**
           * Question with external binary answer.
           */
          String binaryFilename = getSubmissionValue(ec);
          if (binaryFilename == null || binaryFilename.length() == 0) {
            emitString(osw, first, null);
            first = false;
          } else {
            int dotIndex = binaryFilename.lastIndexOf(".");
            String namePart = (dotIndex == -1) ? binaryFilename : binaryFilename.substring(0,
                dotIndex);
            String extPart = (dotIndex == -1) ? "" : binaryFilename.substring(dotIndex);

            File binaryFile = new File(instanceDir, binaryFilename);
            String destBinaryFilename = binaryFilename;
            int version = 1;
            File destFile = new File(outputMediaDir, destBinaryFilename);
            while (destFile.exists()) {
              destBinaryFilename = namePart + "-" + (++version) + extPart;
              destFile = new File(outputMediaDir, destBinaryFilename);
            }
            FileUtils.copyFile(binaryFile, destFile);
            emitString(osw, first, MEDIA_DIR + File.separator + destFile.getName());
            first = false;
          }
          break;
        case org.javarosa.core.model.Constants.DATATYPE_NULL: /*
                                                               * for nodes that
                                                               * have no data,
                                                               * or data type
                                                               * otherwise
                                                               * unknown
                                                               */
          if (current.repeatable) {
            if (prior == null || !current.getName().equals(prior.getName())) {
              // repeatable group...
              if (ec == null) {
                emitString(osw, first, null);
                first = false;
              } else {
                String uniqueGroupPath = uniquePath + "/" + getFullName(current, primarySet);
                emitString(osw, first, uniqueGroupPath);
                first = false;
                // first time processing this repeat group (ignore templates)
                List<Element> ecl = findElementList(submissionElement, current.getName());
                emitRepeatingGroupCsv(ecl, current, uniquePath,
                                                    uniqueGroupPath, instanceDir);
              }
            }
          } else if (current.getNumChildren() == 0 && current != lfd.getSubmissionElement()) {
            // assume fields that don't have children are string fields.
            if (ec == null) {
              emitString(osw, first, null);
              first = false;
            } else { 
              emitString(osw, first, getSubmissionValue(ec));
              first = false;
            }
          } else {
            /* one or more children -- this is a non-repeating group */
            first = emitSubmissionCsv(osw, ec, primarySet, current, first, uniquePath, instanceDir);
          }
          break;
        }
        prior = current;
      }
    }
    return first;
  }

  private void emitRepeatingGroupCsv(List<Element> groupElementList, TreeElement group,
      String uniqueParentPath, String uniqueGroupPath, File instanceDir)
      throws IOException {
    OutputStreamWriter osw = fileMap.get(group);
    int trueOrdinal = 1;
    for ( Element groupElement : groupElementList ) {
      String uniqueGroupInstancePath = uniqueGroupPath + "[" + trueOrdinal + "]";
      boolean first = true;
      first = emitSubmissionCsv(osw, groupElement, group, group, first, uniqueGroupInstancePath, instanceDir);
      emitString(osw, first, uniqueParentPath);
      emitString(osw, false, uniqueGroupInstancePath);
      emitString(osw, false, uniqueGroupPath);
      osw.append("\n");
      ++trueOrdinal;
    }
  }

  private boolean emitCsvHeaders(OutputStreamWriter osw, TreeElement primarySet,
      TreeElement treeElement, boolean first) throws IOException {
    // OK -- group with at least one element -- assume no value...
    // TreeElement list has the begin and end tags for the nested groups.
    // Swallow the end tag by looking to see if the prior and current
    // field names are the same.
    TreeElement prior = null;
    for (int i = 0; i < treeElement.getNumChildren(); ++i) {
      TreeElement current = (TreeElement) treeElement.getChildAt(i);
      if ((prior != null) && (prior.getName().equals(current.getName()))) {
        // it is the end-group tag... seems to happen with two adjacent repeat
        // groups
        log.info("repeating tag at " + i + " skipping " + current.getName());
        prior = current;
      } else {
        switch (current.dataType) {
        case org.javarosa.core.model.Constants.DATATYPE_TEXT:/**
           * Text question
           * type.
           */
        case org.javarosa.core.model.Constants.DATATYPE_INTEGER:/**
           * Numeric
           * question type. These are numbers without decimal points
           */
        case org.javarosa.core.model.Constants.DATATYPE_DECIMAL:/**
           * Decimal
           * question type. These are numbers with decimals
           */
        case org.javarosa.core.model.Constants.DATATYPE_DATE:/**
           * Date question
           * type. This has only date component without time.
           */
        case org.javarosa.core.model.Constants.DATATYPE_TIME:/**
           * Time question
           * type. This has only time element without date
           */
        case org.javarosa.core.model.Constants.DATATYPE_DATE_TIME:/**
           * Date and
           * Time question type. This has both the date and time components
           */
        case org.javarosa.core.model.Constants.DATATYPE_CHOICE:/**
           * This is a
           * question with alist of options where not more than one option can
           * be selected at a time.
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
          emitString(osw, first, getFullName(current, primarySet));
          first = false;
          break;
        case org.javarosa.core.model.Constants.DATATYPE_GEOPOINT:
          /**
           * Question with location answer.
           */
          emitString(osw, first, getFullName(current, primarySet) + "-Latitude");
          emitString(osw, false, getFullName(current, primarySet) + "-Longitude");
          emitString(osw, false, getFullName(current, primarySet) + "-Altitude");
          emitString(osw, false, getFullName(current, primarySet) + "-Accuracy");
          first = false;
          break;
        case org.javarosa.core.model.Constants.DATATYPE_NULL: /*
                                                               * for nodes that
                                                               * have no data,
                                                               * or data type
                                                               * otherwise
                                                               * unknown
                                                               */
          if (current.repeatable) {
            // repeatable group...
            emitString(osw, first, "SET-OF-" + getFullName(current, primarySet));
            first = false;
            processRepeatingGroupDefinition(current, primarySet);
          } else if (current.getNumChildren() == 0 && current != lfd.getSubmissionElement()) {
            // assume fields that don't have children are string fields.
            emitString(osw, first, getFullName(current, primarySet));
            first = false;
          } else {
            /* one or more children -- this is a non-repeating group */
            first = emitCsvHeaders(osw, primarySet, current, first);
          }
          break;
        }
        prior = current;
      }
    }
    return first;
  }

  private void processRepeatingGroupDefinition(TreeElement group, TreeElement primarySet)
      throws IOException {
    String formName = lfd.getFormName() + "-" + getFullName(group, primarySet);
    File topLevelCsv = new File(outputDir, formName + ".csv");
    FileOutputStream os = new FileOutputStream(topLevelCsv);
    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
    fileMap.put(group, osw);
    boolean first = true;
    first = emitCsvHeaders(osw, group, group, first);
    emitString(osw, first, "PARENT_KEY");
    emitString(osw, false, "KEY");
    emitString(osw, false, "SET-OF-" + group.getName());
    osw.append("\n");
  }

  private boolean processFormDefinition() {

    TreeElement submission = lfd.getSubmissionElement();

    String formName = lfd.getFormName();
    File topLevelCsv = new File(outputDir, formName + ".csv");
    FileOutputStream os;
    try {
      os = new FileOutputStream(topLevelCsv);
      OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
      fileMap.put(submission, osw);
      emitString(osw, true, "SubmissionDate");
      emitCsvHeaders(osw, submission, submission, false);
      emitString(osw, false, "KEY");
      if ( lfd.isEncryptedForm() ) {
        emitString(osw, false, "isValidated");
      }
      osw.append("\n");

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      EventBus.publish(new ExportProgressEvent("Unable to create csv file: "
          + topLevelCsv.getPath()));
      for (OutputStreamWriter w : fileMap.values()) {
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
      EventBus.publish(new ExportProgressEvent("Unable to create csv file: "
          + topLevelCsv.getPath()));
      for (OutputStreamWriter w : fileMap.values()) {
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
      EventBus.publish(new ExportProgressEvent("Unable to create csv file: "
          + topLevelCsv.getPath()));
      for (OutputStreamWriter w : fileMap.values()) {
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
    if (!submission.exists() || !submission.isFile()) {
      EventBus.publish(new ExportProgressEvent("Submission not found for instance directory: "
          + instanceDir.getPath()));
      return false;
    }
    EventBus.publish(new ExportProgressEvent("Processing instance: " + instanceDir.getName()));

    // If we are encrypted, be sure the temporary directory 
    // that will hold the unencrypted files is created and empty.
    // If we aren't encrypted, the temporary directory
    // is the same as the instance directory.
    
    File unEncryptedDir;
    if (lfd.isEncryptedForm()) {
      // create or clean-up the temp directory that will hold the unencrypted
      // files.
      unEncryptedDir = new File(instanceDir, "temp");

      if (unEncryptedDir.exists()) {
        // silently delete it...
        try {
          FileUtils.deleteDirectory(unEncryptedDir);
        } catch (IOException e) {
          e.printStackTrace();
          EventBus.publish(new ExportProgressEvent("Unable to delete stale temp directory: "
              + unEncryptedDir.getAbsolutePath()));
          return false;
        }
      }

      if (!unEncryptedDir.mkdirs()) {
        EventBus.publish(new ExportProgressEvent("Unable to create temp directory: "
            + unEncryptedDir.getAbsolutePath()));
        return false;
      }
    } else {
      unEncryptedDir = instanceDir;
    }

    // parse the xml document (this is the manifest if encrypted)...
    Document doc;
    boolean isValidated = false;
    
    try {
      doc = XmlManipulationUtils.parseXml(submission);
    } catch (ParsingException e) {
      e.printStackTrace();
      EventBus.publish(new ExportProgressEvent("Error parsing submission "
          + instanceDir.getName() + " Cause: " + e.toString()));
      return false;
    } catch (FileSystemException e) {
      e.printStackTrace();
      EventBus.publish(new ExportProgressEvent("Error parsing submission "
          + instanceDir.getName() + " Cause: " + e.toString()));
      return false;
    }

    String submissionDate = null;
    // extract the submissionDate, if present, from the attributes
    // of the root element of the submission or submission manifest (if encrypted).
    submissionDate = doc.getRootElement().getAttributeValue(null, "submissionDate");
    if (submissionDate == null || submissionDate.length() == 0) {
      submissionDate = null;
    } else {
      Date theDate = WebUtils.parseDate(submissionDate);
      DateFormat formatter = DateFormat.getDateTimeInstance();
      submissionDate = formatter.format(theDate);
    }

    // Beyond this point, we need to have a finally block that
    // will clean up any decrypted files whenever there is any 
    // failure.
    try {

      if (lfd.isEncryptedForm()) {
        // Decrypt the form and all its media files into the 
        // unEncryptedDir and validate the contents of all 
        // those files.
        // NOTE: this changes the value of 'doc'
        try {
          FileSystemUtils.DecryptOutcome outcome =
            FileSystemUtils.decryptAndValidateSubmission(doc, lfd.getPrivateKey(), 
              instanceDir, unEncryptedDir);
          doc = outcome.submission;
          isValidated = outcome.isValidated;
        } catch (ParsingException e) {
          e.printStackTrace();
          EventBus.publish(new ExportProgressEvent("Error decrypting submission "
              + instanceDir.getName() + " Cause: " + e.toString()));
          return false;
        } catch (FileSystemException e) {
          e.printStackTrace();
          EventBus.publish(new ExportProgressEvent("Error decrypting submission "
              + instanceDir.getName() + " Cause: " + e.toString()));
          return false;
        } catch (CryptoException e) {
          e.printStackTrace();
          EventBus.publish(new ExportProgressEvent("Error decrypting submission "
              + instanceDir.getName() + " Cause: " + e.toString()));
          return false;
        }
      }

      String instanceId = null;
      // find an instanceId to use...
      try {
        FormInstanceMetadata sim = XmlManipulationUtils.getFormInstanceMetadata(doc
            .getRootElement());
        instanceId = sim.instanceId;
      } catch (ParsingException e) {
        e.printStackTrace();
        EventBus.publish(new ExportProgressEvent("Could not extract metadata from submission: "
            + submission.getAbsolutePath() + " Cause: " + e.toString()));
        return false;
      }

      if (instanceId == null || instanceId.length() == 0) {
        // if we have no instanceID, and there isn't any in the file,
        // use the checksum as the id.  
        // NOTE: encrypted submissions always have instanceIDs.
        // This is for legacy non-OpenRosa forms.
        long checksum;
        try {
          checksum = FileUtils.checksumCRC32(submission);
        } catch (IOException e1) {
          e1.printStackTrace();
          EventBus.publish(new ExportProgressEvent("Failed during computing of crc: "
              + e1.getMessage()));
          return false;
        }
        instanceId = "crc32:" + Long.toString(checksum);
      }
      
      if ( terminationFuture.isCancelled() ) {
        EventBus.publish(new ExportProgressEvent("ABORTED"));
        return false;
      }

      // emit the csv record...
      try {
        OutputStreamWriter osw = fileMap.get(lfd.getSubmissionElement());

        emitString(osw, true, submissionDate);
        emitSubmissionCsv(osw, doc.getRootElement(), lfd.getSubmissionElement(),
            lfd.getSubmissionElement(), false, instanceId, unEncryptedDir);
        emitString(osw, false, instanceId);
        if ( lfd.isEncryptedForm() ) {
          emitString(osw, false, Boolean.toString(isValidated));
        }
        osw.append("\n");
        return true;

      } catch (IOException e) {
        e.printStackTrace();
        EventBus.publish(new ExportProgressEvent("Failed writing csv: " + e.getMessage()));
        return false;
      }
    } finally {
      if (lfd.isEncryptedForm()) {
        // destroy the temp directory and its contents...
        try {
          FileUtils.deleteDirectory(unEncryptedDir);
        } catch (IOException e) {
          e.printStackTrace();
          EventBus.publish(new ExportProgressEvent("Unable to remove decrypted files: "
              + e.getMessage()));
          return false;
        }
      }
    }
  }

  @Override
  public LocalFormDefinition getFormDefinition() {
    return lfd;
  }
}
