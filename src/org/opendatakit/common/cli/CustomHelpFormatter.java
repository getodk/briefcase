package org.opendatakit.common.cli;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.opendatakit.common.cli.Cli.mapToOptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
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

  static void printHelp(Set<Operation> requiredOperations, Set<Operation> operations) {
    printUsage();
    Map<String, String> helpLinesPerShortcode = getParamHelpLines(requiredOperations, operations);
    if (!requiredOperations.isEmpty())
      printRequiredParams(helpLinesPerShortcode, requiredOperations);
    printAvailableOperations(helpLinesPerShortcode, operations);
    printParamsPerOperation(helpLinesPerShortcode, operations);
  }

  private static void printParamsPerOperation(Map<String, String> helpLinesPerShortcode, Set<Operation> operations) {
    operations.forEach(operation -> {
      if (operation.hasAnyParam())
        System.out.println("Params for -" + operation.param.shortCode + " operation:");
      if (operation.hasRequiredParams())
        printRequiredParams(helpLinesPerShortcode, operation);
      if (operation.hasOptionalParams())
        printOptionalParams(helpLinesPerShortcode, operation);
      if (operation.hasAnyParam())
        System.out.println("");
    });
  }

  private static void printRequiredParams(Map<String, String> helpLinesPerShortcode, Operation operation) {
    operation.requiredParams.stream()
        .sorted(Comparator.comparing(param -> param.shortCode))
        .forEach(param -> System.out.println("  " + helpLinesPerShortcode.get(param.shortCode)));
  }

  private static void printRequiredParams(Map<String, String> helpLinesPerShortcode, Set<Operation> requiredOperations) {
    System.out.println("Required params:");
    requiredOperations.stream()
        .flatMap(operation -> operation.requiredParams.stream())
        .sorted(Comparator.comparing(param -> param.shortCode))
        .forEach(param -> System.out.println(helpLinesPerShortcode.get(param.shortCode)));
    System.out.println("");
  }

  private static void printOptionalParams(Map<String, String> helpLinesPerShortcode, Operation operation) {
    System.out.println("Optional params for -" + operation.param.shortCode + " operation:");
    operation.optionalParams.stream()
        .sorted(Comparator.comparing(param -> param.shortCode))
        .forEach(param -> System.out.println("  " + helpLinesPerShortcode.get(param.shortCode)));
  }

  private static void printAvailableOperations(Map<String, String> helpLinesPerShortcode, Set<Operation> operations) {
    System.out.println("Available operations:");
    operations.stream()
        .sorted(Comparator.comparing(operation -> operation.param.shortCode))
        .forEach(operation -> System.out.println(helpLinesPerShortcode.get(operation.param.shortCode)));
    System.out.println("");
  }

  private static void printUsage() {
    System.out.println("");
    System.out.println("Launch the GUI with: java -jar " + jarFile);
    System.out.println("Launch a CLI operation with: java -jar " + jarFile + " <operation> <params>");
    System.out.println("");
  }

  private static Map<String, String> getParamHelpLines(Set<Operation> requiredOperations, Set<Operation> operations) {
    Set<Param> allParams = Stream.of(
        requiredOperations.stream().flatMap(operation -> operation.requiredParams.stream()),
        operations.stream().flatMap(operation -> operation.getAllParams().stream())
    ).flatMap(Function.identity()).collect(toSet());
    Options options = mapToOptions(allParams);

    StringWriter out = new StringWriter();
    PrintWriter pw = new PrintWriter(out);
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(pw, 999, "ignore", "ignore", options, 0, 4, "ignore");
    return Stream.of(out.toString().split("\n"))
        .filter(line -> line.startsWith("-"))
        .collect(toMap(
            (String line) -> line.substring(1, line.indexOf(",")),
            Function.identity()
        ));
  }


}
