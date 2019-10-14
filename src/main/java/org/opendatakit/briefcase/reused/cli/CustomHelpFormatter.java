package org.opendatakit.briefcase.reused.cli;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.opendatakit.briefcase.reused.cli.Cli.mapToOptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

class CustomHelpFormatter {
  private static String jarFile;

  static {
    try {
      jarFile = Optional.ofNullable(System.getProperty("java.class.path"))
          .filter(classpath -> !classpath.isEmpty())
          .map(Paths::get)
          .filter(Files::isRegularFile)
          .map(Object::toString)
          .map(filename -> {
            // The easiest way to make paths with spaces work both on
            // Windows & Unix is to wrap the path in double quotes
            return filename.contains(" ") ? '"' + filename + '"' : filename;
          })
          .orElse("briefcase.jar");
    } catch (Throwable t) {
      // We can't allow this code to break Briefcase execution.
      // In case of any problem, we'll just use the default "briefcase.jar" value
      jarFile = "briefcase.jar";
    }
  }

  static void printHelp(Set<Operation> operations) {
    printUsage();
    Map<String, String> helpLinesPerOperationName = getParamHelpLines(operations);
    printAvailableOperations(operations);
    printParamsPerOperation(helpLinesPerOperationName, operations);
  }

  private static void printParamsPerOperation(Map<String, String> helpLinesPerOperationName, Set<Operation> operations) {
    operations
        .stream()
        .sorted(comparing(Operation::getName))
        .forEach(operation -> {
          if (operation.hasAnyParam())
            System.out.println(operation.getName() + ":");
          if (operation.hasRequiredParams())
            printRequiredParams(helpLinesPerOperationName, operation);
          if (operation.hasOptionalParams())
            printOptionalParams(helpLinesPerOperationName, operation);
          if (operation.hasAnyParam())
            System.out.println();
        });
  }

  private static void printRequiredParams(Map<String, String> helpLinesPerOperationName, Operation operation) {
    operation.requiredParams.stream()
        .sorted(comparing(param -> param.shortCode))
        .forEach(param -> System.out.println("  " + helpLinesPerOperationName.get(param.shortCode)));
  }

  private static void printOptionalParams(Map<String, String> helpLinesPerOperationName, Operation operation) {
    System.out.println();
    System.out.println("  (optionally)");
    operation.optionalParams.stream()
        .sorted(comparing(param -> param.shortCode))
        .forEach(param -> System.out.println("  " + helpLinesPerOperationName.get(param.shortCode)));
  }

  private static void printAvailableOperations(Set<Operation> operations) {
    System.out.println("Available operations:");
    operations.stream()
        .filter(o -> !o.isDeprecated())
        .sorted(comparing(Operation::getName))
        .forEach(operation -> System.out.println(String.format(
            "  - %s: %s%s",
            operation.getName(),
            operation.getRequiredParams().stream().sorted(comparing(Param::getShortCode)).map(p -> "-" + p.shortCode).collect(joining(",")),
            operation.getOptionalParams().isEmpty()
                ? ""
                : " (" + operation.getOptionalParams().stream().sorted(comparing(Param::getShortCode)).map(p -> "-" + p.shortCode).collect(joining(",")) + ")"
        )));
    System.out.println();
  }

  private static void printUsage() {
    System.out.println();
    System.out.println("Launch the GUI with: java -jar " + jarFile);
    System.out.println("Launch a CLI operation with: java -jar " + jarFile + " <args & flags>");
    System.out.println();
  }

  private static Map<String, String> getParamHelpLines(Set<Operation> operations) {
    Set<Param> allParams = operations.stream()
        .flatMap(operation -> operation.getAllParams().stream())
        .collect(toSet());
    Options options = mapToOptions(allParams);

    StringWriter out = new StringWriter();
    PrintWriter pw = new PrintWriter(out);
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(pw, 999, "ignore", "ignore", options, 0, 4, "ignore");
    return Stream.of(out.toString().split("\n"))
        .filter(line -> line.startsWith("-") && line.contains(","))
        .collect(toMap(
            (String line) -> line.substring(1, line.indexOf(",")),
            Function.identity()
        ));
  }


}
