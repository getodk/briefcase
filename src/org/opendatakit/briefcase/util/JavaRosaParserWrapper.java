package org.opendatakit.briefcase.util;

import static org.opendatakit.briefcase.util.FileSystemUtils.getMediaDirectory;

import java.io.File;
import org.javarosa.core.model.instance.TreeElement;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.form.XFormParameters;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;

public class JavaRosaParserWrapper extends BaseFormParserForJavaRosa {
  private final File formDefinitionFile;

  public JavaRosaParserWrapper(File formDefinitionFile, String inputXml) throws ODKIncompleteSubmissionData {
    super(inputXml, null, getMediaDirectory(formDefinitionFile.getParentFile()), true);
    this.formDefinitionFile = formDefinitionFile;
  }

  public XFormParameters getSubmissionElementDefn() {
    return submissionElementDefn;
  }

  public TreeElement getSubmissionElement() {
    // for Briefcase, this is the original un-encrypted submission element
    return trueSubmissionElement;
  }

  public boolean isFieldEncryptedForm() {
    return isFieldEncryptedForm;
  }

  public boolean isFileEncryptedForm() {
    return isFileEncryptedForm;
  }

  public String getFormName() {
    return title;
  }

  public File getFormDefinitionFile() {
    return formDefinitionFile;
  }

}
