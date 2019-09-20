package org.opendatakit.briefcase.reused.model.preferences;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;

public class DatabasePreferenceAdapter implements PreferencePort {
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  public DatabasePreferenceAdapter(Supplier<DSLContext> dslContextSupplier) {
    this.dslContextSupplier = dslContextSupplier;
  }

  public static PreferencePort from(BriefcaseDb db) {
    return new DatabasePreferenceAdapter(db::getDslContext);
  }

  private DSLContext getDslContext() {
    return dslContextCache.computeIfAbsent("default", __ -> dslContextSupplier.get());
  }

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
