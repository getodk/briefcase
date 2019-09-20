package org.opendatakit.briefcase.reused.model.preferences;

import static org.jooq.impl.DSL.deleteFrom;
import static org.jooq.impl.DSL.falseCondition;
import static org.jooq.impl.DSL.mergeInto;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.truncate;
import static org.jooq.impl.DSL.value;
import static org.opendatakit.briefcase.reused.db.jooq.Tables.PREFERENCE;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.opendatakit.briefcase.reused.db.BriefcaseDb;
import org.opendatakit.briefcase.reused.db.jooq.tables.records.PreferenceRecord;

public class DatabasePreferenceAdapter implements PreferencePort {
  private final Supplier<DSLContext> dslContextSupplier;
  private Map<String, DSLContext> dslContextCache = new ConcurrentHashMap<>();

  private DatabasePreferenceAdapter(Supplier<DSLContext> dslContextSupplier) {
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
    getDslContext().execute(truncate(PREFERENCE));
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
    // TODO Use generated records and let jOOQ do the work instead of explicitly using all the fields in the table, because this will break each time we change the table structure
    getDslContext().execute(mergeInto(PREFERENCE)
        .using(selectOne())
        .on(getMatchingCriteria(preference.getKey()))
        .whenMatchedThenUpdate()
        .set(PREFERENCE.VALUE, preference.serializeValue())
        // TODO deal with the new fields pull_source_type and pull_source_value
        .whenNotMatchedThenInsert(
            PREFERENCE.CATEGORY,
            PREFERENCE.NAME,
            PREFERENCE.VALUE
        )
        .values(
            value(preference.getKey().getCategory().getName()),
            value(preference.getKey().getName()),
            value(preference.serializeValue())
            // TODO deal with the new fields pull_source_type and pull_source_value
        )
    );
  }

  @Override
  public void persist(Stream<Preference<?>> preferences) {
    execute(port -> preferences.forEach(port::persist));
  }

  @Override
  public void remove(PreferenceKey<?> key) {
    getDslContext().execute(deleteFrom(PREFERENCE).where(getMatchingCriteria(key)));
  }

  @Override
  public void remove(Stream<PreferenceKey<?>> keys) {
    getDslContext().execute(deleteFrom(PREFERENCE)
        .where(keys.map(DatabasePreferenceAdapter::getMatchingCriteria).reduce(falseCondition(), Condition::or)));
  }

  @Override
  public <T> Preference<T> fetch(PreferenceKey<T> key) {
    PreferenceRecord record = getDslContext().fetchOne(selectFrom(PREFERENCE).where(getMatchingCriteria(key)));
    return mapToDomain(key, record);
  }

  @Override
  public <T> Optional<Preference<T>> fetchOptional(PreferenceKey<T> key) {
    Optional<PreferenceRecord> record = getDslContext().fetchOptional(selectFrom(PREFERENCE).where(getMatchingCriteria(key)));
    return record.map(r -> mapToDomain(key, r));
  }

  @Override
  public Stream<PreferenceKey<?>> fetchAllKeys() {
    return getDslContext().fetchStream(select(PREFERENCE.CATEGORY, PREFERENCE.NAME).from(PREFERENCE)).map(r -> new PreferenceKey<>(
        PreferenceCategory.from(r.component1()),
        r.component2(),
        Function.identity(),
        Function.identity()
    ));
  }

  private static Condition getMatchingCriteria(PreferenceKey<?> key) {
    return trueCondition()
        .and(PREFERENCE.CATEGORY.eq(key.getCategory().getName()))
        .and(PREFERENCE.NAME.eq(key.getName()));
  }


  private <T> Preference<T> mapToDomain(PreferenceKey<T> key, PreferenceRecord record) {
    return Preference.from(key, record.getValue());
  }
}
