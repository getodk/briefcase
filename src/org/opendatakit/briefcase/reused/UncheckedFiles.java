/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.reused;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds unchecked versions of some methods in {@link Files}.
 */
public class UncheckedFiles {
  private static final String README_CONTENTS = "" +
      "This ODK Briefcase storage area retains\n" +
      "all the forms and submissions that have been\n" +
      "gathered into it.\n" +
      "\n" +
      "Users should not navigate into or modify its\n" +
      "contents unless explicitly directed to do so.\n";
  private static final Logger log = LoggerFactory.getLogger(UncheckedFiles.class);

  public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) {
    try {
      return Files.createTempFile(prefix, suffix, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path write(Path path, Stream<String> lines, OpenOption... options) {
    try {
      return Files.write(path, (Iterable<String>) lines::iterator, UTF_8, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path write(Path path, String contents, OpenOption... options) {
    try {
      return Files.write(path, contents.getBytes(UTF_8), options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path createDirectories(Path dir, FileAttribute<?>... attrs) {
    try {
      return Files.createDirectories(dir, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void deleteRecursive(Path path) {
    walk(path).filter(Files::isRegularFile).forEach(UncheckedFiles::delete);
    walk(path).filter(p -> !Files.isRegularFile(p)).sorted(Comparator.reverseOrder()).forEach(UncheckedFiles::delete);
  }

  public static void delete(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path copy(Path source, Path target, CopyOption... options) {
    try {
      return Files.copy(source, target, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Stream<Path> walk(Path path, FileVisitOption... options) {
    try {
      return Files.walk(path, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs) {
    try {
      return Files.createTempDirectory(dir, prefix, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) {
    try {
      return Files.createTempDirectory(prefix, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void createFile(Path path, FileAttribute<?>... attrs) {
    try {
      Files.createFile(path, attrs);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static boolean exists(Path path, LinkOption... options) {
    return Files.exists(path, options);
  }

  public static byte[] readAllBytes(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Optional<String> getMd5Hash(Path file) {
    try {
      // CTS (6/15/2010) : stream file through digest instead of handing
      // it the
      // byte[]
      MessageDigest md = MessageDigest.getInstance("MD5");
      int chunkSize = 256;

      byte[] chunk = new byte[chunkSize];

      // Get the size of the file
      long lLength = Files.size(file);

      if (lLength > Integer.MAX_VALUE) {
        log.error("File is too large");
        return Optional.empty();
      }

      int length = (int) lLength;

      InputStream is = Files.newInputStream(file);

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
      return Optional.of(md5);

    } catch (NoSuchAlgorithmException e) {
      log.error("MD5 calculation failed", e);
      return Optional.empty();
    } catch (FileNotFoundException e) {
      log.error("No File", e);
      return Optional.empty();
    } catch (IOException e) {
      log.error("Problem reading from file", e);
      return Optional.empty();
    }

  }

  public static String stripFileExtension(String fileName) {
    return fileName.contains(".")
        ? fileName.substring(0, fileName.lastIndexOf("."))
        : fileName;
  }

  public static Optional<String> getFileExtension(String fileName) {
    return fileName.contains(".")
        ? Optional.of(fileName.substring(fileName.lastIndexOf(".") + 1))
        : Optional.empty();
  }

  public static long checksumOf(Path file) {
    try {
      return FileUtils.checksumCRC32(file.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static boolean isFormDir(Path dir) {
    String dirName = dir.getFileName().toString();
    return Files.isDirectory(dir)
        // Ignore hidden mac/linux hidden folders
        && !dirName.startsWith(".")
        // Check for presence of the blank form
        && Files.exists(dir.resolve(dirName + ".xml"));
  }

  public static boolean isInstanceDir(Path dir) {
    return Files.isDirectory(dir)
        // Ignore hidden mac/linux hidden folders
        && !isHidden(dir)
        // Check for presence of a submission.xml file inside
        && Files.exists(dir.resolve("submission.xml"));
  }

  private static boolean isHidden(Path path) {
    try {
      return Files.isHidden(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String readFirstLine(Path path) {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      return reader.readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static URI toURI(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static InputStream newInputStream(Path path, OpenOption... options) {
    try {
      return Files.newInputStream(path, options);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static OutputStreamWriter newOutputStreamWriter(Path path, OpenOption... options) {
    try {
      return new OutputStreamWriter(Files.newOutputStream(path, options));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static InputStreamReader newInputStreamReader(InputStream in, String charsetName) {
    try {
      return new InputStreamReader(in, charsetName);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void append(String content, OutputStreamWriter outputStreamWriter) {
    try {
      outputStreamWriter.append(content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void close(OutputStreamWriter osw) {
    try {
      osw.flush();
      osw.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Stream<Path> list(Path dir) {
    try {
      return Files.list(dir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void createBriefcaseDir(Path briefcaseDir) {
    createDirectories(briefcaseDir);
    createDirectories(briefcaseDir.resolve("forms"));
    write(briefcaseDir.resolve("readme.txt"), README_CONTENTS);
  }
}
