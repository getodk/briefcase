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

package org.opendatakit.briefcase.util;

import static org.opendatakit.briefcase.util.FileSystemUtils.INSTANCE_DIR;
import static org.opendatakit.briefcase.util.FileSystemUtils.SMALLSQL_JDBC_PREFIX;
import static org.opendatakit.briefcase.util.FileSystemUtils.getFormDatabaseUrl;
import static org.opendatakit.briefcase.util.FileSystemUtils.isFormRelativeInstancePath;
import static org.opendatakit.briefcase.util.FileSystemUtils.makeRelative;

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
import org.opendatakit.briefcase.model.FileSystemException;
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

  private static final Logger log = LoggerFactory.getLogger(DatabaseUtils.class);

  private static final String CREATE_DDL = "CREATE TABLE recorded_instance (instanceId varchar(256) primary key, directory varchar(4096))";
  private static final String ASSERT_SQL = "SELECT instanceId, directory FROM recorded_instance limit 1";
  private static final String SELECT_ALL_SQL = "SELECT instanceId, directory FROM recorded_instance";
  private static final String SELECT_DIR_SQL = "SELECT directory FROM recorded_instance WHERE instanceId = ?";
  private static final String INSERT_DML = "INSERT INTO recorded_instance (instanceId, directory) VALUES(?,?)";
  private static final String RELATIVE_DML = "UPDATE recorded_instance set directory = regexp_replace(directory,'.*(" + INSTANCE_DIR + ")','$1')";

  final private File formDir;
  private Connection connection;

  private boolean hasRecordedInstanceTable = false;
  private PreparedStatement getRecordedInstanceQuery = null;
  private PreparedStatement insertRecordedInstanceQuery = null;

  private DatabaseUtils(File formDir) throws FileSystemException, SQLException {
    this.formDir = formDir;
    connect();
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
    if (getRecordedInstanceQuery != null) {
      try {
        getRecordedInstanceQuery.close();
      } catch (SQLException e) {
        log.error("failed to close connection", e);
      } finally {
        getRecordedInstanceQuery = null;
      }
    }
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

      if (insertRecordedInstanceQuery == null) {
        insertRecordedInstanceQuery =
            connection.prepareStatement(INSERT_DML);
      }

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
  }

  static void migrateData(String fromDbUrl, String toDbUrl) throws SQLException {
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

      if (getRecordedInstanceQuery == null) {
        getRecordedInstanceQuery = connection.prepareStatement(SELECT_DIR_SQL);
      }

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
