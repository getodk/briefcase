package org.opendatakit.briefcase.reused.model.preferences;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class InMemoryPreferenceAdapter implements PreferencePort {
  private final Map<PreferenceKey<?>, Preference> store = new ConcurrentHashMap<>();

  @Override
  public void flush() {
    store.clear();
  }

  @Override
  public <T> T query(Function<PreferencePort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<PreferencePort> command) {
    command.accept(this);
  }

  @Override
  public void persist(Preference<?> preference) {
    store.put(preference.getKey(), preference);
  }

  @Override
  public void persist(Stream<Preference<?>> preferences) {
    preferences.forEach(this::persist);
  }

  @Override
  public void remove(PreferenceKey<?> key) {
    store.remove(key);
  }

  @Override
  public void remove(Stream<PreferenceKey<?>> keys) {
    keys.forEach(this::remove);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Preference<T> fetch(PreferenceKey<T> key) {
    return store.get(key);
  }

  @Override
  public <T> Optional<Preference<T>> fetchOptional(PreferenceKey<T> key) {
    return Optional.ofNullable(fetch(key));
  }

  @Override
  public Stream<PreferenceKey<?>> fetchAllKeys() {
    return store.keySet().stream();
  }
}
