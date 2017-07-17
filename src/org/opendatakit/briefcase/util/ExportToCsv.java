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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.EventBus;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportProgressPercentageEvent;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.XmlManipulationUtils.FormInstanceMetadata;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportToCsv implements ITransformFormAction {

  private static final String MEDIA_DIR = "media";

  private static final Log log = LogFactory.getLog(ExportToCsv.class);

  File outputDir;
  File outputMediaDir;
  String baseFilename;
  BriefcaseFormDefinition briefcaseLfd;
  TerminationFuture terminationFuture;
  Map<TreeElement, OutputStreamWriter> fileMap = new HashMap<TreeElement, OutputStreamWriter>();
  Map<String, String> fileHashMap = new HashMap<String, String>();
  
  boolean exportMedia = true;
  Date startDate;
  Date endDate;
  boolean overwrite = false;
  int totalFilesSkipped = 0;
  int totalInstances = 0;
  int processedInstances = 0;
  
  
  // Default briefcase constructor
  public ExportToCsv(File outputDir, BriefcaseFormDefinition lfd, TerminationFuture terminationFuture) {
    this(outputDir, lfd, terminationFuture, lfd.getFormName(), true, false, null, null);
  }
  
  public ExportToCsv(File outputDir, BriefcaseFormDefinition lfd, TerminationFuture terminationFuture, String filename, boolean exportMedia, Boolean overwrite, Date start, Date end) {
     this.outputDir = outputDir;
     this.outputMediaDir = new File(outputDir, MEDIA_DIR);
     this.briefcaseLfd = lfd;
     this.terminationFuture = terminationFuture;

     // Strip .csv, it gets added later
     if (filename.endsWith(".csv")) {
         filename = filename.substring(0, filename.length()-4);
     }
     this.baseFilename = filename;
     this.exportMedia = exportMedia;
     this.overwrite = overwrite;
     this.startDate = start;
     this.endDate = end;
  }

  @Override
  public boolean doAction() {
    boolean allSuccessful = true;
    File instancesDir;
    try {
      instancesDir = FileSystemUtils.getFormInstancesDirectory(briefcaseLfd.getFormDirectory());
    } catch (FileSystemException e) {
      String msg = "Unable to access instances directory of form";
      log.error(msg, e);
      EventBus.publish(new ExportProgressEvent(msg));
      return false;
    }

    if (!outputDir.exists()) {
      if (!outputDir.mkdir()) {
        EventBus.publish(new ExportProgressEvent("Unable to create destination directory"));
        return false;
      }
    }

    if (!processFormDefinition()) {
      // weren't able to initialize the csv file...
      return false;
    }

    File[] instances = instancesDir.listFiles(new FileFilter() {
      public boolean accept(File file) {
        // do we have a folder with submission.xml inside
        return file.isDirectory() && new File(file, "submission.xml").exists();
      }
    });
    totalInstances = instances.length;

    // Sorts the instances by the submission date. If no submission date, we
    // assume it to be latest.
    if (instances != null) {
      Arrays.sort(instances, new Comparator<File>() {
        public int compare(File f1, File f2) {
          try {
            if (f1.isDirectory() && f2.isDirectory()) {
              File submission1 = new File(f1, "submission.xml");
              String submissionDate1String = XmlManipulationUtils.parseXml(submission1).getRootElement()
                  .getAttributeValue(null, "submissionDate");

              File submission2 = new File(f2, "submission.xml");
              String submissionDate2String = XmlManipulationUtils.parseXml(submission2).getRootElement()
                  .getAttributeValue(null, "submissionDate");

              Date submissionDate1 = StringUtils.isNotEmptyNotNull(submissionDate1String)
                  ? WebUtils.parseDate(submissionDate1String) : new Date();
              Date submissionDate2 = StringUtils.isNotEmptyNotNull(submissionDate2String)
                  ? WebUtils.parseDate(submissionDate2String) : new Date();
              return submissionDate1.compareTo(submissionDate2);
            }
          } catch (ParsingException | FileSystemException e) {
            log.error("failed to sort submissions", e);
          }
          return 0;
        }
      });
    }

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
        String msg = "Error flushing csv file";
        EventBus.publish(new ExportProgressEvent(msg));
        log.error(msg, e);
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

  private String getFullName(AbstractTreeElement e, TreeElement group) {
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

  private String getSubmissionValue(EncryptionInformation ei, TreeElement model, Element element) {
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
    String rawElement = b.toString();

    // Field-level encryption support -- experimental
    if ( JavaRosaParserWrapper.isEncryptedField(model) ) {

      InputStreamReader isr = null;
      try {
        Cipher c = ei.getCipher("field:" + model.getName(), model.getName());

        isr = new InputStreamReader(new CipherInputStream(
                  new ByteArrayInputStream(Base64.decodeBase64(rawElement)), c),"UTF-8");

        b.setLength(0);
        int ch;
        while ( (ch = isr.read()) != -1 ) {
          char theChar = (char) ch;
          b.append(theChar);
        }
        return b.toString();

      } catch (IOException e) {
        log.debug(" element name: " + model.getName() + " exception: " + e);
      } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
        log.debug(" element name: " + model.getName() + " exception: " + e, e);
      } finally {
        if (isr != null) {
          try {
            isr.close();
          } catch (IOException e) {
            log.error("failed to close reader", e);
          }
        }
      }
    }
    return rawElement;
  }

  private boolean emitSubmissionCsv(OutputStreamWriter osw, EncryptionInformation ei,
      Element submissionElement,
      TreeElement primarySet, TreeElement treeElement, boolean first, String uniquePath,
      File instanceDir) throws IOException {
    // OK -- group with at least one element -- assume no value...
    // TreeElement list has the begin and end tags for the nested groups.
    // Swallow the end tag by looking to see if the prior and current
    // field names are the same.
    TreeElement prior = null;
    for (int i = 0; i < treeElement.getNumChildren(); ++i) {
      TreeElement current = (TreeElement) treeElement.getChildAt(i);
      log.debug(" element name: " + current.getName());
      if ((prior != null) && (prior.getName().equals(current.getName()))) {
        // it is the end-group tag... seems to happen with two adjacent repeat
        // groups
        log.info("repeating tag at " + i + " skipping " + current.getName());
        prior = current;
      } else {
        Element ec = findElement(submissionElement, current.getName());
        switch (current.getDataType()) {
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
            emitString(osw, first, getSubmissionValue(ei,current,ec));
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
            String value = getSubmissionValue(ei,current,ec);
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
            String value = getSubmissionValue(ei,current,ec);
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
            String value = getSubmissionValue(ei,current,ec);
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
          String compositeValue = (ec == null) ? null : getSubmissionValue(ei,current,ec);
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
          String binaryFilename = getSubmissionValue(ei,current,ec);
          if (binaryFilename == null || binaryFilename.length() == 0) {
            emitString(osw, first, null);
            first = false;
          } else {
            if (exportMedia) {
               if (!outputMediaDir.exists()) {
                  if (!outputMediaDir.mkdir()) {
                    EventBus.publish(new ExportProgressEvent("Unable to create destination media directory"));
                    return false;
                  }
               }

               int dotIndex = binaryFilename.lastIndexOf(".");
               String namePart = (dotIndex == -1) ? binaryFilename : binaryFilename.substring(0,
                   dotIndex);
               String extPart = (dotIndex == -1) ? "" : binaryFilename.substring(dotIndex);
   
               File binaryFile = new File(instanceDir, binaryFilename);
               String destBinaryFilename = binaryFilename;
               int version = 1;
               File destFile = new File(outputMediaDir, destBinaryFilename);
               boolean exists = false;
                String binaryFileHash = null;
                String destFileHash = null;
 
                if (destFile.exists() && binaryFile.exists()) {
                   binaryFileHash = FileSystemUtils.getMd5Hash(binaryFile);
                     
                   while (destFile.exists()) {
                    /* check if the contents of the destFile and binaryFile is same
                     * if yes, skip the export of such file
                     */
 
                    if (fileHashMap.containsKey(destFile.getName())) {
                       destFileHash = fileHashMap.get(destFile.getName());
                    } else {
                       destFileHash = FileSystemUtils.getMd5Hash(destFile);
                       if (destFileHash != null) {
                         fileHashMap.put(destFile.getName(), destFileHash);
                       }
                    }
 
                    if (binaryFileHash != null && destFileHash != null && destFileHash.equals(binaryFileHash)) {
                     exists = true;
                     break;
                    }
 
                    destBinaryFilename = namePart + "-" + (++version) + extPart;
                    destFile = new File(outputMediaDir, destBinaryFilename);
                  }
                }
               if (binaryFile.exists() && exists == false) {
                 FileUtils.copyFile(binaryFile, destFile);
               }
               emitString(osw, first, MEDIA_DIR + File.separator + destFile.getName());
            } else {
                emitString(osw, first, binaryFilename);
            }

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
          if (current.isRepeatable()) {
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
                emitRepeatingGroupCsv(ei, ecl, current, uniquePath,
                                                    uniqueGroupPath, instanceDir);
              }
            }
          } else if (current.getNumChildren() == 0 && current != briefcaseLfd.getSubmissionElement()) {
            // assume fields that don't have children are string fields.
            if (ec == null) {
              emitString(osw, first, null);
              first = false;
            } else {
              emitString(osw, first, getSubmissionValue(ei,current,ec));
              first = false;
            }
          } else {
            /* one or more children -- this is a non-repeating group */
            first = emitSubmissionCsv(osw, ei, ec, primarySet, current, first, uniquePath, instanceDir);
          }
          break;
        }
        prior = current;
      }
    }
    return first;
  }

  private void emitRepeatingGroupCsv(EncryptionInformation ei, List<Element> groupElementList, TreeElement group,
      String uniqueParentPath, String uniqueGroupPath, File instanceDir)
      throws IOException {
    OutputStreamWriter osw = fileMap.get(group);
    int trueOrdinal = 1;
    for ( Element groupElement : groupElementList ) {
      String uniqueGroupInstancePath = uniqueGroupPath + "[" + trueOrdinal + "]";
      boolean first = true;
      first = emitSubmissionCsv(osw, ei, groupElement, group, group, first, uniqueGroupInstancePath, instanceDir);
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
        switch (current.getDataType()) {
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
          if (current.isRepeatable()) {
            // repeatable group...
            emitString(osw, first, "SET-OF-" + getFullName(current, primarySet));
            first = false;
            processRepeatingGroupDefinition(current, primarySet, true);
          } else if (current.getNumChildren() == 0 && current != briefcaseLfd.getSubmissionElement()) {
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

  private void populateRepeatGroupsIntoFileMap(TreeElement primarySet,
      TreeElement treeElement) throws IOException {
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
        switch (current.getDataType()) {
        default:
          break;
        case org.javarosa.core.model.Constants.DATATYPE_NULL: 
          /* for nodes that have no data, or data type otherwise unknown */
          if (current.isRepeatable()) {
            processRepeatingGroupDefinition(current, primarySet, false);
          } else if (current.getNumChildren() == 0 && current != briefcaseLfd.getSubmissionElement()) {
            // ignore - string type
          } else {
            /* one or more children -- this is a non-repeating group */
            populateRepeatGroupsIntoFileMap(primarySet, current);
          }
          break;
        }
        prior = current;
      }
    }
  }

  private void processRepeatingGroupDefinition(TreeElement group, TreeElement primarySet, boolean emitCsvHeaders)
      throws IOException {
    String formName = baseFilename + "-" + getFullName(group, primarySet);
    File topLevelCsv = new File(outputDir, safeFilename(formName) + ".csv");
    FileOutputStream os = new FileOutputStream(topLevelCsv, !overwrite);
    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
    fileMap.put(group, osw);
    if ( emitCsvHeaders ) {
      boolean first = true;
      first = emitCsvHeaders(osw, group, group, first);
      emitString(osw, first, "PARENT_KEY");
      emitString(osw, false, "KEY");
      emitString(osw, false, "SET-OF-" + group.getName());
      osw.append("\n");
    } else {
      populateRepeatGroupsIntoFileMap(group, group);
    }
  }

  private String safeFilename(String name) {
    return name.replaceAll("\\p{Punct}", "_").replace("\\p{Space}", " ");
  }

  private boolean processFormDefinition() {

    TreeElement submission = briefcaseLfd.getSubmissionElement();

    String formName = baseFilename;
    File topLevelCsv = new File(outputDir, safeFilename(formName) + ".csv");
    boolean exists = topLevelCsv.exists();
    FileOutputStream os;
    try {
      os = new FileOutputStream(topLevelCsv, !overwrite);
      OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
      fileMap.put(submission, osw);
      // only write headers if overwrite is set, or creating file for the first time
      if (overwrite || !exists) {
          emitString(osw, true, "SubmissionDate");
          emitCsvHeaders(osw, submission, submission, false);
          emitString(osw, false, "KEY");
          if ( briefcaseLfd.isFileEncryptedForm() ) {
              emitString(osw, false, "isValidated");
          }
          osw.append("\n");
       } else {
         populateRepeatGroupsIntoFileMap(submission, submission);
       }

    } catch (IOException e) {
      String msg = "Unable to create csv file: " + topLevelCsv.getPath();
      log.error(msg, e);
      EventBus.publish(new ExportProgressEvent(msg));
      for (OutputStreamWriter w : fileMap.values()) {
        try {
          w.close();
        } catch (IOException e1) {
          log.warn("failed to close writer", e1);
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

    processedInstances++;

    EventBus.publish(new ExportProgressEvent("Processing instance: " + instanceDir.getName()));
    EventBus.publish(new ExportProgressPercentageEvent((processedInstances * 100.0) / totalInstances));

    // If we are encrypted, be sure the temporary directory
    // that will hold the unencrypted files is created and empty.
    // If we aren't encrypted, the temporary directory
    // is the same as the instance directory.

    File unEncryptedDir;
    if (briefcaseLfd.isFileEncryptedForm()) {
      // create or clean-up the temp directory that will hold the unencrypted
      // files. Do this in the outputDir so that the briefcase storage location
      // can be a read-only network mount. issue 676.
      unEncryptedDir = new File(outputDir, ".temp");

      if (unEncryptedDir.exists()) {
        // silently delete it...
        try {
          FileUtils.deleteDirectory(unEncryptedDir);
        } catch (IOException e) {
          String msg = "Unable to delete stale temp directory: " + unEncryptedDir.getAbsolutePath();
          log.warn(msg, e);
          EventBus.publish(new ExportProgressEvent(msg));
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
    } catch (ParsingException | FileSystemException e) {
      String msg = "Error parsing submission " + instanceDir.getName();
      log.error(msg, e);
      EventBus.publish(new ExportProgressEvent(msg + " Cause: " + e.toString()));
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
      
      // just return true to skip records out of range
      if (startDate != null && theDate.before(startDate)) {
          log.info("Submission date is before specified, skipping: " + instanceDir.getName());
          return true;
      }
      if (endDate != null && theDate.after(endDate)) {
          log.info("Submission date is after specified, skipping: " + instanceDir.getName());
          return true;
      }
      // don't export records without dates if either date is set
      if ((startDate != null || endDate != null) && submissionDate == null) {
          log.info("No submission date found, skipping: " + instanceDir.getName());
          return true;
      }
    }
    
    // Beyond this point, we need to have a finally block that
    // will clean up any decrypted files whenever there is any
    // failure.
    try {

      if (briefcaseLfd.isFileEncryptedForm()) {
        // Decrypt the form and all its media files into the
        // unEncryptedDir and validate the contents of all
        // those files.
        // NOTE: this changes the value of 'doc'
        try {
          FileSystemUtils.DecryptOutcome outcome =
            FileSystemUtils.decryptAndValidateSubmission(doc, briefcaseLfd.getPrivateKey(),
              instanceDir, unEncryptedDir);
          doc = outcome.submission;
          isValidated = outcome.isValidated;
        } catch (ParsingException | CryptoException | FileSystemException e) {
          //Was unable to parse file or decrypt file or a file system error occurred
          //Hence skip this instance
          EventBus.publish(new ExportProgressEvent("Error decrypting submission "
                  + instanceDir.getName() + " Cause: " + e.toString() + " skipping...."));

          log.info("Error decrypting submission "
                  + instanceDir.getName() + " Cause: " + e.toString());

          //update total number of files skipped
          totalFilesSkipped++;
          return true;
        }
      }

      String instanceId = null;
      String base64EncryptedFieldKey = null;
      // find an instanceId to use...
      try {
        FormInstanceMetadata sim = XmlManipulationUtils.getFormInstanceMetadata(doc.getRootElement());
        instanceId = sim.instanceId;
        base64EncryptedFieldKey = sim.base64EncryptedFieldKey;
      } catch (ParsingException e) {
        String msg = "Could not extract metadata from submission: " + submission.getAbsolutePath();
        log.error(msg, e);
        EventBus.publish(new ExportProgressEvent(msg + " Cause: " + e.toString()));
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
          String msg = "Failed during computing of crc";
          log.error(msg, e1);
          EventBus.publish(new ExportProgressEvent(msg + ": " + e1.getMessage()));
          return false;
        }
        instanceId = "crc32:" + Long.toString(checksum);
      }

      if ( terminationFuture.isCancelled() ) {
        EventBus.publish(new ExportProgressEvent("ABORTED"));
        return false;
      }

      EncryptionInformation ei = null;
      if ( base64EncryptedFieldKey != null ) {
        try {
          ei = new EncryptionInformation(base64EncryptedFieldKey, instanceId, briefcaseLfd.getPrivateKey());
        } catch (CryptoException e) {
          String msg = "Error establishing field decryption for submission " + instanceDir.getName();
          log.error(msg, e);
          EventBus.publish(new ExportProgressEvent(msg + " Cause: " + e.toString()));
          return false;
        }
      }

      // emit the csv record...
      try {
        OutputStreamWriter osw = fileMap.get(briefcaseLfd.getSubmissionElement());

        emitString(osw, true, submissionDate);
        emitSubmissionCsv(osw, ei, doc.getRootElement(), briefcaseLfd.getSubmissionElement(),
            briefcaseLfd.getSubmissionElement(), false, instanceId, unEncryptedDir);
        emitString(osw, false, instanceId);
        if ( briefcaseLfd.isFileEncryptedForm() ) {
          emitString(osw, false, Boolean.toString(isValidated));
          if ( !isValidated ) {
            EventBus.publish(new ExportProgressEvent("Decrypted submission "
                + instanceDir.getName() + " may be missing attachments and could not be validated."));
          }
        }
        osw.append("\n");
        return true;

      } catch (IOException e) {
        String msg = "Failed writing csv";
        log.error(msg, e);
        EventBus.publish(new ExportProgressEvent(msg + ": " + e.getMessage()));
        return false;
      }
    } finally {
      if (briefcaseLfd.isFileEncryptedForm()) {
        // destroy the temp directory and its contents...
        try {
          FileUtils.deleteDirectory(unEncryptedDir);
        } catch (IOException e) {
          String msg = "Unable to remove decrypted files";
          log.error(msg, e);
          EventBus.publish(new ExportProgressEvent(msg + ": " + e.getMessage()));
          return false;
        }
      }
    }
  }

  @Override
  public BriefcaseFormDefinition getFormDefinition() {
    return briefcaseLfd;
  }

  @Override
  public FilesSkipped totalFilesSkipped() {
    //Determine if all files where skipped or just some
    //Note that if totalInstances = 0 then no files were skipped
    if (totalInstances == 0 || totalFilesSkipped == 0) {
      return FilesSkipped.NONE;
    }
    if (totalFilesSkipped == totalInstances) {
      return FilesSkipped.ALL;
    } else {
     return FilesSkipped.SOME;
    }
  }
}
