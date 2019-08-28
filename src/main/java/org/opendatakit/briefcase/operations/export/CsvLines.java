package org.opendatakit.briefcase.operations.export;

import static java.util.Comparator.comparing;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class represents a group of CSV lines belonging to the same
 * CSV output file. The link between these being a shared field FQN.
 */
class CsvLines {
  private final String modelFqn;
  private final List<CsvLine> lines;
  private final Optional<CsvLine> lastLine;

  CsvLines(String modelFqn, List<CsvLine> lines, Optional<CsvLine> lastLine) {
    this.modelFqn = modelFqn;
    this.lines = lines;
    this.lastLine = lastLine;
  }

  static CsvLines empty() {
    return new CsvLines(null, new ArrayList<>(), Optional.empty());
  }

  public static CsvLines of(String modelFqn, String instanceId, OffsetDateTime submissionDate, String line) {
    CsvLine csvLine = new CsvLine(instanceId, submissionDate, line);
    return new CsvLines(modelFqn, Collections.singletonList(csvLine), Optional.of(csvLine));
  }

  public static CsvLines of(String modelFqn, String instanceId, OffsetDateTime submissionDate, List<String> lines) {
    List<CsvLine> csvLines = lines.stream().map(line -> new CsvLine(instanceId, submissionDate, line)).collect(Collectors.toList());
    return new CsvLines(modelFqn, csvLines, csvLines.isEmpty() ? Optional.empty() : Optional.of(csvLines.get(csvLines.size() - 1)));
  }

  static CsvLines merge(CsvLines left, CsvLines right) {
    // One of them must have a non-null modelFqn
    if (left.modelFqn == null && right.modelFqn == null)
      throw new BriefcaseException("At least one CsvLines arg has to have a non-null FQDN");
    // modelFqns must match or be null
    if (left.modelFqn != null && right.modelFqn != null && !left.modelFqn.equals(right.modelFqn))
      throw new BriefcaseException("FQDN don't match");
    List<CsvLine> csvLines = new ArrayList<>();
    csvLines.addAll(left.lines);
    csvLines.addAll(right.lines);
    List<CsvLine> lastCsvLines = new ArrayList<>();
    left.lastLine.ifPresent(lastCsvLines::add);
    right.lastLine.ifPresent(lastCsvLines::add);
    Optional<CsvLine> lastLine = lastCsvLines.stream().max(comparing(CsvLine::getSubmissionDate));
    return new CsvLines(coalesce(left.modelFqn, right.modelFqn), csvLines, lastLine);
  }

  private static String coalesce(String... values) {
    return Stream.of(values).filter(Objects::nonNull).findFirst().orElseThrow(BriefcaseException::new);
  }

  String getModelFqn() {
    return modelFqn;
  }

  Optional<CsvLine> getLastLine() {
    return lastLine;
  }

  Stream<String> sorted() {
    return lines.stream().sorted(comparing(CsvLine::getSubmissionDate)).map(CsvLine::getLine);
  }

  Stream<String> unsorted() {
    return lines.stream().map(CsvLine::getLine);
  }
}
