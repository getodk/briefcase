package org.opendatakit.briefcase.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Optional;

import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Abanda on 1/2/2018.
 */
public class PrivateKeyUtils {
    private static final Logger log = LoggerFactory.getLogger(PrivateKeyUtils.class);
    public static boolean isValidPrivateKey(Path path) {
        //Check if path is a file and is readable
        if (Files.isRegularFile(path) && Files.isReadable(path)) {
            try (PEMReader rdr = new PEMReader(new BufferedReader(new InputStreamReader(Files.newInputStream(path), "UTF-8")))) {
                Optional<Object> o = Optional.ofNullable(rdr.readObject());
                if (!o.isPresent())
                    return false;
                Optional<PrivateKey> pk = extractPrivateKey(o.get());
                return pk.isPresent();
            } catch (IOException e) {
                log.error("Error while reading PEM file", e);
                return false;
            }
        }
        return false;
    }

    public static ErrorsOr<PrivateKey> readPemFile(Path pemFile) {
        try (PEMReader rdr = new PEMReader(new BufferedReader(new InputStreamReader(Files.newInputStream(pemFile), "UTF-8")))) {
            Optional<Object> o = Optional.ofNullable(rdr.readObject());
            if (!o.isPresent())
                return ErrorsOr.errors("The supplied file is not in PEM format.");
            Optional<PrivateKey> pk = extractPrivateKey(o.get());
            if (!pk.isPresent())
                return ErrorsOr.errors("The supplied file does not contain a private key.");
            return ErrorsOr.some(pk.get());
        } catch (IOException e) {
            log.error("Error while reading PEM file", e);
            return ErrorsOr.errors("Briefcase can't read the provided file: " + e.getMessage());
        }
    }

    private static Optional<PrivateKey> extractPrivateKey(Object o) {
        if (o instanceof KeyPair)
            return Optional.of(((KeyPair) o).getPrivate());
        if (o instanceof PrivateKey)
            return Optional.of((PrivateKey) o);
        return Optional.empty();
    }
}
