package org.opendatakit.common.cli;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.opendatakit.common.cli.CustomHelpFormatter.printHelp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opendatakit.briefcase.model.BriefcasePreferences;

public class Cli {
  private static final Param<Void> SHOW_HELP = Param.flag("h", "help", "Show help");
  private static final Param<Void> SHOW_VERSION = Param.flag("v", "version", "Show version");

  private final Set<Operation> requiredOperations = new HashSet<>();
  private final Set<Operation> operations = new HashSet<>();
  private final Set<Runnable> otherwiseRunnables = new HashSet<>();
  private final Set<Operation> executedOperations = new HashSet<>();

  public Cli() {
    register(Operation.of(SHOW_HELP, args -> printHelp(requiredOperations, operations)));
    register(Operation.of(SHOW_VERSION, args -> printVersion()));
  }

  public Cli always(Operation operation) {
    requiredOperations.add(operation);
    return this;
  }

  public Cli register(Operation operation) {
    operations.add(operation);
    return this;
  }

  public Cli otherwise(Runnable runnable) {
    otherwiseRunnables.add(runnable);
    return this;
  }

  public void run(String[] args) {
    Set<Param> allParams = getAllParams();
    CommandLine cli = getCli(args, allParams);

    requiredOperations.forEach(operation -> {
      checkForMissingParams(cli, operation.requiredParams);
      Map<Param, String> valuesMap = getValuesMap(cli, operation.requiredParams);
      operation.argsConsumer.accept(new Args(valuesMap));
    });

    operations.forEach(operation -> {
      if (cli.hasOption(operation.param.shortCode)) {
        checkForMissingParams(cli, operation.requiredParams);
        Map<Param, String> valuesMap = getValuesMap(cli, operation.getAllParams());
        operation.argsConsumer.accept(new Args(valuesMap));
        executedOperations.add(operation);
      }
    });

    if (executedOperations.isEmpty())
      otherwiseRunnables.forEach(Runnable::run);
  }

  private Set<Param> getAllParams() {
    return Stream.of(
        requiredOperations.stream().flatMap(operation -> operation.requiredParams.stream()),
        operations.stream().flatMap(operation -> operation.getAllParams().stream())
    ).flatMap(Function.identity()).collect(toSet());
  }

  private CommandLine getCli(String[] args, Set<Param> params) {
    try {
      return new DefaultParser().parse(mapToOptions(params), args, false);
    } catch (ParseException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void checkForMissingParams(CommandLine cli, Set<Param> paramsToCheck) {
    Set<Param> missingParams = paramsToCheck.stream().filter(param -> !cli.hasOption(param.shortCode)).collect(toSet());
    if (!missingParams.isEmpty()) {
      System.out.printf("Missing params: ");
      System.out.printf(missingParams.stream().map(param -> "-" + param.shortCode).collect(joining(", ")));
      System.out.println("");
      printHelp(requiredOperations, operations);
      System.exit(1);
    }
  }

  static Options mapToOptions(Set<Param> params) {
    Options options = new Options();
    params.forEach(param -> options.addOption(param.option));
    return options;
  }

  private Map<Param, String> getValuesMap(CommandLine cli, Set<Param> params) {
    Map<Param, String> valuesMap = new HashMap<>();
    // We can't collect toMap() because it will throw NPEs if values are null
    params.forEach(param -> {
      if (param.isArg())
        valuesMap.put(param, cli.getOptionValue(param.shortCode));
      if (param.isFlag() && cli.hasOption(param.shortCode))
        valuesMap.put(param, null);

    });
    return valuesMap;
  }

  private static void printVersion() {
    System.out.println("Briefcase " + BriefcasePreferences.VERSION);
  }
}
