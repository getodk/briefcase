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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class abstracts all the functionality of the instance-tracking
 * database.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class DatabaseUtils {
  private static boolean DEBUG = true;

  private Connection connection;
  
  private boolean hasRecordedInstanceTable = false;
  private PreparedStatement getRecordedInstanceQuery = null;
  private PreparedStatement insertRecordedInstanceQuery = null;
  private PreparedStatement deleteRecordedInstanceQuery = null;
  
  public DatabaseUtils(Connection connection) {
    this.connection = connection;
  }
  
  public void close() throws SQLException {
    if ( getRecordedInstanceQuery != null ) {
      try {
        getRecordedInstanceQuery.close();
      } catch ( SQLException e ) {
        e.printStackTrace();
      } finally {
        getRecordedInstanceQuery = null;
      }
    }
    try {
      connection.close();
    } finally {
      connection =  null;
    }
  }
  
  private void assertRecordedInstanceTable() throws SQLException {
    if ( hasRecordedInstanceTable ) return;
    
    Statement stmt = connection.createStatement();
    try {
      stmt.execute("SELECT FIRST(instanceId) FROM recorded_instance");
      if ( DEBUG ) {
        stmt.execute("SELECT instanceId, directory FROM recorded_instance");
        ResultSet rs = stmt.getResultSet();
        while ( rs.next() ) {
          System.err.println("recorded: " + rs.getString(1) + " @dir=" + rs.getString(2));
        }
      }
      hasRecordedInstanceTable = true;
    } catch ( SQLException e ) {
      // doesn't exist -- create it...
      stmt.execute("CREATE TABLE recorded_instance (" +
    		" instanceId varchar2(256), directory varchar2(4096) )" );
      hasRecordedInstanceTable = true;
    } finally {
      stmt.close();
    }
  }
  
  // recorded instances have known instanceIds
  public void putRecordedInstanceDirectory( String instanceId, File dir) {
    try {
      assertRecordedInstanceTable();
      
      if ( insertRecordedInstanceQuery == null ) {
        insertRecordedInstanceQuery = 
            connection.prepareStatement("INSERT INTO recorded_instance (instanceId, directory) VALUES(?,?)");
      }
            
      insertRecordedInstanceQuery.setString(1, instanceId);
      insertRecordedInstanceQuery.setString(2, dir.getAbsolutePath());
      
      if ( 1 != insertRecordedInstanceQuery.executeUpdate() ) {
        throw new SQLException("Expected one row to be updated");
      }
    } catch ( SQLException e ) {
      e.printStackTrace();
    }
  }
  
  // recorded instances have known instanceIds
  private void forgetRecordedInstance( String instanceId ) {
    try {
      assertRecordedInstanceTable();
      
      if ( deleteRecordedInstanceQuery == null ) {
        deleteRecordedInstanceQuery = 
            connection.prepareStatement("DELETE FROM recorded_instance WHERE instanceId = ?");
      }
      
      deleteRecordedInstanceQuery.setString(1, instanceId );
      
      if ( deleteRecordedInstanceQuery.executeUpdate() > 1 ) {
        throw new SQLException("Expected one row to be deleted");
      }
    } catch ( SQLException e ) {
      e.printStackTrace();
    }
  }
  
  // ask whether we have the recorded instance in this briefcase
  // return null if we don't.
  public File hasRecordedInstance( String instanceId ) {
    try {
      assertRecordedInstanceTable();
      
      if ( getRecordedInstanceQuery == null ) {
        getRecordedInstanceQuery = 
            connection.prepareStatement("SELECT directory FROM recorded_instance WHERE instanceId = ?");
      }
      
      getRecordedInstanceQuery.setString(1, instanceId);
      ResultSet values = getRecordedInstanceQuery.executeQuery();
      File f = null;
      while ( values.next() ) {
        if ( f != null ) {
          throw new SQLException("Duplicate entries for instanceId: " + instanceId);
        }
        f = new File( values.getString(1) );
      }
      return (f != null && f.exists() && f.isDirectory()) ? f : null;
    } catch ( SQLException e ) {
      e.printStackTrace();
      return null;
    }
  }
  
  public void assertRecordedInstanceDirectory( String instanceId, File dir) {
    forgetRecordedInstance( instanceId );
    putRecordedInstanceDirectory(instanceId, dir);
  }
    
  public void updateInstanceLists( Set<File> instanceList ) {
    Set<File> workingSet = new TreeSet<File>(instanceList);
    // first, go through the database's reported set of directories
    // removing all that aren't in the set...
    Statement stmt = null;
    try {
      assertRecordedInstanceTable();
      stmt = connection.createStatement();
      ResultSet values = stmt.executeQuery("SELECT instanceId, directory FROM recorded_instance");
      while ( values.next() ) {
        String instanceId = values.getString(1);
        File f = new File(values.getString(2));
        if ( !f.exists() || !f.isDirectory() ) {
          forgetRecordedInstance(instanceId);
        } else {
          workingSet.remove(f);
        }
      }
    } catch ( SQLException e ) {
      e.printStackTrace();
    }
  }
}
