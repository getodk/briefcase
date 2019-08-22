package org.opendatakit.briefcase.build;

import static java.nio.file.Files.createDirectories;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Logging;
import org.jooq.meta.jaxb.Schema;
import org.jooq.meta.jaxb.Target;

public class Utils {
  private static Server server;

  public static String startDb(File projectDir) {
    try {
      Path dbDir = projectDir.toPath().resolve("db").toAbsolutePath();
      createDirectories(dbDir);
      HsqlProperties props = new HsqlProperties();
      props.setProperty("server.database.0", dbDir.resolve("briefcase").toString());
      props.setProperty("server.dbname.0", "briefcase");
      props.setProperty("server.port", 9002);
      server = new Server();
      server.setProperties(props);
      server.start();
      return "jdbc:hsqldb:hsql://localhost:9002/briefcase";
    } catch (IOException | ServerAcl.AclFormatException e) {
      throw new RuntimeException(e);
    }
  }

  public static void stopDb() throws ClassNotFoundException, SQLException {
    Class.forName("org.hsqldb.jdbcDriver");
    DriverManager
        .getConnection("jdbc:hsqldb:hsql://localhost:9002/briefcase", "sa", "")
        .createStatement()
        .execute("SHUTDOWN IMMEDIATELY");
    server.signalCloseAllServerConnections();
    server.shutdown();
    server.stop();
  }

  public static void migrateDb(File projectDir, File mainResourcesDir, String dsn) {
    Flyway
        .configure()
        .validateOnMigrate(false)
        .locations("filesystem:" + projectDir.toPath().resolve(mainResourcesDir.toPath()).resolve("db").resolve("migration").toAbsolutePath().toString())
        .dataSource(dsn, "sa", "")
        .load()
        .migrate();
  }

  public static void generateJooq(File projectDir, File mainJavaDir, String dsn) throws Exception {
    GenerationTool.generate(new Configuration()
        .withLogging(Logging.DEBUG)
        .withJdbc(new Jdbc()
            .withDriver("org.hsqldb.jdbc.JDBCDriver")
            .withUrl(dsn)
            .withUser("sa")
            .withPassword(""))
        .withGenerator(new Generator()
            .withDatabase(new Database()
                .withName("org.jooq.meta.hsqldb.HSQLDBDatabase")
                .withIncludes(".*")
                .withExcludes("flyway_schema_history")
                .withSchemata(new Schema().withInputSchema("PUBLIC")))
            .withTarget(new Target()
                .withPackageName("org.opendatakit.briefcase.jooq")
                .withDirectory(projectDir.toPath().resolve(mainJavaDir.toPath()).toAbsolutePath().toString()))
            .withGenerate(new Generate()
                .withGeneratedAnnotation(false))
        ));
  }
}
