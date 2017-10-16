package org.opendatakit.briefcase.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.javarosa.core.model.instance.TreeElement;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.form.XFormParameters;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.common.web.constants.HtmlConsts;

public class JavaRosaParserWrapper extends BaseFormParserForJavaRosa {
  private final File formDefinitionFile;
  private final String md5Hash;

  public JavaRosaParserWrapper(File formDefinitionFile, String inputXml) throws ODKIncompleteSubmissionData {
    super(inputXml, null, true);
    this.formDefinitionFile = formDefinitionFile;
    try {
      md5Hash = newMD5HashUri(inputXml.getBytes(HtmlConsts.UTF8_ENCODE));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("should not happen");
    }
  }

  public final static String newMD5HashUri(byte[] asBytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(asBytes);

      byte[] messageDigest = md.digest();

      BigInteger number = new BigInteger(1, messageDigest);
      String md5 = number.toString(16);
      while (md5.length() < 32) {
        md5 = "0" + md5;
      }
      return "md5:" + md5;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unexpected problem computing md5 hash", e);
    }
  }

  public XFormParameters getRootElementDefn() {
    return rootElementDefn;
  }

  public XFormParameters getSubmissionElementDefn() {
    return submissionElementDefn;
  }

  public String getSubmissionKey(String uri) {
    return submissionElementDefn.formId + "[@version=" + submissionElementDefn.modelVersion + " and @uiVersion=null"
        + "]/" + (isFileEncryptedForm() ? "data" : getSubmissionElement().getName()) + "[@key=" + uri + "]";
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

  public String getBase64RsaPublicKey() {
    return base64RsaPublicKey;
  }

  public String getBase64EncryptedFieldRsaPublicKey() {
    return base64EncryptedFieldRsaPublicKey;
  }

  public boolean isNotUploadableForm() {
    return isNotUploadableForm;
  }

  public boolean isInvalidFormXmlns() {
    return isInvalidFormXmlns;
  }

  public String getFormName() {
    return title;
  }

  public File getFormDefinitionFile() {
    return formDefinitionFile;
  }

  public String getMD5Hash() {
    return md5Hash;
  }

}
