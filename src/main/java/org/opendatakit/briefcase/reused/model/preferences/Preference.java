package org.opendatakit.briefcase.reused.model.preferences;

public class Preference<T> {
  private final PreferenceKey<T> key;
  private final T value;

  public Preference(PreferenceKey<T> key, T value) {
    this.key = key;
    this.value = value;
  }

  static <U> Preference<U> of(PreferenceKey<U> key, U value) {
    return new Preference<>(key, value);
  }

  static <U> Preference<U> from(PreferenceKey<U> key, String serializedValue) {
    return new Preference<>(key, key.deserialize(serializedValue));
  }

  public PreferenceKey<T> getKey() {
    return key;
  }

  public T getValue() {
    return value;
  }

  public String serializeValue() {
    return key.serialize(value);
  }
}
