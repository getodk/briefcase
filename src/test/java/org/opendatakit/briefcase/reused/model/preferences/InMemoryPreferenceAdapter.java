package org.opendatakit.briefcase.reused.model.preferences;

import java.util.function.Consumer;
import java.util.function.Function;

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
  public void persist(Preference preference) {

  }
}
