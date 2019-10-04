package org.opendatakit.briefcase.delivery;

import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;
import org.opendatakit.briefcase.Launcher;
import org.opendatakit.briefcase.reused.api.Pair;

public class LegacyPrefs {
  public static void main(String[] args) throws BackingStoreException {
    extract().forEach(p -> System.out.println(p.getLeft() + " = \"" + p.getRight() + "\""));
  }

  private static Stream<Pair<LegacyPreferenceKey, String>> extract() throws BackingStoreException {
    Stream<Pair<LegacyPreferenceKey, String>> appScopedPrefs = extract(
        Preferences.userNodeForPackage(Launcher.class),
        "/org/opendatakit/briefcase"
    );

    Preferences userRootNode = Preferences.userRoot();
    Stream<Pair<LegacyPreferenceKey, String>> classScopedPrefs = Stream
        .of(userRootNode.node("").childrenNames())
        // Do no evil, and iterate just what we need
        .filter(nodeName -> nodeName.startsWith("org.opendatakit"))
        .flatMap(nodeName -> extract(userRootNode.node("/" + nodeName), "/" + nodeName));

    return Stream.of(appScopedPrefs, classScopedPrefs).flatMap(identity());
  }

  private static Stream<Pair<LegacyPreferenceKey, String>> extract(Preferences node, String currentName) {
    try {
      Stream<Pair<LegacyPreferenceKey, String>> prefs = Stream
          .of(node.keys())
          .map(keyName -> Pair.of(
              new LegacyPreferenceKey(currentName, keyName),
              Optional.ofNullable(node.get(keyName, null)).filter(not(String::isBlank))
          ))
          .filter(pair -> pair.getRight().isPresent())
          .map(pair -> pair.map(identity(), Optional::get));

      Stream<Pair<LegacyPreferenceKey, String>> childrenPrefs = Stream
          .of(node.childrenNames())
          .map(nodeName -> extract(node.node(nodeName), currentName + "/" + nodeName))
          .flatMap(identity());

      return Stream.of(
          prefs,
          childrenPrefs
      ).flatMap(identity());
    } catch (BackingStoreException e) {
      throw new RuntimeException(e);
    }
  }

}

