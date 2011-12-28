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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.xform.parse.XFormParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;
import org.opendatakit.briefcase.util.XmlManipulationUtils.FormInstanceMetadata;

public class FileSystemUtils {
  static final Log logger = LogFactory.getLog(FileSystemUtils.class);

  static final String FORMS_DIR = "forms";
  static final String SCRATCH_DIR = "scratch";

  // encryption support....
  static final String ASYMMETRIC_ALGORITHM = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
  static final String SYMMETRIC_ALGORITHM = "AES/CFB/PKCS5Padding";
  static final String UTF_8 = "UTF-8";
  static final int SYMMETRIC_KEY_LENGTH = 256;
  static final int IV_BYTE_LENGTH = 16;
  static final String ENCRYPTED_FILE_EXTENSION = ".enc";

  public static final String getMountPoint() {
    return System.getProperty("os.name").startsWith("Win") ? File.separator + ".." : (System
        .getProperty("os.name").startsWith("Mac") ? "/Volumes/" : "/mnt/");
  }

  public static final boolean isUnderBriefcaseFolder(File pathname) {
    File parent = (pathname == null ? null : pathname.getParentFile());
    while (parent != null) {
      if (isBriefcaseFolder(parent, false))
        return true;
      parent = parent.getParentFile();
    }
    return false;
  }

  private static final boolean isBriefcaseFolder(File pathname, boolean strict) {
    String[] contents = pathname.list();
    int len = (contents == null) ? 0 : contents.length;
    File foi = FileSystemUtils.getScratchFolder(pathname);
    File fof = FileSystemUtils.getFormsFolder(pathname);
    return pathname.exists()
        && ((len == 0) || ((len == 1) && (foi.exists() || fof.exists())) || (((len == 2) || (!strict && (len == 3)))
            && foi.exists() && fof.exists()));
  }

  public static final boolean isValidBriefcaseFolder(File pathname) {
    return pathname != null && isBriefcaseFolder(pathname, true);
  }

  public static final boolean isUnderODKFolder(File pathname) {
    File parent = (pathname == null ? null : pathname.getParentFile());
    while (parent != null) {
      if (isODKDevice(parent) && pathname.getName().equals("odk"))
        return true;
      parent = parent.getParentFile();
    }
    return false;
  }

  private static final boolean isODKDevice(File pathname) {
    File fo = new File(pathname, "odk");
    File foi = new File(fo, "instances");
    File fof = new File(fo, "forms");
    return fo.exists() && foi.exists() && fof.exists();
  }

  public static final boolean isValidODKFolder(File pathname) {
    return pathname != null && isODKDevice(pathname);
  }

  public static final List<LocalFormDefinition> getBriefcaseFormList(String briefcaseDirectory) {
    List<LocalFormDefinition> formsList = new ArrayList<LocalFormDefinition>();
    File briefcase = new File(briefcaseDirectory);
    File forms = FileSystemUtils.getFormsFolder(briefcase);
    if (forms.exists()) {
      File[] formDirs = forms.listFiles();
      for (File f : formDirs) {
        if (f.isDirectory()) {
          try {
            File formFile = new File(f, f.getName() + ".xml");
            formsList.add(new LocalFormDefinition(formFile));
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

  public static final List<LocalFormDefinition> getODKFormList(String odkDeviceDirectory) {
    List<LocalFormDefinition> formsList = new ArrayList<LocalFormDefinition>();
    File sdcard = new File(odkDeviceDirectory);
    File odk = new File(sdcard, "odk");
    File forms = new File(odk, "forms");
    if (forms.exists()) {
      File[] formDirs = forms.listFiles();
      for (File f : formDirs) {
        if (f.isFile() && f.getName().endsWith(".xml")) {
          try {
            formsList.add(new LocalFormDefinition(f));
          } catch (BadFormDefinition e) {
            // TODO report bad form definition?
            e.printStackTrace();
          }
        }
      }
    }
    return formsList;
  }

  public static File getFormsFolder(File briefcaseDir) {
    return new File(briefcaseDir, FORMS_DIR);
  }

  public static File getScratchFolder(File briefcaseDir) {
    return new File(briefcaseDir, SCRATCH_DIR);
  }

  public static void removeBriefcaseScratch(File scratchDir) throws IOException {
    if (scratchDir.exists()) {
      FileUtils.deleteDirectory(scratchDir);
    }
  }

  public static void establishBriefcaseScratch(File scratchDir) throws IOException {

    if (scratchDir.exists()) {
      return;
    }

    if (!scratchDir.getParentFile().exists()) {
      if (!scratchDir.getParentFile().mkdir()) {
        throw new IOException("Unable to create briefcase directory");
      }
    }

    if (!scratchDir.mkdir()) {
      throw new IOException("Unable to create scratch space");
    }
  }

  public static String asFilesystemSafeName(String formName) {
    return formName.replaceAll("[/\\\\:]", "").trim();
  }

  public static File getFormDirectory(File briefcaseFormsDir, String formName)
      throws FileSystemException {
    // clean up friendly form name...
    String rootName = asFilesystemSafeName(formName);
    File formPath = new File(briefcaseFormsDir, rootName);
    if (!formPath.exists() && !formPath.mkdirs()) {
      throw new FileSystemException("unable to create directory: " + formPath.getAbsolutePath());
    }
    return formPath;
  }

  public static File getFormDefinitionFileIfExists(File briefcaseFormsDir, String formName) {
    // clean up friendly form name...
    String rootName = asFilesystemSafeName(formName);
    File formPath = new File(briefcaseFormsDir, rootName);
    if (!formPath.exists()) {
      return null;
    }
    File formDefnFile = new File(formPath, rootName + ".xml");
    if (!formDefnFile.exists()) {
      return null;
    }
    return formDefnFile;
  }

  public static File getFormDefinitionFile(File briefcaseFormsDir, String formName)
      throws FileSystemException {
    String rootName = asFilesystemSafeName(formName);
    File formPath = getFormDirectory(briefcaseFormsDir, formName);
    File formDefnFile = new File(formPath, rootName + ".xml");

    return formDefnFile;
  }

  public static File getMediaDirectoryIfExists(File briefcaseFormsDir, String formName) {
    // clean up friendly form name...
    String rootName = asFilesystemSafeName(formName);
    File formPath = new File(briefcaseFormsDir, rootName);
    if (!formPath.exists()) {
      return null;
    }
    File mediaDir = new File(formPath, rootName + "-media");
    if (!mediaDir.exists()) {
      return null;
    }
    return mediaDir;
  }

  public static File getMediaDirectory(File briefcaseFormsDir, String formName)
      throws FileSystemException {
    String rootName = asFilesystemSafeName(formName);
    File formPath = getFormDirectory(briefcaseFormsDir, formName);
    File mediaDir = new File(formPath, rootName + "-media");
    if (!mediaDir.exists() && !mediaDir.mkdirs()) {
      throw new FileSystemException("unable to create directory: " + mediaDir.getAbsolutePath());
    }

    return mediaDir;
  }

  public static File getFormInstancesDirectory(File briefcaseFormsDir, String formName)
      throws FileSystemException {
    File formPath = getFormDirectory(briefcaseFormsDir, formName);
    File instancesDir = new File(formPath, "instances");
    if (!instancesDir.exists() && !instancesDir.mkdirs()) {
      throw new FileSystemException("unable to create directory: " + instancesDir.getAbsolutePath());
    }
    return instancesDir;
  }

  public static List<File> getFormSubmissionDirectories(File briefcaseFormsDir, String formName) {
    List<File> files = new ArrayList<File>();

    File formInstancesDir = null;
    try {
      formInstancesDir = getFormInstancesDirectory(briefcaseFormsDir, formName);
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
      logger.error("No Xml File: " + e.getMessage());
      return null;
    } catch (IOException e) {
      logger.error("Problem reading from file: " + e.getMessage());
      return null;
    }

  }

  private static final void decryptFile(CipherFactory cipherFactory,
      File original, File unencryptedDir) throws IOException, NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    InputStream fin = null;
    OutputStream fout = null;

    try {
      String name = original.getName();
      if (!name.endsWith(ENCRYPTED_FILE_EXTENSION)) {
        String errMsg = "Unexpected non-" + ENCRYPTED_FILE_EXTENSION + " extension " + name
            + " -- ignoring file";
        throw new IllegalArgumentException(errMsg);
      }
      name = name.substring(0, name.length() - ENCRYPTED_FILE_EXTENSION.length());
      File decryptedFile = new File(unencryptedDir, name);

      Cipher c = cipherFactory.getCipher();

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

  private static final class CipherFactory {
    private final SecretKeySpec symmetricKey;
    private final byte[] ivSeedArray;
    private int ivCounter = 0;

    CipherFactory(String instanceId, byte[] symmetricKeyBytes) throws CryptoException {
      
      symmetricKey = new SecretKeySpec( symmetricKeyBytes, SYMMETRIC_ALGORITHM);
      // construct the fixed portion of the iv -- the ivSeedArray
      // this is the md5 hash of the instanceID and the symmetric key
      try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(instanceId.getBytes("UTF-8"));
        md.update(symmetricKeyBytes);
        byte[] messageDigest = md.digest();
        ivSeedArray = new byte[IV_BYTE_LENGTH];
        for ( int i = 0 ; i < IV_BYTE_LENGTH ; ++i ) {
           ivSeedArray[i] = messageDigest[(i % messageDigest.length)];
        }
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        throw new CryptoException("Error constructing ivSeedArray Cause: " + e.toString());
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        throw new CryptoException("Error constructing ivSeedArray Cause: " + e.toString());
      }
    }
    
    public Cipher getCipher() throws InvalidKeyException,
          InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
       ++ivSeedArray[ivCounter % ivSeedArray.length];
       ++ivCounter;
       IvParameterSpec baseIv = new IvParameterSpec(ivSeedArray);
       Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM);
       c.init(Cipher.DECRYPT_MODE, symmetricKey, baseIv);
       return c;
    }
  }
  
  private static void decryptSubmissionFiles(String base64EncryptedSymmetricKey, 
      FormInstanceMetadata fim, List<String> mediaNames,
      String encryptedSubmissionFile, String base64EncryptedElementSignature,
      PrivateKey rsaPrivateKey, File instanceDir, File unencryptedDir) throws FileSystemException,
      CryptoException, ParsingException {

    CipherFactory cipherFactory;
    try {
      // construct the base64-encoded RSA-encrypted symmetric key
      Cipher pkCipher;
      pkCipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM);
      // write AES key
      pkCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
      byte[] encryptedSymmetricKey = Base64.decodeBase64(base64EncryptedSymmetricKey);
      byte[] decryptedKey = pkCipher.doFinal(encryptedSymmetricKey);
      cipherFactory = new CipherFactory(fim.instanceId, decryptedKey);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (BadPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    }

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
      List<String> missing = new ArrayList<String>();
      for ( String name : mediaNames ) {
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
      }
      throw new FileSystemException("Error decrypting: " + instanceDir.getName() + " Missing files:" + b.toString());
    }

    // decrypt the media files IN ORDER.
    for (String mediaName : mediaNames) {
      File f = new File(instanceDir, mediaName);
      try {
        decryptFile(cipherFactory, f, unencryptedDir);
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
    }

    // decrypt the submission file
    File f = new File(instanceDir, encryptedSubmissionFile);
    try {
      decryptFile(cipherFactory, f, unencryptedDir);
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
    
    boolean same = submissionFim.formId.equals(fim.formId) &&
        ((submissionFim.version == null) 
            ? (fim.version == null) : submissionFim.version.equals(fim.version)) &&
        ((submissionFim.uiVersion == null) 
            ? (fim.uiVersion == null) : submissionFim.uiVersion.equals(fim.uiVersion));
        
    if ( !same ) {
      throw new FileSystemException("Error decrypting:" + unencryptedDir.getName() 
          + " Cause: form instance metadata differs from that in manifest");
    }

    // Construct the element signature string
    StringBuilder b = new StringBuilder();
    appendElementSignatureSource(b, fim.formId);
    if ( fim.version != null ) {
      appendElementSignatureSource(b, fim.version.toString());
    }
    if ( fim.uiVersion != null ) {
      appendElementSignatureSource(b, fim.uiVersion.toString());
    }
    appendElementSignatureSource(b, base64EncryptedSymmetricKey);

    appendElementSignatureSource(b, fim.instanceId);
    
    for ( String encFilename : mediaNames ) {
      File decryptedFile = new File( unencryptedDir,
          encFilename.substring(0, encFilename.lastIndexOf(".enc")));
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
    if ( !same ) {
      throw new CryptoException("Xml signature does not match!");
    }
  }
  
  private static void appendElementSignatureSource(StringBuilder b, String value) {
    b.append(value).append("\n");
  }
  
  public static Document decryptAndValidateSubmission(Document doc, 
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
                    throw new ParsingException("Empty filename within media file element of encrypted form.");
                  }
                  mediaNames.add(mediaName);
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

    FileSystemUtils.decryptSubmissionFiles(base64EncryptedSymmetricKey, fim,
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
    if ( !fim.formId.equals(sim.formId) ||
         ((fim.version == null) 
             ? (sim.version != null) : !fim.version.equals(sim.version)) ||
         ((fim.uiVersion == null) 
             ? (sim.uiVersion != null) : !fim.uiVersion.equals(sim.uiVersion)) ) {
      throw new ParsingException(
          "FormId, version or uiVersion in decrypted submission does not match that in manifest!");
    }
    if ( !fim.instanceId.equals(sim.instanceId) ) {
      throw new ParsingException(
          "InstanceId in decrypted submission does not match that in manifest!");
    }
    
    return doc;
  }
}
