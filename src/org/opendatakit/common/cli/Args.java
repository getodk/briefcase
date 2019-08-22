package org.opendatakit.common.cli;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.cli.CommandLine;

/**
 * This class serves as a container of values parsed from the command line.
 * <p>It holds the values inside a <code>{@link Map}&lt;{@link Param}, String&gt;</code> and offers a
 * type-safe, null-safe API to get them thanks to the use of generics and the
 * {@link Optional} type.
 */
public class Args {
  private final Map<Param, String> valuesMap;

  /**
   * Private constructor.
   * <p>Users of this class should call the {@link #from(CommandLine, Set)}
   * factory instead.
   *
   * @param valuesMap the <code>{@link java.util.Map}&lt;{@link Param}, String&gt;</code> with the params and values
   *                  that this instance will hold
   */
  private Args(Map<Param, String> valuesMap) {
    this.valuesMap = valuesMap;
  }


  public static Args from(CommandLine cli, Param... params) {
    return Args.from(cli, new HashSet<>(Arrays.asList(params)));
  }

  /**
   * <p>Factory that extracts a set of values for the given params set from a
   * {@link CommandLine} instance
   * <ul>
   * <li>For argument params, it extracts their value</li>
   * <li>For flag params, it uses a <code>null</code> value</li>
   * </ul>
   *
   * @param cli    A {@link CommandLine} instance
   * @param params A <code>{@link Set}&lt;{@link Param}&gt;</code> with the params to extract using <code>cli</code>
   * @return An {@link Args} instance
   */
  static Args from(CommandLine cli, Set<Param> params) {
    Map<Param, String> valuesMap = new HashMap<>();
    // We can't collect toMap() because it will throw NPEs if values are null
    params.forEach(param -> {
      if (param.isArg())
        valuesMap.put(param, cli.getOptionValue(param.shortCode));
      if (param.isFlag() && cli.hasOption(param.shortCode))
        valuesMap.put(param, null);

    });
    return new Args(valuesMap);
  }

  /**
   * <p>Returns true if the <code>key</code> is contained in the <code>valuesMap</code>
   * <p>Useful to check for presence of flag params
   *
   * @param key the {@link Param} instance to be searched
   * @return true if <code>key</code> is contained in the <code>valuesMap</code>
   */
  public boolean has(Param<?> key) {
    return valuesMap.containsKey(key);
  }

  /**
   * <p>Returns an {@link Optional} instance with the value of the given <code>key</code>
   *
   * @param key the {@link Param} key of the value to be retrieved
   * @param <T> type of the value to be retrieved
   * @return an {@link Optional} instance with the value of the given <code>key</code>
   */
  public <T> Optional<T> getOptional(Param<T> key) {
    return Optional.ofNullable(valuesMap.get(key))
        .map(key::map);
  }

  /**
   * <p>Returns the value contained in the given <code>key</code>
   * <p>Throws an {@link IllegalArgumentException} if the key is not contained,
   * or if the <code>key</code> corresponds to a flag param
   *
   * @param key the {@link Param} key of the value to be retrieved
   * @param <T> type of the value to be retrieved
   * @return the value contained in the given <code>key</code>
   */
  public <T> T get(Param<T> key) {
    return getOptional(key)
        .orElseThrow(() -> new IllegalArgumentException("No param -" + key.shortCode + " has been declared"));
  }

  /**
   * <p>Shortcut to <code>getOptional(key).orElse(null)</code> to avoid handling explicit
   * null values
   *
   * @param key the {@link Param} key of the value to be retrieved
   * @param <T> type of the value to be retrieved
   * @return the value contained in the given <code>key</code> or <code>null</code> if the key is not contained
   */
  public <T> T getOrNull(Param<T> key) {
    return getOptional(key).orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Args args = (Args) o;
    return Objects.equals(valuesMap, args.valuesMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valuesMap);
  }

  @Override
  public String toString() {
    return "Args{" +
        "valuesMap=" + valuesMap +
        '}';
  }
}
