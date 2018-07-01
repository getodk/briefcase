package org.opendatakit.briefcase.export;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class represents a group of {@link CsvLine} belonging to the same
 * {@link Csv} output file. The link between these being a shared {@link CsvLines#modelFqn}.
 * <p>
 * Members of this class can be merged to produced a new object that will
 * have the combined output lines.
 */
class CsvLines {
  private final String modelFqn;
  private final List<CsvLine> lines;

  private CsvLines(String modelFqn, List<CsvLine> lines) {
    this.modelFqn = modelFqn;
    this.lines = lines;
  }

  /**
   * Factory of an empty instance to be used when reducing a stream of instances.
   */
  static CsvLines empty() {
    return new CsvLines(null, new ArrayList<>());
  }

  public static CsvLines of(String modelFqn, OffsetDateTime submissionDate, String line) {
    return new CsvLines(modelFqn, Collections.singletonList(new CsvLine(submissionDate, line)));
  }

  public static CsvLines of(String modelFqn, OffsetDateTime submissionDate, List<String> lines) {
    return new CsvLines(modelFqn, lines.stream().map(line -> new CsvLine(submissionDate, line)).collect(Collectors.toList()));
  }

  /**
   * Returns a new instance with the combined list of lines taken from both arguments.
   * <p>
   * The {@link CsvLines#modelFqn} of both arguments must match or be
   * null and at least one of them must be non-null.
   */
  static CsvLines merge(CsvLines left, CsvLines right) {
    // One of them must have a non-null modelFqn
    assert left.modelFqn != null || right.modelFqn != null;
    // modelFqns must match or be null
    assert left.modelFqn == null || right.modelFqn == null || left.modelFqn.equals(right.modelFqn);
    List<CsvLine> lines = new ArrayList<>();
    lines.addAll(left.lines);
    lines.addAll(right.lines);
    return new CsvLines(coalesce(left.modelFqn, right.modelFqn), lines);
  }

  /**
   * Utiliy method to get the first non-null value from a list
   */
  private static String coalesce(String... values) {
    return Stream.of(values).filter(Objects::nonNull).findFirst().orElseThrow(BriefcaseException::new);
  }

  String getModelFqn() {
    return modelFqn;
  }

  /**
   * Return the sorted stream of lines this instance holds
   */
  Stream<String> sorted() {
    return lines.stream().sorted(Comparator.comparing(CsvLine::getSubmissionDate)).map(CsvLine::getLine);
  }

  /**
   * Return the stream of lines this instance holds in any order
   */
  Stream<String> unsorted() {
    return lines.stream().map(CsvLine::getLine);
  }
}
