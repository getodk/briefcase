package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class InMemoryFormMetadataAdapter implements FormMetadataPort {
  private final Map<FormKey, FormMetadata> store = new HashMap<>();

  @Override
  public void persist(FormMetadata metaData) {
    store.put(metaData.getKey(), metaData);
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return Optional.ofNullable(store.get(key));
  }

  @Override
  public void flush() {
    store.clear();
  }

  @Override
  public FormMetadataPort syncWithFilesAt(Path storageLocation) {
    return this;
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return query.apply(this);
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {
    command.accept(this);
  }
}
