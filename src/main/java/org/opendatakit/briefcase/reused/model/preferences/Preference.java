package org.opendatakit.briefcase.reused.model.preferences;

public class Preference<T> {
  private final PreferenceKey<T> key;
  private final String value;

  public Preference(PreferenceKey<T> key, String value) {
    this.key = key;
    this.value = value;
  }

  static <U> Preference<U> of(PreferenceKey<U> key, U value) {
    return new Preference<>(key, key.serialize(value));
  }

  public PreferenceKey<T> getKey() {
    return key;
  }

  public T getValue() {
    return key.deserialize(value);
  }
}
