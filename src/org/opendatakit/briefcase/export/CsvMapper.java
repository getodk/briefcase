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
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.javarosa.core.model.DataType.BINARY;
import static org.javarosa.core.model.DataType.DATE;
import static org.javarosa.core.model.DataType.DATE_TIME;
import static org.javarosa.core.model.DataType.GEOPOINT;
import static org.javarosa.core.model.DataType.NULL;
import static org.javarosa.core.model.DataType.TIME;
import static org.opendatakit.briefcase.export.FieldMapper.simpleMapper;
import static org.opendatakit.briefcase.reused.UncheckedFiles.copy;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.getMd5Hash;
import static org.opendatakit.common.utils.WebUtils.parseDate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.javarosa.core.model.DataType;
import org.opendatakit.briefcase.reused.Pair;

/**
 * This class contains all the supported mappers from {@link DataType} to CSV compatible
 * values, and an API to produce CSV compatible values and lines from submissions.
 */
@SuppressWarnings("checkstyle:ParameterName")
class CsvMapper {
  private static final Map<String, String> fileHashMap = new ConcurrentHashMap<>();
  private static final Set<DataType> EMPTY_COL_WHEN_NULL_DATATYPES = Stream.of(GEOPOINT, DATE, TIME, DATE_TIME).collect(toSet());
  private static Map<DataType, FieldMapper> mappers = new HashMap<>();

  // Register all non-text supported mappers
  static {
    // All these are simple, 1 column fields
    mappers.put(DATE, simpleMapper(CsvMapper::date));
    mappers.put(TIME, simpleMapper(CsvMapper::time));
    mappers.put(DATE_TIME, simpleMapper(CsvMapper::dateTime));

    // Geopoint fields have a size of 4 columns and have to be decoded
    mappers.put(GEOPOINT, simpleMapper(CsvMapper::geopoint, 4));

    // Binary fields require knowledge of the export configuration and working dir
    mappers.put(BINARY, (__, workingDir, field, element, exportMedia, exportMediaPath) -> element
        .map(e -> binary(e, exportMedia, workingDir, exportMediaPath))
        .orElse(empty(field.fqn())));

    // Null fields encode groups (repeating and non-repeating), therefore,
    // they require the full context
    mappers.put(NULL, (localId, workingDir, model, element, exportMedia, exportMediaPath) -> {
      if (model.isRepeatable())
        return element.map(e -> repeatableGroup(localId, model, e))
            .orElse(empty("SET-OF-" + model.getParent().fqn(), 1));

      if (model.isEmpty() && !model.isRoot())
        return element.map(CsvMapper::text).orElse(empty(model.fqn()));

      return nonRepeatableGroup(localId, workingDir, model, element, exportMedia, exportMediaPath);
    });
  }

  /**
   * Produce a CSV line with the main form's header column names.
   *
   * @param model       {@link Model} of the form
   * @param isEncrypted {@link Boolean} indicating if the form is encrypted
   * @return a {@link String} with the main form's header column names
   */
  static String getMainHeader(Model model, boolean isEncrypted) {
    StringBuilder sb = new StringBuilder();
    sb.append("SubmissionDate");
    model.forEach(field -> field.getNames().forEach(name -> sb.append(",").append(name)));
    sb.append(",").append("KEY");
    if (isEncrypted)
      sb.append(",").append("isValidated");
    return sb.toString() + "\n";
  }

  /**
   * Produce a CSV line with a repeat group's header column names.
   *
   * @param groupModel {@link Model} of the group
   * @return a {@link String} with a repeat group's header column names
   */
  static String getRepeatHeader(Model groupModel) {
    int shift = groupModel.countAncestors();
    StringBuilder sb = new StringBuilder();
    groupModel.forEach(m -> m.getNames(shift).forEach(name -> sb.append(",").append(name)));
    sb.append(",").append("PARENT_KEY");
    sb.append(",").append("KEY");
    sb.append(",").append("SET-OF-").append(groupModel.getName());
    return sb.toString().substring(1) + "\n";
  }

  /**
   * Produces a {@link Pair} containing a {@link Submission} instance's {@link OffsetDateTime}
   * submission date and its main form's values.
   *
   * @param submission      the {@link Submission} instance to use
   * @param model           the {@link Submission} instance's form's {@link Model}
   * @param isEncrypted     {@link Boolean} indicating if the form is encrypted
   * @param exportMedia     a {@link Boolean} indicating if we have to export media files too
   * @param exportMediaPath the {@link Path} to the output media dir
   * @return a {@link Pair} containing the {@link OffsetDateTime} submission date and a {@link String} CSV line
   */
  static String getMainSubmissionLines(Submission submission, Model model, boolean isEncrypted, boolean exportMedia, Path exportMediaPath) {
    StringBuilder sb = new StringBuilder();
    sb.append(encode(submission.getSubmissionDate().map(CsvMapper::format).orElse(null), false));
    model.forEach(field -> getMapper(field).apply(
        submission.getInstanceId(),
        submission.getWorkingDir(),
        field,
        submission.findElement(field.getName()),
        exportMedia,
        exportMediaPath
    ).forEach(value -> sb.append(",").append(encode(
        value.getRight(),
        EMPTY_COL_WHEN_NULL_DATATYPES.contains(field.getDataType()) || value.getLeft().startsWith("meta")
    ))));
    sb.append(",").append(submission.getInstanceId());
    if (isEncrypted)
      sb.append(",").append(submission.getValidationStatus().asCsvValue());
    return sb.toString() + "\n";
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
  static List<Pair<String, String>> empty(String fqn, int outputSize) {
    return IntStream.range(0, outputSize).boxed().map(__ -> Pair.of(fqn, (String) null)).collect(toList());
  }

  /**
   * Produces a {@link List} of CSV compatible {@link String} lines with the values of
   * a {@link Submission} instance's particular repeat group.
   *
   * @param groupModel      the repeat group's {@link Model}
   * @param elements        a {@link List} of {@link XmlElement} instances to be transformed into a CSV line
   * @param exportMedia     a {@link Boolean} indicating if we have to export media files too
   * @param exportMediaPath the {@link Path} to the output media dir
   * @param instanceId      the instance ID of the {@link Submission} instance that it's being exported
   * @param workingDir      the {@link Path} to the working directory of the {@link Submission} instance that it's being exported
   * @return a {@link List} of CSV compatible {@link String} lines
   */
  static String getRepeatCsvLine(Model groupModel, List<XmlElement> elements, boolean exportMedia, Path exportMediaPath, String instanceId, Path workingDir) {
    StringBuilder sb = new StringBuilder();
    elements.forEach(element -> {
      String localId = element.getCurrentLocalId(instanceId);
      StringBuilder sb2 = new StringBuilder();
      groupModel.forEach(field -> getMapper(field).apply(
          localId,
          workingDir,
          field,
          element.findElement(field.getName()),
          exportMedia,
          exportMediaPath
      ).forEach(pair -> sb2.append(",").append(encode(pair.getRight(), pair.getLeft().startsWith("meta") || pair.getLeft().startsWith("SET-OF")))));

      sb2.append(",").append(encode(element.getParentLocalId(instanceId), false));
      sb2.append(",").append(encode(element.getCurrentLocalId(instanceId), false));
      sb2.append(",").append(encode(element.getGroupLocalId(instanceId), false));
      sb.append(sb2.toString().substring(1)).append("\n");
    });
    return sb.toString();
  }

  private static FieldMapper getMapper(Model field) {
    return Optional.ofNullable(mappers.get(field.getDataType()))
        // If no mapper has been defined, we'll just output the text
        .orElse(simpleMapper(CsvMapper::text));
  }

  private static List<Pair<String, String>> text(XmlElement element) {
    return singletonList(Pair.of(
        element.fqn(),
        element.maybeValue().orElse(null)
    ));
  }

  private static List<Pair<String, String>> date(XmlElement element) {
    return formattedDate(element, getDateInstance());
  }

  private static List<Pair<String, String>> time(XmlElement element) {
    return formattedDate(element, getTimeInstance());
  }

  private static List<Pair<String, String>> dateTime(XmlElement element) {
    return formattedDate(element, getDateTimeInstance());
  }

  private static List<Pair<String, String>> geopoint(XmlElement element) {
    String[] tags = new String[]{"Latitude", "Longitude", "Altitude", "Accuracy"};
    String[] fields = element.maybeValue()
        .map(v -> v.split(" ", 4))
        .orElse(new String[]{});
    return IntStream.range(0, tags.length)
        .mapToObj(i -> Pair.of(element.fqn() + "-" + tags[i], i < fields.length ? fields[i] : null))
        .collect(toList());
  }

  private static List<Pair<String, String>> binary(XmlElement element, boolean exportMedia, Path workingDir, Path exportMediaPath) {
    if (!element.hasValue())
      return empty(element.fqn());

    String binaryFilename = element.getValue();

    if (!exportMedia)
      return singletonList(Pair.of(element.fqn(), binaryFilename));

    if (!Files.exists(exportMediaPath))
      createDirectories(exportMediaPath);

    int dotIndex = binaryFilename.lastIndexOf(".");
    String namePart = (dotIndex == -1) ? binaryFilename : binaryFilename.substring(0,
        dotIndex);
    String extPart = (dotIndex == -1) ? "" : binaryFilename.substring(dotIndex);

    File binaryFile = new File(workingDir.toFile(), binaryFilename);
    int version = 1;
    File destFile = exportMediaPath.resolve(binaryFilename).toFile();
    boolean exists = false;
    String binaryFileHash;
    String destFileHash;

    // TODO Refactor this and add semantics
    if (destFile.exists() && binaryFile.exists()) {
      binaryFileHash = getMd5Hash(binaryFile.toPath());

      while (destFile.exists()) {
        if (fileHashMap.containsKey(destFile.getName())) {
          destFileHash = fileHashMap.get(destFile.getName());
        } else {
          destFileHash = getMd5Hash(destFile.toPath());
          if (destFileHash != null) {
            fileHashMap.put(destFile.getName(), destFileHash);
          }
        }

        if (destFileHash != null && destFileHash.equals(binaryFileHash)) {
          exists = true;
          break;
        }

        String otherDestBinaryFilename = namePart + "-" + (++version) + extPart;
        destFile = exportMediaPath.resolve(otherDestBinaryFilename).toFile();
      }
    }
    if (binaryFile.exists() && !exists) {
      copy(binaryFile.toPath(), destFile.toPath());
    }
    return singletonList(Pair.of(element.fqn(), "media" + File.separator + destFile.getName()));
  }

  private static List<Pair<String, String>> repeatableGroup(String localId, Model current, XmlElement element) {
    int shift = current.countAncestors() - 1;
    return element == null
        ? empty("SET-OF-" + current.fqn(shift))
        : singletonList(Pair.of(current.fqn(), localId + "/" + current.fqn(shift)));
  }

  private static List<Pair<String, String>> nonRepeatableGroup(String localId, Path workingDir, Model current, Optional<XmlElement> maybeElement, boolean exportMedia, Path exportMediaPath) {
    return current.flatMap(field -> getMapper(field).apply(
        localId,
        workingDir,
        field,
        maybeElement.flatMap(element -> element.findElement(field.getName())),
        exportMedia,
        exportMediaPath
    ));
  }

  @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
  private static List<Pair<String, String>> empty(String fqn) {
    return empty(fqn, 1);
  }

  private static List<Pair<String, String>> formattedDate(XmlElement element, DateFormat formatter) {
    return element.maybeValue()
        .map(value -> singletonList(Pair.of(element.fqn(), formatter.format(parseDate(value)))))
        .orElse(singletonList(Pair.of(element.fqn(), "")));
  }

  private static String format(OffsetDateTime offsetDateTime) {
    DateFormat formatter = getDateTimeInstance();
    return formatter.format(new Date(offsetDateTime.toInstant().toEpochMilli()));
  }

  private static String encode(String string, boolean allowNulls) {
    if (string == null || string.isEmpty())
      return allowNulls ? "" : "\"\"";
    if (string.contains("\n") || string.contains("\"") || string.contains(","))
      return String.format("\"%s\"", string.replaceAll("\"", "\"\""));
    return string;
  }


}
