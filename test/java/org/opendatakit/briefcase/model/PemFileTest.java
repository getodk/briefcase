package org.opendatakit.briefcase.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


/**
 * Created by Abanda on 3/30/2018.
 */
public class PemFileTest {

    public static Path getSomePemFile() {
        Path pemFile;
        try {
            pemFile = Paths.get(Files.createTempFile("private-key_", ".pem").toUri());
            File f = new File("test/resources/private-key.pem");
            Files.copy(f.toPath(), pemFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            pemFile.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pemFile;
    }
}
