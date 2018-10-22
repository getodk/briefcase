package org.opendatakit.briefcase.export;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.getMainHeader;
import static org.opendatakit.briefcase.export.CsvSubmissionMappers.getRepeatHeader;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.UncheckedFiles;

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

  /**
   * Factory for the main CSV export file of a form.
   */
  static Csv main(FormDefinition formDefinition, ExportConfiguration configuration) {
    Path output = configuration.getExportDir().resolve(
        configuration.getExportFileName().orElse(stripIllegalChars(formDefinition.getFormName()) + ".csv")
    );
    return new Csv(
        formDefinition.getModel().fqn(),
        getMainHeader(formDefinition.getModel(), formDefinition.isFileEncryptedForm(), configuration.resolveSplitSelectMultiples()),
        output,
        true,
        configuration.resolveOverwriteExistingFiles(),
        CsvSubmissionMappers.main(formDefinition, configuration)
    );
  }

  /**
   * Factory of any repeat CSV export file.
   */
  static Csv repeat(FormDefinition formDefinition, Model groupModel, ExportConfiguration configuration) {
    String repeatFileNameBase = configuration.getExportFileName()
        .map(UncheckedFiles::stripFileExtension)
        .orElse(stripIllegalChars(formDefinition.getFormName()));
    Path output = configuration.getExportDir().resolve(repeatFileNameBase + "-" + stripIllegalChars(groupModel.getName()) + ".csv");
    return new Csv(
        groupModel.fqn(),
        getRepeatHeader(groupModel, configuration.resolveSplitSelectMultiples()),
        output,
        false,
        configuration.resolveOverwriteExistingFiles(),
        CsvSubmissionMappers.repeat(formDefinition, groupModel, configuration)
    );
  }

  /**
   * Returns true if the grandparent node of the given Model is the model's root
   * <p>
   * Example 1:
   * <p>
   * <code><pre>
   * &lt;data&gt;
   * &nbsp;&nbsp;&lt;/some_field&gt;
   * &lt;/data&gt;
   * </pre></code>
   * <p>
   * In this example:
   * <ul>
   * <li>&lt;data&gt; has a parent with <code>null</code> name</li>
   * <li>Returns true on &lt;some_field&gt;</li>
   * </ul>
   */
  private static boolean grandParentIsRoot(Model current) {
    return current.hasParent()
        && current.getParent().hasParent() // Check if current has a grandparent
        && current.getParent().getParent().getName() == null; // The root node has a null name
  }

  private static boolean parentIsRepeatGroup(Model current) {
    return current.hasParent()
        && current.getParent().isRepeatable();
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
