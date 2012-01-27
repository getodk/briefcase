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
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;

public class TransferFromODK implements ITransferFromSourceAction {

  final File odkOriginDir;
  final TerminationFuture terminationFuture;
  final List<FormStatus> formsToTransfer;

  public TransferFromODK(File odkOriginDir, TerminationFuture terminationFuture, List<FormStatus> formsToTransfer) {
    this.odkOriginDir = odkOriginDir;
    this.terminationFuture = terminationFuture;
    this.formsToTransfer = formsToTransfer;
  }

  private boolean doRetrieveFormDefinition(FormStatus fs) {
    fs.setStatusString("retrieving form definition", true);
    EventBus.publish(new FormStatusEvent(fs));

    LocalFormDefinition formDef = (LocalFormDefinition) fs.getFormDefinition();
    File odkFormDefFile = formDef.getFormDefinitionFile();

    // compose the ODK media directory...
    final String odkFormName = odkFormDefFile.getName().substring(0,
        odkFormDefFile.getName().lastIndexOf("."));
    String odkMediaName = odkFormName + "-media";
    File odkFormMediaDir = new File(odkFormDefFile.getParentFile(), odkMediaName);

    File destinationFormDefFile;
    try {
      destinationFormDefFile = FileSystemUtils.getFormDefinitionFile(fs.getFormName());
    } catch (FileSystemException e) {
      e.printStackTrace();
      fs.setStatusString("unable to create form folder: " + e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(fs));
      return false;
    }
    File destinationFormMediaDir;
    try {
      destinationFormMediaDir = FileSystemUtils.getMediaDirectory(fs.getFormName());
    } catch (FileSystemException e) {
      e.printStackTrace();
      fs.setStatusString("unable to create media folder: " + e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(fs));
      return false;
    }
    
    // copy form definition files from ODK to briefcase (scratch area)
    try {
      FileUtils.copyFile(odkFormDefFile, destinationFormDefFile);
      if (odkFormMediaDir.exists()) {
        FileUtils.copyDirectory(odkFormMediaDir, destinationFormMediaDir);
      }
    } catch ( Exception e ) {
      e.printStackTrace();
      fs.setStatusString("unable to copy form definition and/or media folder: " + e.getMessage(), false);
      EventBus.publish(new FormStatusEvent(fs));
      return false;
    }
    return true;
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
  
        boolean outcome = doRetrieveFormDefinition(fs);

        if ( !outcome ) {
          allSuccessful = isSuccessful = false;
          continue;
        }

        LocalFormDefinition formDef = (LocalFormDefinition) fs.getFormDefinition();
        File odkFormDefFile = formDef.getFormDefinitionFile();
        final String odkFormName = odkFormDefFile.getName().substring(0,
            odkFormDefFile.getName().lastIndexOf("."));

        DatabaseUtils formDatabase = null;
        try {
          formDatabase = new DatabaseUtils(FileSystemUtils.getFormDatabase(fs.getFormName()));
  
          File destinationFormInstancesDir;
          try {
            destinationFormInstancesDir = FileSystemUtils.getFormInstancesDirectory(fs.getFormName());
          } catch (FileSystemException e) {
            e.printStackTrace();
            allSuccessful = isSuccessful = false;
            fs.setStatusString("unable to create instances folder: " + e.getMessage(), false);
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
              //
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
    					e.printStackTrace();
    					allSuccessful = isSuccessful = false;
    		         fs.setStatusString("unable to rename form instance xml: " + e.getMessage(), false);
    			      EventBus.publish(new FormStatusEvent(fs));
    			      continue;
    				}
            	  }
              }
              
              if (xml.exists()) {
                // OK, we can copy the directory off...
                // Briefcase instances directory name is arbitrary.
                // Rename the xml within that to always be "submission.xml"
                // to remove the correspondence to the directory name.
                File scratchInstance = FileSystemUtils.getFormSubmissionDirectory(destinationFormInstancesDir, dir.getName());
                String safeName = scratchInstance.getName();
                
                int i = 2;
                while (scratchInstance.exists()) {
                  String[] contents = scratchInstance.list();
                  if ( contents == null || contents.length == 0 ) break;
                  scratchInstance = new File(destinationFormInstancesDir, safeName + "-" + Integer.toString(i));
                  i++;
                }
                try {
                  FileUtils.copyDirectory(dir, scratchInstance);
                } catch (IOException e) {
                  e.printStackTrace();
                  allSuccessful = isSuccessful = false;
                  fs.setStatusString("unable to copy saved instance: " + e.getMessage(), false);
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
    	              e.printStackTrace();
    	              allSuccessful = isSuccessful = false;
    	              fs.setStatusString("unable to rename submission file to submission.xml: " + e.getMessage(), false);
    	              EventBus.publish(new FormStatusEvent(fs));
    	              continue;
    	            }
                } else {
                	// delete the full xml file (keep only the submission.xml)
                	File odkSubmissionFile = new File(scratchInstance, fullXml.getName());
                	odkSubmissionFile.delete();
                }
                
                fs.putScratchFromMapping(scratchInstance, dir);
                fs.setStatusString(String.format("retrieving (%1$d)", instanceCount), true);
                EventBus.publish(new FormStatusEvent(fs));
                ++instanceCount;
              }
            }
          }
          
        } catch ( FileSystemException e ) {
          e.printStackTrace();
          allSuccessful = isSuccessful = false;
          fs.setStatusString("unable to open form database: " + e.getMessage(), false);
          EventBus.publish(new FormStatusEvent(fs));
          continue;
        } finally {
          if ( formDatabase != null ) {
            try {
              formDatabase.close();
            } catch ( SQLException e) {
              e.printStackTrace();
              allSuccessful = isSuccessful = false;
              fs.setStatusString("unable to close form database: " + e.getMessage(), false);
              EventBus.publish(new FormStatusEvent(fs));
              continue;
            }
          }
        }
      } finally {
        if ( isSuccessful ) {
          fs.setStatusString("SUCCESS!", true);
          EventBus.publish(new FormStatusEvent(fs));
        } else {
          fs.setStatusString("FAILED.", true);
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