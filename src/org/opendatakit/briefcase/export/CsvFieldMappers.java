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
package org.opendatakit.briefcase.export;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;
import static org.javarosa.core.model.DataType.BINARY;
import static org.javarosa.core.model.DataType.DATE;
import static org.javarosa.core.model.DataType.DATE_TIME;
import static org.javarosa.core.model.DataType.GEOPOINT;
import static org.javarosa.core.model.DataType.NULL;
import static org.javarosa.core.model.DataType.TIME;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.getFileExtension;
import static org.opendatakit.briefcase.reused.UncheckedFiles.getMd5Hash;
import static org.opendatakit.briefcase.reused.UncheckedFiles.stripFileExtension;
import static org.opendatakit.common.utils.WebUtils.parseDate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.javarosa.core.model.DataType;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.Pair;

/**
 * This class contains all the supported mappers from {@link DataType} to CSV compatible
 * values, and an API to produce CSV compatible values and lines from submissions.
 */
@SuppressWarnings("checkstyle:ParameterName")
final class CsvFieldMappers {
  private static final Map<DataType, CsvFieldMapper> mappers = new HashMap<>();

  private static CsvFieldMapper AUDIT_MAPPER = (formName, localId, workingDir, model, maybeElement, configuration) -> maybeElement
      .map(e -> {
        // TODO We should separate the side effect of writing files to disk from the csv output generation

        if (!e.hasValue())
          return empty(e.fqn());

        String sourceFilename = e.getValue();

        if (!configuration.resolveExportMedia())
          return Stream.of(Pair.of(e.fqn(), sourceFilename));

        if (!Files.exists(configuration.getExportMediaPath()))
          createDirectories(configuration.getExportMediaPath());

        Path sourceFile = workingDir.resolve(sourceFilename);

        // When the source file doesn't exist, we return the input value
        if (!exists(sourceFile))
          return Stream.of(Pair.of(e.fqn(), Paths.get("media").resolve(sourceFilename).toString()));

        // When the destination file doesn't exist, we copy the source file
        // there and return its path relative to the instance folder
        Path destinationFile = configuration.getExportMediaPath().resolve(sourceFilename);
        if (!exists(destinationFile)) {
          copy(sourceFile, destinationFile);
          return Stream.of(Pair.of(e.fqn(), Paths.get("media").resolve(destinationFile.getFileName()).toString()));
        }

        // When the destination file has the same hash as the source file,
        // we don't do any side-effect and return its path relative to the
        // instance folder
        Boolean sameHash = OptionalProduct.all(getMd5Hash(sourceFile), getMd5Hash(destinationFile)).map(Objects::equals).orElse(false);
        if (sameHash)
          return Stream.of(Pair.of(e.fqn(), Paths.get("media").resolve(destinationFile.getFileName()).toString()));

        // When the hashes are different, we compute the next sequential suffix for
        // the a new destination file to avoid overwriting the one we found already
        // there. We try every number in the sequence until we find one that won't
        // produce a destination file that exists in the output directory.
        String namePart = stripFileExtension(sourceFilename);
        String extPart = getFileExtension(sourceFilename).map(extension -> "." + extension).orElse("");
        int sequenceSuffix = 2;
        Path sequentialDestinationFile;
        do {
          sequentialDestinationFile = configuration.getExportMediaPath().resolve(String.format("%s-%d%s", namePart, sequenceSuffix++, extPart));
        } while (exists(sequentialDestinationFile));

        // Now that we have a valid destination file, we copy the source file
        // there and return its path relative to the instance folder
        copy(sourceFile, sequentialDestinationFile);
        return Stream.of(Pair.of(e.fqn(), Paths.get("media").resolve(sequentialDestinationFile.getFileName()).toString()));
      })
      .orElse(empty(model.fqn()));

  // Register all non-text supported mappers
  static {
    // All these are simple, 1 column fields
    mappers.put(DATE, simpleMapper(CsvFieldMappers::date));
    mappers.put(TIME, simpleMapper(CsvFieldMappers::time));
    mappers.put(DATE_TIME, simpleMapper(CsvFieldMappers::dateTime));

    // Geopoint fields have a size of 4 columns and have to be decoded
    mappers.put(GEOPOINT, simpleMapper(CsvFieldMappers::geopoint, 4));

    // Binary fields require knowledge of the export configuration and working dir
    mappers.put(BINARY, (__, ___, workingDir, field, element, configuration) -> element
        .map(e -> binary(e, workingDir, configuration))
        .orElse(empty(field.fqn())));

    // Null fields encode groups (repeating and non-repeating), therefore,
    // they require the full context
    mappers.put(NULL, (formName, localId, workingDir, model, element, configuration) -> {
      if (model.isRepeatable())
        return element.map(e -> repeatableGroup(localId, model, e))
            .orElse(empty("SET-OF-" + model.getParent().fqn(), 1));

      if (model.isEmpty() && !model.isRoot())
        return element.map(CsvFieldMappers::text).orElse(empty(model.fqn()));

      return nonRepeatableGroup(formName, localId, workingDir, model, element, configuration);
    });
  }

  static CsvFieldMapper getMapper(Model field, boolean splitSelectMultiples) {
    // If no mapper is available for this field, default to a simple text mapper
    CsvFieldMapper mapper = field.isMetaAudit()
        ? AUDIT_MAPPER
        : Optional.ofNullable(mappers.get(field.getDataType()))
        .orElse(simpleMapper(CsvFieldMappers::text));
    return splitSelectMultiples ? SplitSelectMultiples.decorate(mapper) : mapper;
  }

  /**
   * @see CsvFieldMappers#empty(String, int)
   */
  private static Stream<Pair<String, String>> empty(String fqn) {
    return empty(fqn, 1);
  }

  /**
   * Produces a {@link List} of {@link Pair} instances to represent that some CSV values will be empty.
   * <p>
   * The {@link Pair#left} is the FQN of the CSV column and the {@link Pair#right} contains its value.
   * <p>
   * The output size parameter lets caller sites to specify how much empty pairs they need. This is used
   * with some data types, like the {@link DataType#GEOPOINT}, which need to be represented with
   * 4 columns on a CSV file.
   *
   * @param fqn        the {@link String} FQN of the CSV column
   * @param outputSize the wanted {@link Integer} output size
   * @return a {@link List} of {@link Pair} instances to represent that some CSV values will be empty.
   */
  private static Stream<Pair<String, String>> empty(String fqn, int outputSize) {
    return IntStream.range(0, outputSize).boxed().map(__ -> Pair.of(fqn, null));
  }

  private static CsvFieldMapper simpleMapper(Function<XmlElement, Stream<Pair<String, String>>> mapper) {
    return simpleMapper(mapper, 1);
  }

  private static CsvFieldMapper simpleMapper(Function<XmlElement, Stream<Pair<String, String>>> mapper, int outputSize) {
    return (formName, localId, workingDir, model, element, configuration) -> element
        .map(mapper)
        .orElse(empty(model.fqn(), outputSize));
  }

  private static Stream<Pair<String, String>> text(XmlElement element) {
    return Stream.of(Pair.of(
        element.fqn(),
        element.maybeValue().orElse(null)
    ));
  }

  private static Stream<Pair<String, String>> date(XmlElement element) {
    return formattedDate(element, getDateInstance());
  }

  private static Stream<Pair<String, String>> time(XmlElement element) {
    return formattedDate(element, getTimeInstance());
  }

  private static Stream<Pair<String, String>> dateTime(XmlElement element) {
    return formattedDate(element, getDateTimeInstance());
  }

  private static Stream<Pair<String, String>> formattedDate(XmlElement element, DateFormat formatter) {
    return Stream.of(element.maybeValue()
        .map(value -> Pair.of(element.fqn(), formatter.format(parseDate(value))))
        .orElse(Pair.of(element.fqn(), "")));
  }

  private static Stream<Pair<String, String>> geopoint(XmlElement element) {
    String[] tags = new String[]{"Latitude", "Longitude", "Altitude", "Accuracy"};
    String[] fields = element.maybeValue()
        .map(v -> v.split(" ", 4))
        .orElse(new String[]{});
    return IntStream.range(0, tags.length)
        .mapToObj(i -> Pair.of(element.fqn() + "-" + tags[i], i < fields.length ? fields[i] : null));
  }

  private static Stream<Pair<String, String>> binary(XmlElement element, Path workingDir, ExportConfiguration configuration) {
    // TODO We should separate the side effect of writing files to disk from the csv output generation

    if (!element.hasValue())
      return empty(element.fqn());

    String sourceFilename = element.getValue();

    if (!configuration.resolveExportMedia())
      return Stream.of(Pair.of(element.fqn(), sourceFilename));

    if (!Files.exists(configuration.getExportMediaPath()))
      createDirectories(configuration.getExportMediaPath());

    Path sourceFile = workingDir.resolve(sourceFilename);

    // When the source file doesn't exist, we return the input value
    if (!exists(sourceFile))
      return Stream.of(Pair.of(element.fqn(), Paths.get("media").resolve(sourceFilename).toString()));

    // When the destination file doesn't exist, we copy the source file
    // there and return its path relative to the instance folder
    Path destinationFile = configuration.getExportMediaPath().resolve(sourceFilename);
    if (!exists(destinationFile)) {
      copy(sourceFile, destinationFile);
      return Stream.of(Pair.of(element.fqn(), Paths.get("media").resolve(destinationFile.getFileName()).toString()));
    }

    // When the destination file has the same hash as the source file,
    // we don't do any side-effect and return its path relative to the
    // instance folder
    Boolean sameHash = OptionalProduct.all(getMd5Hash(sourceFile), getMd5Hash(destinationFile)).map(Objects::equals).orElse(false);
    if (sameHash)
      return Stream.of(Pair.of(element.fqn(), Paths.get("media").resolve(destinationFile.getFileName()).toString()));

    // When the hashes are different, we compute the next sequential suffix for
    // the a new destination file to avoid overwriting the one we found already
    // there. We try every number in the sequence until we find one that won't
    // produce a destination file that exists in the output directory.
    String namePart = stripFileExtension(sourceFilename);
    String extPart = getFileExtension(sourceFilename).map(extension -> "." + extension).orElse("");
    int sequenceSuffix = 2;
    Path sequentialDestinationFile;
    do {
      sequentialDestinationFile = configuration.getExportMediaPath().resolve(String.format("%s-%d%s", namePart, sequenceSuffix++, extPart));
    } while (exists(sequentialDestinationFile));

    // Now that we have a valid destination file, we copy the source file
    // there and return its path relative to the instance folder
    copy(sourceFile, sequentialDestinationFile);
    return Stream.of(Pair.of(element.fqn(), Paths.get("media").resolve(sequentialDestinationFile.getFileName()).toString()));
  }

  private static Stream<Pair<String, String>> repeatableGroup(String localId, Model current, XmlElement element) {
    int shift = current.countAncestors() - 1;
    return element == null
        ? empty("SET-OF-" + current.fqn(shift))
        : Stream.of(Pair.of(current.fqn(), localId + "/" + current.fqn(shift)));
  }

  private static Stream<Pair<String, String>> nonRepeatableGroup(String formName, String localId, Path workingDir, Model current, Optional<XmlElement> maybeElement, ExportConfiguration configuration) {
    return current.flatMap(field -> getMapper(field, configuration.resolveSplitSelectMultiples()).apply(
        formName,
        localId,
        workingDir,
        field,
        maybeElement.flatMap(element -> element.findElement(field.getName())),
        configuration
    ));
  }

}
