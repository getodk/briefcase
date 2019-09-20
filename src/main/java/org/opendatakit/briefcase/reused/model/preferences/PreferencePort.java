package org.opendatakit.briefcase.reused.model.preferences;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface PreferencePort {
  void flush();

  <T> T query(Function<PreferencePort, T> query);

  void execute(Consumer<PreferencePort> command);

  void persist(Preference<?> preference);

  default void persist(Preference<?>... preferences) {
    persist(Stream.of(preferences));
  }

  void persist(Stream<Preference<?>> preferences);

  void remove(PreferenceKey<?> key);

  default void remove(PreferenceKey<?>... preferences) {
    remove(Stream.of(preferences));
  }

  void remove(Stream<PreferenceKey<?>> keys);

  <T> Preference<T> fetch(PreferenceKey<T> key);

  <T> Optional<Preference<T>> fetchOptional(PreferenceKey<T> key);

  Stream<PreferenceKey<?>> fetchAllKeys();
}
