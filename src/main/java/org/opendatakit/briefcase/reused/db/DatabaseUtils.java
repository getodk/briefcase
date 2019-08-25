/*
 * Copyright (C) 2012 University of Washington.
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

package org.opendatakit.briefcase.reused.db;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class abstracts all the functionality of the instance-tracking
 * database.
 *
 * @author mitchellsundt@gmail.com
 */
public class DatabaseUtils implements AutoCloseable {

  private static final String SMALLSQL_JDBC_PREFIX = "jdbc:smallsql:";
  private static final String HSQLDB_JDBC_PREFIX = "jdbc:hsqldb:file:";
  private static final Logger log = LoggerFactory.getLogger(DatabaseUtils.class);

  private static final String CREATE_DDL = "CREATE TABLE recorded_instance (instanceid VARCHAR(256) PRIMARY KEY, directory VARCHAR(4096))";
  private static final String ASSERT_SQL = "SELECT instanceid, directory FROM recorded_instance LIMIT 1";
  private static final String SELECT_ALL_SQL = "SELECT instanceid, directory FROM recorded_instance";
  private static final String SELECT_DIR_SQL = "SELECT directory FROM recorded_instance WHERE instanceid = ?";
  private static final String INSERT_DML = "INSERT INTO recorded_instance (instanceid, directory) VALUES(?,?)";
  private static final String INSTANCE_DIR = "instances";
  private static final String RELATIVE_DML = "UPDATE recorded_instance SET directory = regexp_replace(directory,'.*(" + INSTANCE_DIR + ")','$1')";
  private static final String HSQLDB_DIR = "info.hsqldb";
  private static final String HSQLDB_DB = "info";
  private static final String SMALLSQL_DIR = "info.db";

  final private File formDir;
  private Connection connection;

  private boolean hasRecordedInstanceTable = false;

  private DatabaseUtils(File formDir) throws FileSystemException, SQLException {
    this.formDir = formDir;
    connect();
  }

  private static String getFormDatabaseUrl(File formDirectory) throws FileSystemException {

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
      migrateData(getJdbcUrl(oldDbFile), getJdbcUrl(dbFile));
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

  private static boolean isFormRelativeInstancePath(String path) {
    return path.startsWith(INSTANCE_DIR);
  }

  private static Path makeRelative(File parent, File child) {
    Path parentPath = parent.toPath();
    Path childPath = child.toPath();
    return parentPath.relativize(childPath);
  }

  private void connect() throws FileSystemException, SQLException {
    if (connection == null) {
      connection = getConnection(getFormDatabaseUrl(formDir));
    }
  }

  public static <T> T withDb(Path formDir, Function<DatabaseUtils, T> dbUsingFunction) {
    try (DatabaseUtils db = new DatabaseUtils(formDir.toFile())) {
      return dbUsingFunction.apply(db);
    } catch (SQLException e) {
      throw new BriefcaseException(e);
    }
  }

  public static void withDb(Path formDir, Consumer<DatabaseUtils> dbUsingFunction) {
    try (DatabaseUtils db = new DatabaseUtils(formDir.toFile())) {
      dbUsingFunction.accept(db);
    } catch (SQLException e) {
      throw new BriefcaseException(e);
    }
  }

  private void assertRecordedInstanceTable() throws SQLException {
    if (!hasRecordedInstanceTable) {
      try (Statement stmt = connection.createStatement();
           ResultSet rset = stmt.executeQuery(ASSERT_SQL)) {
        if (rset.next() && !isFormRelativeInstancePath(rset.getString(2))) {
          makeRecordedInstanceDirsRelative(connection);
        }
        if (log.isDebugEnabled()) {
          dumpRecordedInstanceTable();
        }
      } catch (SQLException e) {
        log.debug("assertion failed, attempting to create instance table");
        createRecordedInstanceTable(connection);
      }
      hasRecordedInstanceTable = true;
    }
  }

  private void makeRecordedInstanceDirsRelative(Connection c) throws SQLException {
    try (Statement stmt = c.createStatement()) {
      stmt.execute(RELATIVE_DML);
    }
  }

  private void dumpRecordedInstanceTable() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(SELECT_ALL_SQL);
      try (ResultSet rs = stmt.getResultSet()) {
        while (rs.next()) {
          log.debug("recorded: " + rs.getString(1) + " @dir=" + rs.getString(2));
        }
      }
    }
  }

  private static void createRecordedInstanceTable(Connection c) throws SQLException {
    try (Statement stmt = c.createStatement()) {
      stmt.execute(CREATE_DDL);
    }
  }

  @Override
  public synchronized void close() throws SQLException {
    try {
      connection.close();
    } finally {
      connection = null;
    }
  }

  // recorded instances have known instanceIds
  public synchronized void putRecordedInstanceDirectory(String instanceId, File instanceDir) {
    try {
      assertRecordedInstanceTable();

      PreparedStatement insertRecordedInstanceQuery = connection.prepareStatement(INSERT_DML);

      insertRecordedInstanceQuery.setString(1, instanceId);
      insertRecordedInstanceQuery.setString(2, makeRelative(formDir, instanceDir).toString());

      if (1 != insertRecordedInstanceQuery.executeUpdate()) {
        throw new SQLException("Expected one row to be updated");
      }
    } catch (SQLException e) {
      log.error("failed to record instance " + instanceId, e);
    }
  }

  static DatabaseUtils newInstance(File formDirectory) throws FileSystemException, SQLException {
    return new DatabaseUtils(formDirectory);
  }

  private static Connection getConnection(String jdbcUrl) throws SQLException {
    loadDriver(jdbcUrl);
    return DriverManager.getConnection(jdbcUrl);
  }

  private static void loadDriver(String jdbcUrl) throws SQLException {
    if (jdbcUrl.startsWith(SMALLSQL_JDBC_PREFIX)) {
      try {
        Class.forName("smallsql.database.SSDriver");
      } catch (ClassNotFoundException e) {
        throw new SQLException("unable to load smallsql driver", e);
      }
    }
    if (jdbcUrl.startsWith(HSQLDB_JDBC_PREFIX)) {
      try {
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
      } catch (ClassNotFoundException e) {
        throw new SQLException("unable to load hsqldb driver", e);
      }
    }
  }

  private static void migrateData(String fromDbUrl, String toDbUrl) throws SQLException {
    try (Connection fromConn = getConnection(fromDbUrl);
         Connection toConn = getConnection(toDbUrl)) {
      createRecordedInstanceTable(toConn);
      try (PreparedStatement selectStmt = fromConn.prepareStatement(SELECT_ALL_SQL);
           ResultSet results = selectStmt.executeQuery();
           PreparedStatement insertStmt = toConn.prepareStatement(INSERT_DML)) {
        while (results.next()) {
          insertStmt.setString(1, results.getString(1));
          insertStmt.setString(2, results.getString(2));
          insertStmt.executeUpdate();
        }
      }
    }
  }

  // ask whether we have the recorded instance in this briefcase
  // return null if we don't.
  public synchronized File hasRecordedInstance(String instanceId) {
    try {
      assertRecordedInstanceTable();

      PreparedStatement getRecordedInstanceQuery = connection.prepareStatement(SELECT_DIR_SQL);

      getRecordedInstanceQuery.setString(1, instanceId);
      ResultSet values = getRecordedInstanceQuery.executeQuery();
      File f = null;
      while (values.next()) {
        if (f != null) {
          throw new SQLException("Duplicate entries for instanceId: " + instanceId);
        }
        f = new File(formDir, values.getString(1));
      }
      return (f != null && f.exists() && f.isDirectory()) ? f : null;
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("failed to find recorded instance " + instanceId, e);
      }
      return null;
    }
  }
}
