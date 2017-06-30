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

package org.opendatakit.briefcase.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.security.PrivateKey;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.EventBus;
import org.javarosa.core.model.instance.TreeElement;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa.DifferenceResult;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.JavaRosaParserWrapper;

public class BriefcaseFormDefinition implements IFormDefinition, Serializable {

  private static final Log log = LogFactory.getLog(BriefcaseFormDefinition.class);
  private final File formFolder;
  private final File revisedFormFile;
  private boolean needsMediaUpdate = false;
  private JavaRosaParserWrapper formDefn;
  private PrivateKey privateKey = null;

  private static final String readFile(File formDefinitionFile) throws BadFormDefinition {
    StringBuilder xmlBuilder = new StringBuilder();
    BufferedReader rdr = null;
    try {
      rdr = new BufferedReader(new InputStreamReader(new FileInputStream(formDefinitionFile),
          "UTF-8"));
      String line = rdr.readLine();
      while (line != null) {
        xmlBuilder.append(line);
        line = rdr.readLine();
      }
    } catch (FileNotFoundException e) {
      throw new BadFormDefinition("Form not found");
    } catch (IOException e) {
      throw new BadFormDefinition("Unable to read form");
    } finally {
      if (rdr != null) {
        try {
          rdr.close();
        } catch (IOException e) {
          log.warn("failed to close reader", e);
        }
      }
    }
    String inputXml = xmlBuilder.toString();
    return inputXml;
  }

  public boolean needsMediaUpdate() {
    return needsMediaUpdate;
  }

  public void clearMediaUpdate() {
    needsMediaUpdate = false;
  }

  public static final BriefcaseFormDefinition resolveAgainstBriefcaseDefn(File tmpFormFile)
      throws BadFormDefinition {
    return resolveAgainstBriefcaseDefn(tmpFormFile, false);
  }

  public static final BriefcaseFormDefinition resolveAgainstBriefcaseDefn(File tmpFormFile,
      boolean copyFile) throws BadFormDefinition {

    if (!tmpFormFile.exists()) {
      throw new BadFormDefinition("Form directory does not contain form");
    }
    // parse the temp file into a form definition...
    boolean badForm = false;
    JavaRosaParserWrapper newDefn;
    File briefcaseFormDirectory;
    File briefcaseFormFile;
    try {
      newDefn = new JavaRosaParserWrapper(tmpFormFile, readFile(tmpFormFile));
      briefcaseFormDirectory = FileSystemUtils.getFormDirectory(newDefn.getFormName());
      briefcaseFormFile = FileSystemUtils.getFormDefinitionFile(briefcaseFormDirectory);
    } catch (ODKIncompleteSubmissionData e) {
      log.warn("bad form definition", e);
      try {
        badForm = true;
        newDefn = null;
        briefcaseFormDirectory = FileSystemUtils.getFormDirectory("_badForm");
        briefcaseFormFile = FileSystemUtils.getFormDefinitionFile(briefcaseFormDirectory);
      } catch (FileSystemException ex) {
        log.error("failed to establish storage location for bad form", e);
        throw new BadFormDefinition(ex);
      }
    } catch (FileSystemException e) {
      log.error("failed to establish storage location for form", e);
      throw new BadFormDefinition(e);
    }

    boolean isIdentical = false;
    boolean needsMediaUpdate = false;
    File revised = new File(briefcaseFormFile.getParentFile(), briefcaseFormFile.getName() + ".revised");
    String revisedXml = null;
    JavaRosaParserWrapper revisedDefn = null;
    // determine the most up-to-date existing definition...
    JavaRosaParserWrapper existingDefn;
    try {
      if (revised.exists()) {
        revisedXml = readFile(revised);
        revisedDefn = new JavaRosaParserWrapper(revised, revisedXml);
      }

      if (!briefcaseFormFile.exists()) {
        // the tmpFormFile is the first time we saw this form.
        // Rename it to formFile and parse it.
        if (copyFile) {
          try {
            FileUtils.copyFile(tmpFormFile, briefcaseFormFile);
          } catch (IOException e) {
            String msg = "Unable to copy form definition file into briefcase directory";
            log.error(msg, e);
            throw new BadFormDefinition(msg);
          }
        } else {
          if (!tmpFormFile.renameTo(briefcaseFormFile)) {
            // if cannot rename, try to copy instead (and mark for deletion)
            try {
                FileUtils.copyFile(tmpFormFile,briefcaseFormFile);
                tmpFormFile.deleteOnExit();
            } catch (IOException e) {
              String msg = "Form directory does not contain form (can neither rename nor copy into briefcase directory)";
              log.error(msg);
              throw new BadFormDefinition(msg);
            }
          }
        }
        needsMediaUpdate = !revised.exists(); // weird if it does...
        existingDefn = new JavaRosaParserWrapper(briefcaseFormFile, readFile(briefcaseFormFile));
      } else {
        // get the current existing definition...
        String existingXml = readFile(briefcaseFormFile);
        existingDefn = new JavaRosaParserWrapper(briefcaseFormFile, existingXml);
        String existingTitle = existingDefn.getFormName();

        // compare the two
        DifferenceResult result;
        if (badForm) {
          // newDefn is considered identical to what we have locally...
          result = DifferenceResult.XFORMS_IDENTICAL;
        } else {
          result = JavaRosaParserWrapper.compareXml(newDefn, existingXml, existingTitle, true);
        }

        if (result == DifferenceResult.XFORMS_DIFFERENT) {
          if (revised.exists()) {
            result = JavaRosaParserWrapper.compareXml(newDefn, revisedXml,
                revisedDefn.getFormName(), true);
            if (result == DifferenceResult.XFORMS_DIFFERENT) {
              throw new BadFormDefinition("Form definitions are incompatible.");
            } else if (result != DifferenceResult.XFORMS_EARLIER_VERSION
                && result != DifferenceResult.XFORMS_MISSING_VERSION
                && result != DifferenceResult.XFORMS_IDENTICAL) {
              if (copyFile) {
                try {
                  FileUtils.copyFile(tmpFormFile, revised);
                } catch (IOException e) {
                  String msg = "Unable to overwrite the '.revised' form definition file in briefcase storage";
                  log.error(msg, e);
                  throw new BadFormDefinition(msg);
                }
              } else {
                if (!tmpFormFile.renameTo(revised)) {
                  // if cannot rename, try to copy instead (and mark for deletion)
                  try {
                      FileUtils.copyFile(tmpFormFile,revised);
                      tmpFormFile.deleteOnExit();
                  } catch (IOException e) {
                    String msg = "Form directory does not contain form (can neither rename nor copy into briefcase directory)";
                    log.error(msg, e);
                    throw new BadFormDefinition(msg);
                  }
                }
              }
              needsMediaUpdate = true;
              // and re-parse the new revised file (since we just updated it...)
              revisedDefn = new JavaRosaParserWrapper(revised, readFile(revised));
            } else if (result == DifferenceResult.XFORMS_IDENTICAL) {
              // confirm that the media is up-to-date when the forms are
              // identical
              // allows briefcase to resume a form download when it failed
              // during
              // the early form-media-fetch phases.
              isIdentical = true;
              needsMediaUpdate = true;
            }
          } else {
            throw new BadFormDefinition("Form definitions are incompatible.");
          }
        } else if (result != DifferenceResult.XFORMS_EARLIER_VERSION
            && result != DifferenceResult.XFORMS_MISSING_VERSION
            && result != DifferenceResult.XFORMS_IDENTICAL) {
          if (!revised.exists()) {
            // not using the revised form definition
            // the tmp form definition is newer
            // overwrite everything and re-parse the new file.
            if (copyFile) {
              try {
                FileUtils.copyFile(tmpFormFile, briefcaseFormFile);
              } catch (IOException e) {
                String msg = "Unable to overwrite form definition file in briefcase storage";
                log.error(msg, e);
                throw new BadFormDefinition(msg);
              }
            } else {
              if (!tmpFormFile.renameTo(briefcaseFormFile)) {
                // if cannot rename, try to copy instead (and mark for deletion)
                try {
                    FileUtils.copyFile(tmpFormFile,briefcaseFormFile);
                    tmpFormFile.deleteOnExit();
                } catch (IOException e) {
                  String msg = "Form directory does not contain form (can neither rename nor copy into briefcase directory)";
                  log.error(msg, e);
                  throw new BadFormDefinition(msg);
                }
              }
            }
            needsMediaUpdate = true;
            // and re-parse the new form file (since we just updated it...)
            existingXml = readFile(briefcaseFormFile);
            existingDefn = new JavaRosaParserWrapper(briefcaseFormFile, existingXml);
          }
        } else if (result == DifferenceResult.XFORMS_IDENTICAL) {
          // if a revised form exists, we assume the media is up-to-date in that
          // folder. Otherwise, confirm that the media is up-to-date when the
          // forms are identical. This allows briefcase to resume a form
          // download
          // when it failed during the early form-media-fetch phases.
          isIdentical = true;
          needsMediaUpdate = !revised.exists();
        }
      }
    } catch (ODKIncompleteSubmissionData e) {
      throw new BadFormDefinition(e, e.getReason());
    }

    BriefcaseFormDefinition defn;
    if (revised.exists()) {
      defn = new BriefcaseFormDefinition(briefcaseFormDirectory, revisedDefn, revised,
          needsMediaUpdate);
    } else {
      defn = new BriefcaseFormDefinition(briefcaseFormDirectory, existingDefn, null,
          needsMediaUpdate);
    }

    if (!isIdentical && needsMediaUpdate) {
      EventBus.publish(new UpdatedBriefcaseFormDefinitionEvent(defn));
    }
    return defn;
  }

  private BriefcaseFormDefinition(File briefcaseFormDirectory, JavaRosaParserWrapper formDefn,
      File revisedFormFile, boolean needsMediaUpdate) {
    this.needsMediaUpdate = needsMediaUpdate;
    this.formDefn = formDefn;
    this.revisedFormFile = revisedFormFile;
    this.formFolder = briefcaseFormDirectory;
  }

  public BriefcaseFormDefinition(File briefcaseFormDirectory, File formFile)
      throws BadFormDefinition {
    formFolder = briefcaseFormDirectory;
    needsMediaUpdate = false;
    if (!formFile.exists()) {
      throw new BadFormDefinition("Form directory does not contain form");
    }
    File revised = new File(formFile.getParentFile(), formFile.getName() + ".revised");
    try {
      if (revised.exists()) {
        revisedFormFile = revised;
        formDefn = new JavaRosaParserWrapper(revisedFormFile, readFile(revisedFormFile));
      } else {
        revisedFormFile = null;
        formDefn = new JavaRosaParserWrapper(formFile, readFile(formFile));
      }
    } catch (ODKIncompleteSubmissionData e) {
      throw new BadFormDefinition(e, e.getReason());
    }
  }

  @Override
  public String toString() {
    return getFormName();
  }

  public File getFormDirectory() {
    return formFolder;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opendatakit.briefcase.model.IFormDefinition#getFormName()
   */
  @Override
  public String getFormName() {
    return formDefn.getFormName();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opendatakit.briefcase.model.IFormDefinition#getFormId()
   */
  @Override
  public String getFormId() {
    return formDefn.getSubmissionElementDefn().formId;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opendatakit.briefcase.model.IFormDefinition#getVersionString()
   */
  @Override
  public String getVersionString() {
    return formDefn.getSubmissionElementDefn().versionString;
  }

  public File getFormDefinitionFile() {
    if (revisedFormFile != null) {
      return revisedFormFile;
    } else {
      return formDefn.getFormDefinitionFile();
    }
  }

  public boolean isInvalidFormXmlns() {
    return formDefn.isInvalidFormXmlns();
  }

  public String getSubmissionKey(String uri) {
    return formDefn.getSubmissionKey(uri);
  }

  public boolean isFieldEncryptedForm() {
    return formDefn.isFieldEncryptedForm();
  }

  public boolean isFileEncryptedForm() {
    return formDefn.isFileEncryptedForm();
  }

  public TreeElement getSubmissionElement() {
    TreeElement treeElement = formDefn.getSubmissionElement();
    if (treeElement == null && formDefn.getFormDefinitionFile() != null) {
      File formFile = formDefn.getFormDefinitionFile();
      try {
          formDefn = new JavaRosaParserWrapper(formFile, readFile(formFile));
      } catch (ODKIncompleteSubmissionData | BadFormDefinition e) {
        e.printStackTrace();
      }
    }
    return formDefn.getSubmissionElement();
  }

  public void setPrivateKey(PrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  @Override
  public LocationType getFormLocation() {
    return LocationType.LOCAL;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof BriefcaseFormDefinition) {
      BriefcaseFormDefinition lf = (BriefcaseFormDefinition) obj;

      String id = getFormId();
      String versionString = getVersionString();

      return (id.equals(lf.getFormId()) && ((versionString == null) ? (lf.getVersionString() == null)
          : versionString.equals(lf.getVersionString())));
    }

    return false;
  }

  @Override
  public int hashCode() {
    String id = getFormId();
    String versionString = getVersionString();

    return id.hashCode() + 3 * (versionString == null ? -123121 : versionString.hashCode());
  }
}
