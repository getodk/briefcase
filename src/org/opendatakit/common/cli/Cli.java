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
package org.opendatakit.common.cli;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.opendatakit.common.cli.CustomHelpFormatter.printHelp;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcasePreferences;

/**
 * <p>Cli is a command line adapter. It helps define executable operations and their
 * required and optional params.
 * <p>It defines some default operations like "show help" and "show version"
 */
public class Cli {
  private static final Log log = LogFactory.getLog(Cli.class);
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

  /**
   * Register an {@link Operation}
   *
   * @param operation an {@link Operation} instance
   * @return self {@link Cli} instance to chain more method calls
   */
  public Cli register(Operation operation) {
    operations.add(operation);
    return this;
  }

  /**
   * Register a {@link Runnable} block that will be executed if no {@link Operation}
   * is executed. For example, if the user passes no arguments when executing this program
   *
   * @param runnable a {@link Runnable} block
   * @return self {@link Cli} instance to chain more method calls
   */
  public Cli otherwise(Runnable runnable) {
    otherwiseRunnables.add(runnable);
    return this;
  }

  /**
   * Runs the command line program
   *
   * @param args command line arguments
   * @see <a href="https://blog.idrsolutions.com/2015/03/java-8-consumer-supplier-explained-in-5-minutes/">Java 8 consumer supplier explained in 5 minutes</a>
   */
  public void run(String[] args) {
    Set<Param> allParams = getAllParams();
    CommandLine cli = getCli(args, allParams);
    try {
      requiredOperations.forEach(operation -> {
        checkForMissingParams(cli, operation.requiredParams);
        operation.argsConsumer.accept(Args.from(cli, operation.requiredParams));
      });

      operations.forEach(operation -> {
        if (cli.hasOption(operation.param.shortCode)) {
          checkForMissingParams(cli, operation.requiredParams);
          operation.argsConsumer.accept(Args.from(cli, operation.getAllParams()));
          executedOperations.add(operation);
        }
      });

      if (executedOperations.isEmpty())
        otherwiseRunnables.forEach(Runnable::run);
    } catch (Throwable t) {
      System.err.println("Briefcase unexpected error. Please review the logs and contact maintainers on the following URLs:");
      System.err.println("\thttps://opendatakit.slack.com/messages/C374LNDK9/");
      System.err.println("\thttps://forum.opendatakit.org/c/support");
      log.error("Unexpected error", t);
      System.exit(1);
    }
  }

  /**
   * Flatmap all required params from all required operations, all params
   * from all operations and flatmap them into a {@link Set}&lt;{@link Param}>&gt;
   *
   * @return a {@link Set} of {@link Param}> instances
   * @see <a href="https://www.mkyong.com/java8/java-8-flatmap-example/">Java 8 flatmap example</a>
   */
  private Set<Param> getAllParams() {
    return Stream.of(
        requiredOperations.stream().flatMap(operation -> operation.requiredParams.stream()),
        operations.stream().flatMap(operation -> operation.getAllParams().stream())
    ).flatMap(Function.identity()).collect(toSet());
  }

  private CommandLine getCli(String[] args, Set<Param> params) {
    try {
      return new DefaultParser().parse(mapToOptions(params), args, false);
    } catch (UnrecognizedOptionException e) {
      System.err.println(e.getMessage());
      printHelp(requiredOperations, operations);
      System.exit(1);
      return null;
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
      return null;
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

  private static void printVersion() {
    System.out.println("Briefcase " + BriefcasePreferences.VERSION);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Cli cli = (Cli) o;
    return Objects.equals(requiredOperations, cli.requiredOperations) &&
        Objects.equals(operations, cli.operations) &&
        Objects.equals(otherwiseRunnables, cli.otherwiseRunnables) &&
        Objects.equals(executedOperations, cli.executedOperations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiredOperations, operations, otherwiseRunnables, executedOperations);
  }

  @Override
  public String toString() {
    return "Cli{" +
        "requiredOperations=" + requiredOperations +
        ", operations=" + operations +
        ", otherwiseRunnables=" + otherwiseRunnables +
        ", executedOperations=" + executedOperations +
        '}';
  }
}
