package org.opendatakit.briefcase.reused.model.preferences;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class InMemoryPreferenceAdapter implements PreferencePort {

  @Override
  public void flush() {

  }

  @Override
  public <T> T query(Function<PreferencePort, T> query) {
    return null;
  }

  @Override
  public void execute(Consumer<PreferencePort> command) {

  }

  @Override
  public void persist(Preference<?> preference) {

  }

  @Override
  public void persist(Stream<Preference<?>> preferenceStream) {

  }

  @Override
  public void remove(PreferenceKey<?> preference) {

  }

  @Override
  public void remove(Stream<PreferenceKey<?>> preferenceStream) {

  }

  @Override
  public <T> Preference<T> fetch(PreferenceKey<T> key) {
    return null;
  }

  @Override
  public <T> Optional<Preference<T>> fetchOptional(PreferenceKey<T> preference) {
    return Optional.empty();
  }

  @Override
  public Stream<Preference<?>> fetchAll() {
    return null;
  }
}
