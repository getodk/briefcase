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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.xform.parse.XFormParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.ui.MessageStrings;
import org.opendatakit.briefcase.util.XmlManipulationUtils.FormInstanceMetadata;

public class FileSystemUtils {
  static final Log logger = LogFactory.getLog(FileSystemUtils.class);

  public static final String BRIEFCASE_DIR = "ODK Briefcase Storage";
  static final String README_TXT = "readme.txt";
  static final String FORMS_DIR = "forms";
  static final String HSQLDB_DIR = "info.hsqldb";
  static final String HSQLDB_DB = "info";
  static final String SMALLSQL_DIR = "info.db";
  static final String HSQLDB_JDBC_PREFIX = "jdbc:hsqldb:file:";
  static final String SMALLSQL_JDBC_PREFIX = "jdbc:smallsql:";

  // encryption support....
  static final String ASYMMETRIC_ALGORITHM = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
  static final String UTF_8 = "UTF-8";
  static final String ENCRYPTED_FILE_EXTENSION = ".enc";
  static final String MISSING_FILE_EXTENSION = ".missing";

  public static final String getMountPoint() {
    return System.getProperty("os.name").startsWith("Win") ? File.separator + ".." : (System
        .getProperty("os.name").startsWith("Mac") ? "/Volumes/" : "/mnt/");
  }

  public static final boolean isBriefcaseStorageLocationParentFolder(File pathname) {
    if ( !pathname.exists() ) {
      return false;
    }
    File folder = new File(pathname, BRIEFCASE_DIR);
    if ( !folder.exists() ) {
      return false;
    }
    if ( !folder.isDirectory() ) {
      return false;
    }
    File forms = new File(folder, FORMS_DIR);
    if ( !forms.exists() ) {
      return false;
    }
    if ( !forms.isDirectory() ) {
      return false;
    }
    return true;
  }

  public static final void assertBriefcaseStorageLocationParentFolder(File pathname) throws FileSystemException {
    File folder = new File(pathname, BRIEFCASE_DIR);
    if ( !folder.exists() ) {
      if ( !folder.mkdir() ) {
        throw new FileSystemException("Unable to create " + BRIEFCASE_DIR);
      }
    }
    File forms = new File(folder, FORMS_DIR);
    if ( !forms.exists() ) {
      if ( !forms.mkdir() ) {
        throw new FileSystemException("Unable to create " + FORMS_DIR);
      }
    }

    File f = new File(folder, README_TXT);
    if ( !f.exists() ) {
      try {
        if ( !f.createNewFile() ) {
          throw new FileSystemException("Unable to create " + README_TXT);
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new FileSystemException("Unable to create " + README_TXT);
      }
    }
    try {
      OutputStreamWriter fout = new OutputStreamWriter(new FileOutputStream(f,false), "UTF-8");
      fout.write(MessageStrings.README_CONTENTS);
      fout.close();
    } catch (IOException e) {
      e.printStackTrace();
      throw new FileSystemException("Unable to write " + README_TXT);
    }
  }

  public static final boolean isUnderBriefcaseFolder(File pathname) {
    File parent = (pathname == null ? null : pathname.getParentFile());
    File current = pathname;
    while (parent != null) {
      if (isBriefcaseStorageLocationParentFolder(parent) &&
          current.getName().equals(BRIEFCASE_DIR)) {
        return true;
      }
      current = parent;
      parent = parent.getParentFile();
    }
    return false;
  }

  // Predicates to determine whether the folder is an ODK Device
  // ODK folder or underneath that folder.

  private static final boolean isODKDevice(File pathname) {
    File fo = new File(pathname, "odk");
    File foi = new File(fo, "instances");
    File fof = new File(fo, "forms");
    return fo.exists() && foi.exists() && fof.exists();
  }

  public static final boolean isUnderODKFolder(File pathname) {
    File parent = (pathname == null ? null : pathname.getParentFile());
    File current = pathname;
    while (parent != null) {
      if (isODKDevice(parent) && current.getName().equals("odk"))
        return true;
      current = parent;
      parent = parent.getParentFile();
    }
    return false;
  }

  public static final boolean isValidODKFolder(File pathname) {
    return pathname != null && isODKDevice(pathname);
  }

  public static final boolean isODKInstancesParentFolder(File pathname) {
    File foi = new File(pathname, "instances");
    return foi.exists() && foi.isDirectory();
  }


  public static final List<BriefcaseFormDefinition> getBriefcaseFormList() {
    List<BriefcaseFormDefinition> formsList = new ArrayList<BriefcaseFormDefinition>();
    File forms = FileSystemUtils.getFormsFolder();
    if (forms.exists()) {
      File[] formDirs = forms.listFiles();
      for (File f : formDirs) {
        if (f.isDirectory()) {
          try {
            File formFile = new File(f, f.getName() + ".xml");
            formsList.add(new BriefcaseFormDefinition(f, formFile));
          } catch (BadFormDefinition e) {
            // TODO report bad form definition?
            e.printStackTrace();
          }
        } else {
          // junk?
          f.delete();
        }
      }
    }
    return formsList;
  }

  public static final List<OdkCollectFormDefinition> getODKFormList(File odk) {
    List<OdkCollectFormDefinition> formsList = new ArrayList<OdkCollectFormDefinition>();
    File forms = new File(odk, "forms");
    if (forms.exists()) {
      File[] formDirs = forms.listFiles();
      for (File f : formDirs) {
        if (f.isFile() && f.getName().endsWith(".xml")) {
          try {
            formsList.add(new OdkCollectFormDefinition(f));
          } catch (BadFormDefinition e) {
            // TODO report bad form definition?
            e.printStackTrace();
          }
        }
      }
    }
    return formsList;
  }

  public static File getBriefcaseFolder() {
    return new File(new File(BriefcasePreferences
        .getBriefcaseDirectoryProperty()), BRIEFCASE_DIR);
  }

  public static File getFormsFolder() {
    return new File(getBriefcaseFolder(), FORMS_DIR);
  }

  public static String asFilesystemSafeName(String formName) {
    return formName.replaceAll("[/\\\\:]", "").trim();
  }

  public static File getFormDirectory(String formName)
      throws FileSystemException {
    // clean up friendly form name...
    String rootName = asFilesystemSafeName(formName);
    File formPath = new File(getFormsFolder(), rootName);
    if (!formPath.exists() && !formPath.mkdirs()) {
      throw new FileSystemException("unable to create directory: " + formPath.getAbsolutePath());
    }
    return formPath;
  }

  static String getFormDatabaseUrl(File formDirectory) throws FileSystemException {

    File dbDir = new File(formDirectory, HSQLDB_DIR), dbFile = new File(dbDir, HSQLDB_DB);

    if (!dbDir.exists() && !dbDir.mkdirs()) {
      logger.warn("failed to create database directory: " + dbDir);
    }

    return getJdbcUrl(dbFile);
  }

  private static String getJdbcUrl(File dbFile) throws FileSystemException {
    if (isHypersonicDatabase(dbFile)) {
      return HSQLDB_JDBC_PREFIX + dbFile.getAbsolutePath();
    } else if (isSmallSQLDatabase(dbFile)){
      return SMALLSQL_JDBC_PREFIX + dbFile.getAbsolutePath() + (dbFile.exists()? "" : "?create=true");
    } else {
      throw new FileSystemException("unknown database type for file " + dbFile);
    }
  }

  private static boolean isSmallSQLDatabase(File dbFile) {
    return SMALLSQL_DIR.equals(dbFile.getName());
  }

  private static boolean isHypersonicDatabase(File dbFile) {
    File parentFile = dbFile.getParentFile();
    return HSQLDB_DB.equals(dbFile.getName()) && parentFile != null && HSQLDB_DIR.equals(parentFile.getName());
  }

  public static File getFormDefinitionFileIfExists(File formDirectory) {
    if (!formDirectory.exists()) {
      return null;
    }
    File formDefnFile = new File(formDirectory, formDirectory.getName() + ".xml");
    if (!formDefnFile.exists()) {
      return null;
    }
    return formDefnFile;
  }

  public static File getTempFormDefinitionFile()
      throws FileSystemException {
    File briefcase = getBriefcaseFolder();
    File tempDefnFile;
    try {
      tempDefnFile = File.createTempFile("tempDefn", ".xml", briefcase);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return tempDefnFile;
  }

  public static File getFormDefinitionFile(File formDirectory)
      throws FileSystemException {
    File formDefnFile = new File(formDirectory, formDirectory.getName() + ".xml");

    return formDefnFile;
  }

  public static File getMediaDirectoryIfExists(File formDirectory) {
    if (!formDirectory.exists()) {
      return null;
    }
    File mediaDir = new File(formDirectory, formDirectory.getName() + "-media");
    if (!mediaDir.exists()) {
      return null;
    }
    return mediaDir;
  }

  public static File getMediaDirectory(File formDirectory)
      throws FileSystemException {
    File mediaDir = new File(formDirectory, formDirectory.getName() + "-media");
    if (!mediaDir.exists() && !mediaDir.mkdirs()) {
      throw new FileSystemException("unable to create directory: " + mediaDir.getAbsolutePath());
    }

    return mediaDir;
  }

  public static File getFormInstancesDirectory(File formDirectory) throws FileSystemException {
    File instancesDir = new File(formDirectory, "instances");
    if (!instancesDir.exists() && !instancesDir.mkdirs()) {
      throw new FileSystemException("unable to create directory: " + instancesDir.getAbsolutePath());
    }
    return instancesDir;
  }

  public static Set<File> getFormSubmissionDirectories(File formDirectory) {
    Set<File> files = new TreeSet<File>();

    File formInstancesDir = null;
    try {
      formInstancesDir = getFormInstancesDirectory(formDirectory);
    } catch (FileSystemException e) {
      e.printStackTrace();
      return files;
    }

    File[] briefcaseInstances = formInstancesDir.listFiles();
    if (briefcaseInstances != null) {
      for (File briefcaseInstance : briefcaseInstances) {
        if (!briefcaseInstance.isDirectory() || briefcaseInstance.getName().startsWith(".")) {
          logger.warn("skipping non-directory or dot-file in form instances subdirectory");
          continue;
        }
        files.add(briefcaseInstance);
      }
    }

    return files;
  }

  public static boolean hasFormSubmissionDirectory(File formInstancesDir, String instanceID) {
    // create instance directory...
    String instanceDirName = asFilesystemSafeName(instanceID);
    File instanceDir = new File(formInstancesDir, instanceDirName);
    return instanceDir.exists();
  }

  public static File getFormSubmissionDirectory(File formInstancesDir, String instanceID) {
    // construct the instance directory File...
    String instanceDirName = asFilesystemSafeName(instanceID);
    File instanceDir = new File(formInstancesDir, instanceDirName);
    if (!instanceDir.exists() || instanceDir.isDirectory()) {
      return instanceDir;
    }
    return null;
  }

  public static File assertFormSubmissionDirectory(File formInstancesDir, String instanceID)
      throws FileSystemException {
    // create instance directory...
    String instanceDirName = asFilesystemSafeName(instanceID);
    File instanceDir = new File(formInstancesDir, instanceDirName);
    if (instanceDir.exists() && instanceDir.isDirectory()) {
      return instanceDir;
    }

    if (!instanceDir.mkdir()) {
      throw new FileSystemException("unable to create instance dir");
    }

    return instanceDir;
  }

  public static final String getMd5Hash(File file) {
    try {
      // CTS (6/15/2010) : stream file through digest instead of handing
      // it the
      // byte[]
      MessageDigest md = MessageDigest.getInstance("MD5");
      int chunkSize = 256;

      byte[] chunk = new byte[chunkSize];

      // Get the size of the file
      long lLength = file.length();

      if (lLength > Integer.MAX_VALUE) {
        logger.error("File " + file.getName() + "is too large");
        return null;
      }

      int length = (int) lLength;

      InputStream is = null;
      is = new FileInputStream(file);

      int l = 0;
      for (l = 0; l + chunkSize < length; l += chunkSize) {
        is.read(chunk, 0, chunkSize);
        md.update(chunk, 0, chunkSize);
      }

      int remaining = length - l;
      if (remaining > 0) {
        is.read(chunk, 0, remaining);
        md.update(chunk, 0, remaining);
      }
      byte[] messageDigest = md.digest();

      BigInteger number = new BigInteger(1, messageDigest);
      String md5 = number.toString(16);
      while (md5.length() < 32)
        md5 = "0" + md5;
      is.close();
      return md5;

    } catch (NoSuchAlgorithmException e) {
      logger.error("MD5 calculation failed: " + e.getMessage());
      return null;

    } catch (FileNotFoundException e) {
      logger.error("No File: " + e.getMessage());
      return null;
    } catch (IOException e) {
      logger.error("Problem reading from file: " + e.getMessage());
      return null;
    }

  }

  private static final void decryptFile(EncryptionInformation ei,
      File original, File unencryptedDir) throws IOException, NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    InputStream fin = null;
    OutputStream fout = null;

    try {
      if ( original == null ) {
        // special case -- user marked-as-complete an encrypted file on a pre-1.4.5 ODK Aggregate
        // need to get a Cipher to update the cipher initialization vector. 
        ei.getCipher("missing.enc");
        logger.info("Missing file (pre-ODK Aggregate 1.4.5 mark-as-complete on server)");
        return;
      }
      
      String name = original.getName();
      if (!name.endsWith(ENCRYPTED_FILE_EXTENSION)) {
        String errMsg = "Unexpected non-" + ENCRYPTED_FILE_EXTENSION + " extension " + name
            + " -- ignoring file";
        throw new IllegalArgumentException(errMsg);
      }
      name = name.substring(0, name.length() - ENCRYPTED_FILE_EXTENSION.length());
      File decryptedFile = new File(unencryptedDir, name);

      Cipher c = ei.getCipher(name);

      // name is now the decrypted file name
      // if it ends in ".missing" then the file
      // was not available and the administrator
      // marked it as complete on the SubmissionAdmin
      // page.
      if ( name.endsWith(MISSING_FILE_EXTENSION) ) {
        logger.info("Missing file (ODK Aggregate 1.4.5 and higher):" + original.getName());
        return;
      }
      
      fin = new FileInputStream(original);
      fin = new CipherInputStream(fin, c);

      fout = new FileOutputStream(decryptedFile);
      byte[] buffer = new byte[2048];
      int len = fin.read(buffer);
      while (len != -1) {
        fout.write(buffer, 0, len);
        len = fin.read(buffer);
      }
      fout.flush();
      logger.info("Decrpyted:" + original.getName() + " -> " + decryptedFile.getName());
    } finally {
      if (fin != null) {
        try {
          fin.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fout != null) {
        try {
          fout.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static boolean decryptSubmissionFiles(String base64EncryptedSymmetricKey,
      FormInstanceMetadata fim, List<String> mediaNames,
      String encryptedSubmissionFile, String base64EncryptedElementSignature,
      PrivateKey rsaPrivateKey, File instanceDir, File unencryptedDir) throws FileSystemException,
      CryptoException, ParsingException {

    EncryptionInformation ei = new EncryptionInformation(base64EncryptedSymmetricKey, fim.instanceId, rsaPrivateKey);

    byte[] elementDigest;
    try {
      // construct the base64-encoded RSA-encrypted symmetric key
      Cipher pkCipher;
      pkCipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM);
      // extract digest
      pkCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
      byte[] encryptedElementSignature = Base64.decodeBase64(base64EncryptedElementSignature);
      elementDigest = pkCipher.doFinal(encryptedElementSignature);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedElementSignature Cause: "
          + e.toString());
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedElementSignature Cause: "
          + e.toString());
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedElementSignature Cause: "
          + e.toString());
    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedElementSignature Cause: "
          + e.toString());
    } catch (BadPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedElementSignature Cause: "
          + e.toString());
    }

    // NOTE: will decrypt only the files in the media list, plus the encryptedSubmissionFile

    File[] allFiles = instanceDir.listFiles();
    List<File> filesToProcess = new ArrayList<File>();
    for (File f : allFiles) {
      if ( mediaNames.contains(f.getName()) ) {
        filesToProcess.add(f);
      } else if ( encryptedSubmissionFile.equals(f.getName()) ) {
        filesToProcess.add(f);
      }
    }

    // should have all media files plus one submission.xml.enc file
    if ( filesToProcess.size() != mediaNames.size() + 1 ) {
      // figure out what we're missing...
      int lostFileCount = 0;
      List<String> missing = new ArrayList<String>();
      for ( String name : mediaNames ) {
        if ( name == null ) {
          // this was lost due to an pre-ODK Aggregate 1.4.5 mark-as-complete action
          ++lostFileCount;
          continue;
        }
        File f = new File(instanceDir, name);
        if ( !filesToProcess.contains(f)) {
          missing.add(name);
        }
      }
      StringBuilder b = new StringBuilder();
      for ( String name : missing ) {
        b.append(" ").append(name);
      }
      if ( !filesToProcess.contains(new File(instanceDir, encryptedSubmissionFile)) ) {
        b.append(" ").append(encryptedSubmissionFile);
        throw new FileSystemException("Error decrypting: " + instanceDir.getName() + " Missing files:" + b.toString());
      } else {
        // ignore the fact that we don't have the lost files
        if ( filesToProcess.size() + lostFileCount != mediaNames.size() + 1 ) {
          throw new FileSystemException("Error decrypting: " + instanceDir.getName() + " Missing files:" + b.toString());
        }
      }
    }

    // decrypt the media files IN ORDER.
    for (String mediaName : mediaNames) {
      String displayedName = (mediaName == null) ? "<missing .enc file>" : mediaName;
      File f = (mediaName == null) ? null : new File(instanceDir, mediaName);
      try {
        decryptFile(ei, f, unencryptedDir);
      } catch (InvalidKeyException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrypting:" + displayedName + " Cause: " + e.toString());
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrypting:" + displayedName + " Cause: " + e.toString());
      } catch (InvalidAlgorithmParameterException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrypting:" + displayedName + " Cause: " + e.toString());
      } catch (NoSuchPaddingException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrypting:" + displayedName + " Cause: " + e.toString());
      } catch (IOException e) {
        e.printStackTrace();
        throw new FileSystemException("Error decrypting:" + displayedName + " Cause: " + e.toString());
      }
    }

    // decrypt the submission file
    File f = new File(instanceDir, encryptedSubmissionFile);
    try {
      decryptFile(ei, f, unencryptedDir);
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting:" + f.getName() + " Cause: " + e.toString());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting:" + f.getName() + " Cause: " + e.toString());
    } catch (InvalidAlgorithmParameterException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting:" + f.getName() + " Cause: " + e.toString());
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting:" + f.getName() + " Cause: " + e.toString());
    } catch (IOException e) {
      e.printStackTrace();
      throw new FileSystemException("Error decrypting:" + f.getName() + " Cause: " + e.toString());
    }

    // get the FIM for the decrypted submission file
    File submissionFile = new File( unencryptedDir,
        encryptedSubmissionFile.substring(0, encryptedSubmissionFile.lastIndexOf(".enc")));

    FormInstanceMetadata submissionFim;
    try {
      Document subDoc = XmlManipulationUtils.parseXml(submissionFile);
      submissionFim = XmlManipulationUtils.getFormInstanceMetadata(subDoc.getRootElement());
    } catch (ParsingException e) {
      e.printStackTrace();
      throw new FileSystemException("Error decrypting: " + submissionFile.getName() + " Cause: " + e.toString());
    } catch (FileSystemException e) {
      e.printStackTrace();
      throw new FileSystemException("Error decrypting: " + submissionFile.getName() + " Cause: " + e.getMessage());
    }

    boolean same = submissionFim.xparam.formId.equals(fim.xparam.formId);

    if ( !same ) {
      throw new FileSystemException("Error decrypting:" + unencryptedDir.getName()
          + " Cause: form instance metadata differs from that in manifest");
    }

    // Construct the element signature string
    StringBuilder b = new StringBuilder();
    appendElementSignatureSource(b, fim.xparam.formId);
    if ( fim.xparam.modelVersion != null ) {
      appendElementSignatureSource(b, Long.toString(fim.xparam.modelVersion));
    }
    appendElementSignatureSource(b, base64EncryptedSymmetricKey);

    appendElementSignatureSource(b, fim.instanceId);

    boolean missingFile = false;
    for ( String encFilename : mediaNames ) {
      if ( encFilename == null ) {
        missingFile = true;
        continue;
      }
      File decryptedFile = new File( unencryptedDir,
          encFilename.substring(0, encFilename.lastIndexOf(".enc")));
      if ( decryptedFile.getName().endsWith(".missing")) {
        // this is a missing file -- we will not be able to 
        // confirm the signature of the submission.
        missingFile = true;
        continue;
      }
      String md5 = FileSystemUtils.getMd5Hash(decryptedFile);
      appendElementSignatureSource(b, decryptedFile.getName() + "::" + md5 );
    }

    String md5 = FileSystemUtils.getMd5Hash(submissionFile);
    appendElementSignatureSource(b, submissionFile.getName() + "::" + md5);

    // compute the digest of the element signature string
    byte[] messageDigest;
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(b.toString().getBytes("UTF-8"));
        messageDigest = md.digest();
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        throw new CryptoException("Error computing xml signature Cause: " + e.toString());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new CryptoException("Error computing xml signature Cause: " + e.toString());
    }

    same = true;
    for ( int i = 0 ; i < messageDigest.length ; ++i ) {
      if ( messageDigest[i] != elementDigest[i] ) {
        same = false;
        break;
      }
    }

    return same;
  }

  private static void appendElementSignatureSource(StringBuilder b, String value) {
    b.append(value).append("\n");
  }

  public static class DecryptOutcome {
    public final Document submission;
    public final boolean isValidated;

    DecryptOutcome(Document submission, boolean isValidated) {
      this.submission = submission;
      this.isValidated = isValidated;
    }
  }

  public static DecryptOutcome decryptAndValidateSubmission(Document doc,
      PrivateKey rsaPrivateKey, File instanceDir, File unEncryptedDir)
          throws ParsingException, FileSystemException, CryptoException {

    Element rootElement = doc.getRootElement();

    String base64EncryptedSymmetricKey;
    String instanceIdMetadata = null;
    List<String> mediaNames = new ArrayList<String>();
    String encryptedSubmissionFile;
    String base64EncryptedElementSignature;

    {
      Element base64Key = null;
      Element base64Signature = null;
      Element encryptedXml = null;
      for (int i = 0; i < rootElement.getChildCount(); ++i) {
        if (rootElement.getType(i) == Node.ELEMENT) {
          Element child = rootElement.getElement(i);
          String name = child.getName();
          if (name.equals("base64EncryptedKey")) {
            base64Key = child;
          } else if (name.equals("base64EncryptedElementSignature")) {
            base64Signature = child;
          } else if (name.equals("encryptedXmlFile")) {
            encryptedXml = child;
          } else if (name.equals("media")) {
            Element media = child;
            for (int j = 0; j < media.getChildCount(); ++j) {
              if (media.getType(j) == Node.ELEMENT) {
                Element mediaChild = media.getElement(j);
                String mediaFileElementName = mediaChild.getName();
                if (mediaFileElementName.equals("file")) {
                  String mediaName = XFormParser.getXMLText(mediaChild, true);
                  if (mediaName == null || mediaName.length() == 0) {
                    mediaNames.add(null);
                  } else {
                    mediaNames.add(mediaName);
                  }
                }
              }
            }
          }
        }
      }

      // verify base64Key
      if (base64Key == null) {
        throw new ParsingException("Missing base64EncryptedKey element in encrypted form.");
      }
      base64EncryptedSymmetricKey = XFormParser.getXMLText(base64Key, true);

      // get instanceID out of OpenRosa meta block
      instanceIdMetadata = XmlManipulationUtils.getOpenRosaInstanceId(rootElement);
      if (instanceIdMetadata == null) {
        throw new ParsingException("Missing instanceID within meta block of encrypted form.");
      }

      // get submission filename
      if (encryptedXml == null) {
        throw new ParsingException("Missing encryptedXmlFile element in encrypted form.");
      }
      encryptedSubmissionFile = XFormParser.getXMLText(encryptedXml, true);
      if (base64Signature == null) {
        throw new ParsingException("Missing base64EncryptedElementSignature element in encrypted form.");
      }
      base64EncryptedElementSignature = XFormParser.getXMLText(base64Signature, true);
    }

    if (instanceIdMetadata == null || base64EncryptedSymmetricKey == null
        || base64EncryptedElementSignature == null || encryptedSubmissionFile == null) {
      throw new ParsingException("Missing one or more required elements of encrypted form.");
    }

    FormInstanceMetadata fim;
    try {
      fim = XmlManipulationUtils.getFormInstanceMetadata(rootElement);
    } catch (ParsingException e) {
      e.printStackTrace();
      throw new ParsingException(
          "Unable to extract form instance medatadata from submission manifest. Cause: " + e.toString());
    }

    if (!instanceIdMetadata.equals(fim.instanceId)) {
      throw new ParsingException("InstanceID within metadata does not match that on top level element.");
    }

    boolean isValidated = FileSystemUtils.decryptSubmissionFiles(base64EncryptedSymmetricKey, fim,
          mediaNames, encryptedSubmissionFile,
          base64EncryptedElementSignature, rsaPrivateKey, instanceDir, unEncryptedDir);

    // and change doc to be the decrypted submission document
    File decryptedSubmission = new File(unEncryptedDir, "submission.xml");
    doc = XmlManipulationUtils.parseXml(decryptedSubmission);
    if (doc == null) {
      return null;
    }

    // verify that the metadata matches between the manifest and the submission
    rootElement = doc.getRootElement();
    FormInstanceMetadata sim = XmlManipulationUtils.getFormInstanceMetadata(rootElement);
    if ( !fim.xparam.equals(sim.xparam) ) {
      throw new ParsingException(
          "FormId or version in decrypted submission does not match that in manifest!");
    }
    if ( !fim.instanceId.equals(sim.instanceId) ) {
      throw new ParsingException(
          "InstanceId in decrypted submission does not match that in manifest!");
    }

    return new DecryptOutcome(doc, isValidated);
  }
}
