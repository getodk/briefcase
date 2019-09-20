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

  void persist(Stream<Preference<?>> preferenceStream);

  void remove(PreferenceKey<?> preference);

  default void remove(PreferenceKey<?>... preferences) {
    remove(Stream.of(preferences));
  }

  void remove(Stream<PreferenceKey<?>> preferenceStream);

  <T> Preference<T> fetch(PreferenceKey<T> key);

  <T> Optional<Preference<T>> fetchOptional(PreferenceKey<T> preference);

  Stream<Preference<?>> fetchAll();
}
