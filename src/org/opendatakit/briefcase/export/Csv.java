package org.opendatakit.briefcase.export;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.getMainHeader;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.getRepeatHeader;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class represents a CSV export output file. It knows how to write
 * its header and contents.
 */
class Csv {
  private final String modelFqn;
  private final String header;
  private final Path output;
  private final boolean sortedOutput;
  private final boolean overwrite;
  private final CsvSubmissionMapper mapper;

  private Csv(String modelFqn, String header, Path output, boolean sortedOutput, boolean overwrite, CsvSubmissionMapper mapper) {
    this.modelFqn = modelFqn;
    this.header = header;
    this.output = output;
    this.sortedOutput = sortedOutput;
    this.overwrite = overwrite;
    this.mapper = mapper;
  }

  static List<Csv> getCsvs(FormDefinition formDef, ExportConfiguration configuration) {
    // Prepare the list of csv files we will export:
    //  - one for the main instance
    //  - one for each repeat group
    List<Csv> csvs = new ArrayList<>();
    csvs.add(main(formDef, configuration));
    List<Csv> repeatCsvs = formDef.getRepeatableFields().stream()
        .collect(groupingBy(Model::getName))
        .values().stream()
        .flatMap(models -> mapToRepeatCsv(formDef, configuration, models))
        .collect(toList());
    csvs.addAll(repeatCsvs);
    return csvs;
  }

  private static Csv main(FormDefinition formDefinition, ExportConfiguration configuration) {
    return new Csv(
        formDefinition.getModel().fqn(),
        getMainHeader(formDefinition.getModel(), formDefinition.isFileEncryptedForm(), configuration.resolveSplitSelectMultiples()),
        buildMainOutputPath(formDefinition, configuration),
        true,
        configuration.resolveOverwriteExistingFiles(),
        CsvSubmissionMappers.main(formDefinition, configuration)
    );
  }

  private static Csv repeat(FormDefinition formDefinition, Model groupModel, ExportConfiguration configuration, Path output) {
    return new Csv(
        groupModel.fqn(),
        getRepeatHeader(groupModel, configuration.resolveSplitSelectMultiples()),
        output,
        false,
        configuration.resolveOverwriteExistingFiles(),
        CsvSubmissionMappers.repeat(formDefinition, groupModel, configuration)
    );
  }

  private static Path buildMainOutputPath(FormDefinition formDefinition, ExportConfiguration configuration) {
    return configuration.getExportDir().resolve(String.format(
        "%s.csv",
        configuration.getFilenameBase(formDefinition.getFormName())
    ));
  }

  private static Path buildRepeatOutputPath(FormDefinition formDefinition, Model groupModel, ExportConfiguration configuration) {
    return configuration.getExportDir().resolve(String.format(
        "%s-%s.csv",
        configuration.getFilenameBase(formDefinition.getFormName()),
        stripIllegalChars(groupModel.getName())
    ));
  }

  private static Path buildRepeatOutputPath(FormDefinition formDefinition, Model groupModel, ExportConfiguration configuration, int sequenceNumber) {
    return configuration.getExportDir().resolve(String.format(
        "%s-%s~%d.csv",
        configuration.getFilenameBase(formDefinition.getFormName()),
        stripIllegalChars(groupModel.getName()),
        sequenceNumber
    ));
  }

  private static Stream<Csv> mapToRepeatCsv(FormDefinition formDef, ExportConfiguration configuration, List<Model> models) {
    if (models.size() == 1)
      return models.stream().map(group -> repeat(formDef, group, configuration, buildRepeatOutputPath(formDef, group, configuration)));
    AtomicInteger sequence = new AtomicInteger(1);
    return models.stream().map(group -> repeat(formDef, group, configuration, buildRepeatOutputPath(formDef, group, configuration, sequence.getAndIncrement())));
  }

  /**
   * This method ensures that the output file is ready to receive new
   * contents by appending lines.
   */
  void prepareOutputFiles() {
    if (!Files.exists(output) || overwrite)
      write(output, Stream.of(header), CREATE, TRUNCATE_EXISTING);
  }

  CsvSubmissionMapper getMapper() {
    return mapper;
  }

  String getModelFqn() {
    return modelFqn;
  }

  /**
   * This method appends the given lines into the file this instance represents.
   */
  void appendLines(CsvLines csvLines) {
    write(output, sortedOutput ? csvLines.sorted() : csvLines.unsorted(), APPEND);
  }
}
