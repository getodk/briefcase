package org.opendatakit.briefcase.reused.db;

import static org.jooq.SQLDialect.HSQLDB;
import static org.jooq.impl.DSL.sql;
import static org.jooq.impl.DSL.using;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;
import org.jooq.DSLContext;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class BriefcaseDb {
  private final Server server;

  static {
    try {
      Class.forName("org.hsqldb.jdbcDriver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private BriefcaseDb(Server server) {
    this.server = server;
  }

  public static BriefcaseDb create() {
    return new BriefcaseDb(new Server());
  }

  public void startAt(Path workspaceLocation) {
    try {
      Path dbDir = workspaceLocation.toAbsolutePath().resolve("db");
      createDirectories(dbDir);
      HsqlProperties props = new HsqlProperties();
      props.setProperty("server.database.0", dbDir.resolve("briefcase").toString());
      props.setProperty("server.dbname.0", "briefcase");
      // TODO This won't work if there is another Briefcase instance running
      props.setProperty("server.port", 9001);
      server.setProperties(props);
      server.start();
    } catch (IOException | ServerAcl.AclFormatException e) {
      throw new BriefcaseException(e);
    }
  }

  public void stop() {
    if (!server.isNotRunning()) {
      getDslContext().execute(sql("SHUTDOWN"));
      server.signalCloseAllServerConnections();
      server.shutdown();
      server.stop();
    }
  }

  public String getDsn() {
    return "jdbc:hsqldb:hsql://localhost:9001/briefcase";
  }

  public String getUser() {
    return "sa";
  }

  public String getPassword() {
    return "";
  }

  private Connection getConnection() {
    try {
      return DriverManager.getConnection(getDsn(), getUser(), getPassword());
    } catch (SQLException e) {
      throw new BriefcaseException(e);
    }
  }

  public DSLContext getDslContext() {
    return using(getConnection(), HSQLDB);
  }
}
