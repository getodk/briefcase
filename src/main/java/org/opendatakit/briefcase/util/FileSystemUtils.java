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
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import org.opendatakit.briefcase.model.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemUtils {

  private static final Logger log = LoggerFactory.getLogger(FileSystemUtils.class);

  public static final String FORMS_DIR = "forms";
  static final String INSTANCE_DIR = "instances";
  private static final String HSQLDB_DIR = "info.hsqldb";
  private static final String HSQLDB_DB = "info";
  private static final String SMALLSQL_DIR = "info.db";
  static final String HSQLDB_JDBC_PREFIX = "jdbc:hsqldb:file:";

  static final String SMALLSQL_JDBC_PREFIX = "jdbc:smallsql:";

  // Predicates to determine whether the folder is an ODK Device
  // ODK folder or underneath that folder.

  private static boolean isODKDevice(File pathname) {
    File fo = new File(pathname, "odk");
    File foi = new File(fo, "instances");
    File fof = new File(fo, "forms");
    return fo.exists() && foi.exists() && fof.exists();
  }

  public static boolean isUnderODKFolder(File pathname) {
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

  static String getFormDatabaseUrl(File formDirectory) throws FileSystemException {

    File oldDbFile = new File(formDirectory, SMALLSQL_DIR);
    File dbDir = new File(formDirectory, HSQLDB_DIR);
    File dbFile = new File(dbDir, HSQLDB_DB);


    if (!dbDir.exists()) {
      log.info("Creating database directory {}", dbDir);
      if (!dbDir.mkdirs()) {
        log.warn("failed to create database directory");
      } else if (oldDbFile.exists()) {
        migrateDatabase(oldDbFile, dbFile);
      }
    }

    return getJdbcUrl(dbFile);
  }

  private static void migrateDatabase(File oldDbFile, File dbFile) throws FileSystemException {
    try {
      DatabaseUtils.migrateData(getJdbcUrl(oldDbFile), getJdbcUrl(dbFile));
    } catch (SQLException e) {
      throw new FileSystemException(String.format("failed to migrate database %s to %s", oldDbFile, dbFile), e);
    }
    if (!oldDbFile.renameTo(getBackupFile(oldDbFile))) {
      throw new FileSystemException("failed to backup database after migration");
    }
  }

  private static File getBackupFile(File file) {
    return new File(file.getParent(), file.getName() + ".bak");
  }

  private static String getJdbcUrl(File dbFile) throws FileSystemException {
    if (isHypersonicDatabase(dbFile)) {
      return HSQLDB_JDBC_PREFIX + dbFile.getAbsolutePath();
    } else if (isSmallSQLDatabase(dbFile)) {
      return SMALLSQL_JDBC_PREFIX + dbFile.getAbsolutePath() + (dbFile.exists() ? "" : "?create=true");
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

  static boolean isFormRelativeInstancePath(String path) {
    return path.startsWith(INSTANCE_DIR);
  }

  static Path makeRelative(File parent, File child) {
    Path parentPath = parent.toPath();
    Path childPath = child.toPath();
    return parentPath.relativize(childPath);
  }

  public static String getMd5Hash(File file) {
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
        log.error("File is too large");
        return null;
      }

      int length = (int) lLength;

      InputStream is;
      is = new FileInputStream(file);

      int l;
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
      log.error("MD5 calculation failed", e);
      return null;

    } catch (FileNotFoundException e) {
      log.error("No File", e);
      return null;
    } catch (IOException e) {
      log.error("Problem reading from file", e);
      return null;
    }

  }

}
