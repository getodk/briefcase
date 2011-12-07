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
import java.math.BigInteger;
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
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.CryptoException;
import org.opendatakit.briefcase.model.FileSystemException;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition;

public class FileSystemUtils {
  static final Log logger = LogFactory.getLog(FileSystemUtils.class);

  static final String FORMS_DIR = "forms";
  static final String SCRATCH_DIR = "scratch";

  // encryption support....
  static final String RSA_ALGORITHM = "RSA";
  static final String SYMMETRIC_ALGORITHM = "AES/CFB/NoPadding";
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

  private static final void decryptFile(String base64EncryptedSymmetricKey,
      PrivateKey rsaPrivateKey, File original, File unencryptedDir) 
          throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, 
          InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    InputStream fin = null;
    OutputStream fout = null;
    
    try {
    SecretKeySpec symmetricKey;
    // construct the base64-encoded RSA-encrypted symmetric key
      Cipher pkCipher;
      pkCipher = Cipher.getInstance(RSA_ALGORITHM);
      // write AES key
      pkCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
      byte[] encryptedSymmetricKey = Base64.decodeBase64(base64EncryptedSymmetricKey);
      byte[] pkDecryptedKey = pkCipher.doFinal(encryptedSymmetricKey);
      symmetricKey = new SecretKeySpec(pkDecryptedKey, SYMMETRIC_ALGORITHM);

    String name = original.getName();
    if (!name.endsWith(ENCRYPTED_FILE_EXTENSION)) {
      String errMsg = "Unexpected non-" + ENCRYPTED_FILE_EXTENSION + " extension " + name
          + " -- ignoring file";
      throw new IllegalArgumentException(errMsg);
    }
    name = name.substring(0, name.length() - ENCRYPTED_FILE_EXTENSION.length());
    File decryptedFile = new File(unencryptedDir, name);

    Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM);
      c.init(Cipher.DECRYPT_MODE, symmetricKey);

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
      if ( fin != null ) {
        try {
          fin.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if ( fout != null ) {
        try {
          fout.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void decryptSubmissionFiles(String base64EncryptedSymmetricKey,
      PrivateKey rsaPrivateKey, File instanceDir, File unencryptedDir) throws FileSystemException, CryptoException {
    // NOTE: assume the directory containing the instanceXml contains ONLY
    // files related to this one instance.

    // encrypt files that do not end with ".enc", and do not start with ".";
    // ignore directories
    File[] allFiles = instanceDir.listFiles();
    List<File> filesToProcess = new ArrayList<File>();
    for (File f : allFiles) {
      if (!f.getName().endsWith(ENCRYPTED_FILE_EXTENSION))
        continue; // not encrypted
      if (f.isDirectory())
        continue; // don't handle directories
      if (f.getName().startsWith("."))
        continue; // MacOSX garbage
      filesToProcess.add(f);
    }

    // decrypt here...
    for (File f : filesToProcess) {
      try {
        decryptFile(base64EncryptedSymmetricKey, rsaPrivateKey, f, unencryptedDir);
      } catch (InvalidKeyException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrpyting:" + f.getName() + " Cause: " + e.toString());
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrpyting:" + f.getName() + " Cause: " + e.toString());
      } catch (NoSuchPaddingException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrpyting:" + f.getName() + " Cause: " + e.toString());
      } catch (IllegalBlockSizeException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrpyting:" + f.getName() + " Cause: " + e.toString());
      } catch (BadPaddingException e) {
        e.printStackTrace();
        throw new CryptoException("Error decrpyting:" + f.getName() + " Cause: " + e.toString());
      } catch (IOException e) {
        e.printStackTrace();
        throw new FileSystemException("Error decrpyting:" + f.getName() + " Cause: " + e.toString());
      }
    }
  }

}
