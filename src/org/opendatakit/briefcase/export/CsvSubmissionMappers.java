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

import static java.text.DateFormat.getDateTimeInstance;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.javarosa.core.model.DataType.DATE;
import static org.javarosa.core.model.DataType.DATE_TIME;
import static org.javarosa.core.model.DataType.GEOPOINT;
import static org.javarosa.core.model.DataType.TIME;
import static org.opendatakit.briefcase.export.CsvFieldMappers.getMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javarosa.core.model.DataType;
import org.javarosa.core.model.SelectChoice;
import org.opendatakit.briefcase.reused.Pair;

final class CsvSubmissionMappers {
  private static final Set<DataType> EMPTY_COL_WHEN_NULL_DATATYPES = Stream.of(GEOPOINT, DATE, TIME, DATE_TIME).collect(toSet());

  /**
   * This value will be used for {@link Submission} instances without submission date.
   * The idea is that these submissions should be the older than any other.
   */
  private static final OffsetDateTime MIN_SUBMISSION_DATE = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, OffsetDateTime.now().getOffset());

  /**
   * Factory that will produce {@link CsvLines} corresponding to the main output file
   * of a form.
   */
  static CsvSubmissionMapper main(FormDefinition formDefinition, ExportConfiguration configuration) {
    return submission -> {
      List<String> cols = new ArrayList<>();
      cols.add(encode(submission.getSubmissionDate().map(CsvSubmissionMappers::format).orElse(null), false));
      cols.addAll(formDefinition.getModel().flatMap(field -> getMapper(field, configuration.resolveSplitSelectMultiples()).apply(
          formDefinition.getFormName(),
          submission.getInstanceId(formDefinition.hasRepeatableFields()),
          submission.getWorkingDir(),
          field,
          submission.findElement(field.getName()),
          configuration
      ).map(value -> encodeMainValue(field, value))).collect(Collectors.toList()));
      cols.add(submission.getInstanceId(formDefinition.hasRepeatableFields()));
      if (formDefinition.isFileEncryptedForm())
        cols.add(submission.getValidationStatus().asCsvValue());
      return CsvLines.of(
          formDefinition.getModel().fqn(),
          submission.getSubmissionDate().orElse(MIN_SUBMISSION_DATE),
          cols.stream().collect(joining(","))
      );
    };
  }

  /**
   * Factory that will produce {@link CsvLines} corresponding to any repeat output file
   * of a form.
   */
  static CsvSubmissionMapper repeat(FormDefinition formDefinition, Model groupModel, ExportConfiguration configuration) {
    return submission -> CsvLines.of(
        groupModel.fqn(),
        submission.getSubmissionDate().orElse(MIN_SUBMISSION_DATE),
        submission.getElements(groupModel.fqn()).stream().map(element -> {
          List<String> cols = new ArrayList<>();
          cols.addAll(groupModel.flatMap(field -> getMapper(field, configuration.resolveSplitSelectMultiples()).apply(
              formDefinition.getFormName(),
              element.getCurrentLocalId(field, submission.getInstanceId(true)),
              submission.getWorkingDir(),
              field,
              element.findElement(field.getName()),
              configuration
          ).map(CsvSubmissionMappers::encodeRepeatValue)).collect(toList()));
          cols.add(encode(element.getParentLocalId(groupModel, submission.getInstanceId(true)), false));
          cols.add(encode(element.getCurrentLocalId(groupModel, submission.getInstanceId(true)), false));
          cols.add(encode(element.getGroupLocalId(groupModel, submission.getInstanceId(true)), false));
          return cols.stream().collect(joining(","));
        }).collect(toList())
    );
  }

  /**
   * Produce a CSV line with the main form's header column names.
   *
   * @param model       {@link Model} of the form
   * @param isEncrypted {@link Boolean} indicating if the form is encrypted
   * @return a {@link String} with the main form's header column names
   */
  static String getMainHeader(Model model, boolean isEncrypted, boolean splitSelectMultiples) {
    StringBuilder sb = new StringBuilder();
    sb.append("SubmissionDate");
    model.forEach(field -> {
      List<String> names = new ArrayList<>();
      names.addAll(field.getNames());
      if (field.isChoiceList() && splitSelectMultiples) {
        List<SelectChoice> choices = field.getChoices();
        names.addAll(choices.stream().map(choice -> field.getName() + "/" + choice.getValue()).collect(toList()));
      }
      names.forEach(name -> sb.append(",").append(name));
    });
    sb.append(",").append("KEY");
    if (isEncrypted)
      sb.append(",").append("isValidated");
    return sb.toString();
  }

  /**
   * Produce a CSV line with a repeat group's header column names.
   *
   * @param groupModel {@link Model} of the group
   * @return a {@link String} with a repeat group's header column names
   */
  static String getRepeatHeader(Model groupModel, boolean splitSelectMultiples) {
    int shift = groupModel.countAncestors();
    StringBuilder sb = new StringBuilder();
    groupModel.forEach(field -> {
      List<String> names = field.getNames(shift);
      if (field.isChoiceList() && splitSelectMultiples)
        names.addAll(field.getChoices().stream().map(choice -> field.getName() + "/" + choice.getValue()).collect(toList()));
      names.forEach(name -> sb.append(",").append(name));
    });
    sb.append(",").append("PARENT_KEY");
    sb.append(",").append("KEY");
    sb.append(",").append("SET-OF-").append(groupModel.getName());
    return sb.toString().substring(1);
  }

  static String encode(String string, boolean allowNulls) {
    if (string == null || string.isEmpty())
      return allowNulls ? "" : "\"\"";
    if (string.contains("\n") || string.contains("\"") || string.contains(","))
      return String.format("\"%s\"", string.replaceAll("\"", "\"\""));
    return string;
  }

  private static String format(OffsetDateTime offsetDateTime) {
    return getDateTimeInstance().format(new Date(offsetDateTime.toInstant().toEpochMilli()));
  }

  static String encodeMainValue(Model field, Pair<String, String> value) {
    return encode(
        value.getRight(),
        EMPTY_COL_WHEN_NULL_DATATYPES.contains(field.getDataType()) || value.getLeft().startsWith("meta")
    );
  }

  static String encodeRepeatValue(Pair<String, String> pair) {
    return encode(
        pair.getRight(),
        // It would be really surprising to have meta fields in a repeat. This is the original implementation, though.
        pair.getLeft().startsWith("meta") || pair.getLeft().startsWith("SET-OF")
    );
  }
}
