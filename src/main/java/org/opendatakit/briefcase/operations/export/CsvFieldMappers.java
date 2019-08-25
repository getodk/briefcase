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
package org.opendatakit.briefcase.operations.export;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.stream.Collectors.toList;
import static org.javarosa.core.model.DataType.BINARY;
import static org.javarosa.core.model.DataType.DATE;
import static org.javarosa.core.model.DataType.DATE_TIME;
import static org.javarosa.core.model.DataType.GEOPOINT;
import static org.javarosa.core.model.DataType.NULL;
import static org.javarosa.core.model.DataType.TIME;
import static org.opendatakit.briefcase.reused.api.Iso8601Helpers.parseDateTime;
import static org.opendatakit.briefcase.reused.api.Iso8601Helpers.parseTime;
import static org.opendatakit.briefcase.reused.api.StringUtils.stripIllegalChars;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.exists;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.getFileExtension;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.getMd5Hash;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.lines;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.stripFileExtension;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.write;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.javarosa.core.model.DataType;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormModel;

/**
 * This class contains all the supported mappers from {@link DataType} to CSV compatible
 * values, and an API to produce CSV compatible values and lines from submissions.
 */
@SuppressWarnings("checkstyle:ParameterName")
final class CsvFieldMappers {
  private static final Map<DataType, CsvFieldMapper> mappers = new HashMap<>();

  private static CsvFieldMapper INDIVIDUAL_FILE_AUDIT_MAPPER = (__, instanceId, workingDir, field, maybeElement, configuration) -> maybeElement
      .map(element -> individualAuditFile(instanceId, workingDir, configuration, element))
      .orElse(empty(field.fqn()));

  private static CsvFieldMapper AGGREGATED_FILE_AUDIT_MAPPER = (formName, localId, workingDir, model, maybeElement, configuration) -> maybeElement
      .map(e -> aggregatedAuditFile(formName, localId, workingDir, configuration, e))
      .orElse(empty(model.fqn()));

  private static CsvFieldMapper AUDIT_MAPPER = INDIVIDUAL_FILE_AUDIT_MAPPER
      .andThen(AGGREGATED_FILE_AUDIT_MAPPER)
      .map(output -> output.filter(pair -> !pair.getLeft().contains("-aggregated")));


  // Register all non-text supported mappers
  static {
    // All these are simple, 1 column fields
    mappers.put(DATE, simpleMapper(CsvFieldMappers::date));
    mappers.put(TIME, simpleMapper(CsvFieldMappers::time));
    mappers.put(DATE_TIME, simpleMapper(CsvFieldMappers::dateTime));

    // Geopoint fields have a size of 4 columns and have to be decoded
    mappers.put(GEOPOINT, simpleMapper(CsvFieldMappers::geopoint, 4));

    // Binary fields require knowledge of the export configuration and working dir
    mappers.put(BINARY, (__, ___, workingDir, field, maybeElement, configuration) -> maybeElement
        .map(element -> binary(element, workingDir, configuration))
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

  static CsvFieldMapper getMapper(FormModel field, boolean splitSelectMultiples) {
    // If no mapper is available for this field, default to a simple text mapper
    CsvFieldMapper mapper = field.isMetaAudit()
        ? AUDIT_MAPPER
        : Optional.ofNullable(mappers.get(field.getDataType())).orElse(simpleMapper(CsvFieldMappers::text));
    return splitSelectMultiples ? SplitSelectMultiples.decorate(mapper) : mapper;
  }

  private static Stream<Pair<String, String>> empty(String fqn) {
    return empty(fqn, 1);
  }

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
    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
        .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
        .appendLiteral(' ')
        .appendValue(ChronoField.DAY_OF_MONTH)
        .appendLiteral(", ")
        .appendValue(ChronoField.YEAR, 4)
        .toFormatter();
    return Stream.of(Pair.of(element.fqn(), element.maybeValue()
        .map(value -> LocalDate.parse(value).format(formatter))
        .orElse("")));
  }

  private static Stream<Pair<String, String>> time(XmlElement element) {
    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .appendLiteral(' ')
        .appendText(ChronoField.AMPM_OF_DAY, TextStyle.FULL)
        .toFormatter();
    return Stream.of(Pair.of(element.fqn(), element.maybeValue()
        .map(value -> parseTime(value).format(formatter))
        .orElse("")));
  }

  private static Stream<Pair<String, String>> dateTime(XmlElement element) {
    return Stream.of(Pair.of(element.fqn(), element.maybeValue()
        .map(CsvFieldMappers::iso8601DateTimeToExportCsvFormat)
        .orElse("")));
  }

  /**
   * Convert an ISO8601 formatted date-time value into the specific
   * format users expect in their exported CSV files.
   */
  static String iso8601DateTimeToExportCsvFormat(String value) {
    return iso8601DateTimeToExportCsvFormat(parseDateTime(value));
  }

  /**
   * Convert an ISO8601 formatted date-time value into the specific
   * format users expect in their exported CSV files.
   */
  static String iso8601DateTimeToExportCsvFormat(OffsetDateTime dateTime) {
    // We make some timezone juggling here to produce exactly what users
    // are expecting. See the tests for more context.
    return dateTime
        .atZoneSameInstant(ZoneOffset.UTC)
        .format(new DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
            .appendLiteral(' ')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral(", ")
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral(' ')
            .appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral(' ')
            .appendText(ChronoField.AMPM_OF_DAY, TextStyle.FULL)
            .toFormatter());
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

    if (element.isEmpty())
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

  private static Stream<Pair<String, String>> individualAuditFile(String instanceId, Path workingDir, ExportConfiguration configuration, XmlElement element) {
    // TODO We should separate the side effect of writing files to disk from the csv output generation

    if (element.isEmpty())
      return empty(element.fqn());

    String sourceFilename = element.getValue();

    if (!configuration.resolveExportMedia())
      return Stream.of(Pair.of(element.fqn(), sourceFilename));

    if (!Files.exists(configuration.getExportMediaPath()))
      createDirectories(configuration.getExportMediaPath());

    Path sourceFile = workingDir.resolve(sourceFilename);

    if (!exists(sourceFile))
      return Stream.of(Pair.of(element.fqn(), Paths.get("media").resolve(sourceFilename).toString()));

    Path destinationFile = configuration.getExportMediaPath().resolve("audit-" + stripIllegalChars(instanceId) + ".csv");
    copy(sourceFile, destinationFile, REPLACE_EXISTING);
    return Stream.of(Pair.of(element.fqn(), Paths.get("media").resolve(destinationFile.getFileName()).toString()));
  }

  private static Stream<Pair<String, String>> aggregatedAuditFile(String formName, String localId, Path workingDir, ExportConfiguration configuration, XmlElement e) {
    if (e.isEmpty())
      return empty(e.fqn() + "-aggregated");

    Path sourceFile = workingDir.resolve(e.getValue());

    // When the source file doesn't exist, we return an empty string
    if (!exists(sourceFile))
      return Stream.of(Pair.of(e.fqn() + "-aggregated", ""));

    // Process the audit file contents and append the instance ID column to all lines
    List<String> sourceLines = lines(sourceFile).collect(toList());
    // We prepend the submission's instance ID to all body lines
    List<String> bodyLines = sourceLines.subList(1, sourceLines.size()).stream()
        .map(line -> localId + "," + line)
        .collect(toList());

    Path destinationFile = configuration.getAuditPath(formName);
    write(destinationFile, bodyLines, APPEND);
    return Stream.of(Pair.of(e.fqn() + "-aggregated", destinationFile.getFileName().toString()));
  }

  private static Stream<Pair<String, String>> repeatableGroup(String localId, FormModel current, XmlElement element) {
    int shift = current.countAncestors() - 1;
    return element == null
        ? empty("SET-OF-" + current.fqn(shift))
        : Stream.of(Pair.of(current.fqn(), localId + "/" + current.fqn(shift)));
  }

  private static Stream<Pair<String, String>> nonRepeatableGroup(String formName, String localId, Path workingDir, FormModel current, Optional<XmlElement> maybeElement, ExportConfiguration configuration) {
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
