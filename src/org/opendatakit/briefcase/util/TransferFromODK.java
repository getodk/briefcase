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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.ParsingException;

public class TransferFromODK implements ITransferFromSourceAction {

  private static final Log log = LogFactory.getLog(TransferFromODK.class);

  final File odkOriginDir;
  final TerminationFuture terminationFuture;
  final List<FormStatus> formsToTransfer;

  public TransferFromODK(File odkOriginDir, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) {
    this.odkOriginDir = odkOriginDir;
    this.terminationFuture = terminationFuture;
    this.formsToTransfer = formsToTransfer;
  }

  /**
   * Given the OdkCollectFormDefinition within the FormStatus argument, try to match it up 
   * with an existing Briefcase storage form definition, or create a new Briefcase storage
   * form definition for it.
   * 
   * @param fs the form transfer status object for an ODK Collect form definition.
   * @return the Briefcase storage form definition.
   */
  private BriefcaseFormDefinition doResolveOdkCollectFormDefinition(FormStatus fs) {
    fs.setStatusString("resolving against briefcase form definitions", true);
    EventBus.publish(new FormStatusEvent(fs));

    OdkCollectFormDefinition formDef = (OdkCollectFormDefinition) fs.getFormDefinition();
    File odkFormDefFile = formDef.getFormDefinitionFile();

    BriefcaseFormDefinition briefcaseLfd;
    
    // copy form definition files from ODK to briefcase (scratch area)
    try {
      briefcaseLfd = BriefcaseFormDefinition.resolveAgainstBriefcaseDefn(odkFormDefFile, true);
      if ( briefcaseLfd.needsMediaUpdate() ) {
        File destinationFormMediaDir;
        try {
          destinationFormMediaDir = FileSystemUtils.getMediaDirectory(briefcaseLfd.getFormDirectory());
        } catch (FileSystemException e) {
          String msg = "unable to create media folder";
          log.error(msg, e);
          fs.setStatusString(msg + ": " + e.getMessage(), false);
          EventBus.publish(new FormStatusEvent(fs));
          return null;
        }
        // compose the ODK media directory...
        final String odkFormName = odkFormDefFile.getName().substring(0,
            odkFormDefFile.getName().lastIndexOf("."));
        String odkMediaName = odkFormName + "-media";
        File odkFormMediaDir = new File(odkFormDefFile.getParentFile(), odkMediaName);

        if (odkFormMediaDir.exists()) {
          FileUtils.copyDirectory(odkFormMediaDir, destinationFormMediaDir);
        }
        briefcaseLfd.clearMediaUpdate();
      }
    } catch (Exception e) {
      String msg = "unable to copy form definition and/or media folder";
      log.error(msg, e);
      fs.setStatusString(msg + ": " + e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(fs));
      return null;
    }
    
    return briefcaseLfd;
  }
  
  @Override
  public boolean doAction() {

    boolean allSuccessful = true;
    
    for (FormStatus fs : formsToTransfer) {
      boolean isSuccessful = true;
      try {
        if ( terminationFuture.isCancelled() ) {
          fs.setStatusString("aborted. Skipping fetch of form and submissions...", true);
          EventBus.publish(new FormStatusEvent(fs));
          return false;
        }
  
        BriefcaseFormDefinition briefcaseLfd = doResolveOdkCollectFormDefinition(fs);

        if ( briefcaseLfd == null ) {
          allSuccessful = isSuccessful = false;
          continue;
        }
        
        OdkCollectFormDefinition odkFormDef = (OdkCollectFormDefinition) fs.getFormDefinition();
        File odkFormDefFile = odkFormDef.getFormDefinitionFile();

        final String odkFormName = odkFormDefFile.getName().substring(0,
            odkFormDefFile.getName().lastIndexOf("."));

        DatabaseUtils formDatabase = null;
        try {
          formDatabase = DatabaseUtils.newInstance(briefcaseLfd.getFormDirectory());
  
          File destinationFormInstancesDir;
          try {
            destinationFormInstancesDir = FileSystemUtils.getFormInstancesDirectory(briefcaseLfd.getFormDirectory());
          } catch (FileSystemException e) {
            allSuccessful = isSuccessful = false;
            String msg = "unable to create instances folder";
            log.error(msg, e);
            fs.setStatusString(msg + ": " + e.getMessage(), false);
            EventBus.publish(new FormStatusEvent(fs));
            continue;
          }
          // we have the needed directory structure created...
    
          fs.setStatusString("preparing to retrieve instance data", true);
          EventBus.publish(new FormStatusEvent(fs));
    
          // construct up the list of folders that might have ODK form data.
          File odkFormInstancesDir = new File(odkFormDefFile.getParentFile().getParentFile(),
              "instances");
          // rely on ODK naming conventions to identify form data files...
          File[] odkFormInstanceDirs = odkFormInstancesDir.listFiles(new FileFilter() {
    
            @Override
            public boolean accept(File pathname) {
              boolean beginsWithFormName = pathname.getName().startsWith(odkFormName);
              if ( !beginsWithFormName ) return false;
              // skip the separator character, as it varies between 1.1.5, 1.1.6 and 1.1.7
              String afterName = pathname.getName().substring(odkFormName.length() + 1);
              // aftername should be a reasonable date though we allow extra stuff at the end...
              // protects against someone having "formname" and "formname_2"
              // and us mistaking "formname_2_2009-01-02_15_10_03" as containing
              // instance data for "formname" instead of "formname_2"
              boolean outcome = afterName.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}.*");
              return outcome;
            }
          });
    
          if ( odkFormInstanceDirs != null ) {
            int instanceCount = 1;
            for (File dir : odkFormInstanceDirs) {
              if ( terminationFuture.isCancelled() ) {
                allSuccessful = isSuccessful = false;
                fs.setStatusString("aborting retrieving submissions...", true);
                EventBus.publish(new FormStatusEvent(fs));
                return false;
              }
      
              // 1.1.8 -- submission is saved as submission.xml.
              // full instance data is stored as directoryName.xml (as is the convention in 1.1.5, 1.1.7)
              String instanceId = null;
              File fullXml = new File(dir, dir.getName() + ".xml");
              File xml = new File(dir, "submission.xml");
              if ( !xml.exists() && fullXml.exists() ) {
            	  xml = fullXml; // e.g., 1.1.5, 1.1.7
              }

              // this is a hack added to support easier generation of large test cases where we 
              // copy a single instance directory repeatedly.  Normally the xml submission file
              // has the name of the enclosing directory, but if you copy directories, this won't
              // be the case.  In this instance, if there is one xml file in the directory,
              // rename it to match the directory name.
              if (!xml.exists()) {
            	  File[] xmlFiles = dir.listFiles(new FilenameFilter() {
    
    				@Override
    				public boolean accept(File dir, String name) {
    					return name.endsWith(".xml");
    				}});
            	  
            	  if ( xmlFiles.length == 1 ) {
            		  try {
    					FileUtils.moveFile(xmlFiles[0], xml);
                      } catch (IOException e) {
                        allSuccessful = isSuccessful = false;
                        String msg = "unable to rename form instance xml";
                        log.error(msg, e);
                        fs.setStatusString(msg + ": " + e.getMessage(), false);
                        EventBus.publish(new FormStatusEvent(fs));
                        continue;
                      }
                  }
              }
              
              if (xml.exists()) {
                //Check if the instance has an instanceID
                try {
                  XmlManipulationUtils.FormInstanceMetadata formInstanceMetadata =
                          XmlManipulationUtils.getFormInstanceMetadata(XmlManipulationUtils.parseXml(xml)
                          .getRootElement());
                  instanceId = formInstanceMetadata.instanceId;
                } catch (ParsingException e) {
                  log.error("failed to get instance id from submission", e);
                }

                // OK, we can copy the directory off...
                // Briefcase instances directory name is arbitrary.
                // Rename the xml within that to always be "submission.xml"
                // to remove the correspondence to the directory name.
                File scratchInstance = FileSystemUtils.getFormSubmissionDirectory(destinationFormInstancesDir, dir.getName());
                String safeName = scratchInstance.getName();
                
                int i = 2;
                boolean same = false;
                while (scratchInstance.exists()) {
                  File[] contents = scratchInstance.listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                      return name.endsWith(".xml");
                    }});
                  if ( contents == null || contents.length == 0 ) break;
                  if (contents.length == 1){
                    String itsInstanceId = null;
                    try {
                      XmlManipulationUtils.FormInstanceMetadata formInstanceMetadata =
                              XmlManipulationUtils.getFormInstanceMetadata(XmlManipulationUtils.parseXml(contents[0])
                                      .getRootElement());
                      itsInstanceId = formInstanceMetadata.instanceId;

                      //Check if the above ODK file(xml) instanceId is equal to this briefcase file instanceId then compare their MD5 hashes
                      //if yes don't copy it, skip to next file
                      if (itsInstanceId != null && itsInstanceId.equals(instanceId) &&
                              FileSystemUtils.getMd5Hash(xml).equals(FileSystemUtils.getMd5Hash(contents[0]))) {
                        same = true;
                        break;
                      }
                    } catch (ParsingException e) {
                      log.error("failed to parse submission", e);
                    }
                  }

                  scratchInstance = new File(destinationFormInstancesDir, safeName + "-" + Integer.toString(i));
                  i++;
                }

                if (same) {
                  fs.setStatusString("already present - skipping: " + xml.getName(), true);
                  EventBus.publish(new FormStatusEvent(fs));
                  continue;
                }

                try {
                  FileUtils.copyDirectory(dir, scratchInstance);
                } catch (IOException e) {
                  allSuccessful = isSuccessful = false;
                  String msg = "unable to copy saved instance";
                  log.error(msg, e);
                  fs.setStatusString(msg + ": " + e.getMessage(), false);
                  EventBus.publish(new FormStatusEvent(fs));
                  continue;
                }
                
                if ( xml.equals(fullXml) ) {
                	// need to rename
    	            File odkSubmissionFile = new File(scratchInstance, fullXml.getName());
    	            File scratchSubmissionFile = new File(scratchInstance, "submission.xml");

    	            try {
    	              FileUtils.moveFile(odkSubmissionFile, scratchSubmissionFile);
    	            } catch (IOException e) {
                      allSuccessful = isSuccessful = false;
                      String msg = "unable to rename submission file to submission.xml";
                      log.error(msg, e);
                      fs.setStatusString(msg + ": " + e.getMessage(), false);
                      EventBus.publish(new FormStatusEvent(fs));
                      continue;
                    }
                } else {
                	// delete the full xml file (keep only the submission.xml)
                	File odkSubmissionFile = new File(scratchInstance, fullXml.getName());
                	odkSubmissionFile.delete();
                }

                fs.setStatusString(String.format("retrieving (%1$d)", instanceCount), true);
                EventBus.publish(new FormStatusEvent(fs));
                ++instanceCount;
              }
            }
          }
          
        } catch ( SQLException | FileSystemException e ) {
          allSuccessful = isSuccessful = false;
          String msg = "unable to open form database";
          log.error(msg, e);
          fs.setStatusString(msg + ": " + e.getMessage(), false);
          EventBus.publish(new FormStatusEvent(fs));
          continue;
        } finally {
          if ( formDatabase != null ) {
            try {
              formDatabase.close();
            } catch ( SQLException e) {
              allSuccessful = isSuccessful = false;
              String msg = "unable to close form database";
              log.error(msg, e);
              fs.setStatusString(msg + ": " + e.getMessage(), false);
              EventBus.publish(new FormStatusEvent(fs));
              continue;
            }
          }
        }
      } finally {
        if ( isSuccessful ) {
          fs.setStatusString(ServerFetcher.SUCCESS_STATUS, true);
          EventBus.publish(new FormStatusEvent(fs));
        } else {
          fs.setStatusString(ServerFetcher.FAILED_STATUS, true);
          EventBus.publish(new FormStatusEvent(fs));
        }
      }
    }
    return allSuccessful;
  }

  @Override
  public boolean isSourceDeletable() {
    return true;
  }
}
