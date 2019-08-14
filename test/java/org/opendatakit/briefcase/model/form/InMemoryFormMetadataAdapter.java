package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class InMemoryFormMetadataAdapter implements FormMetadataPort {
  private final Set<FormMetadata> store = new HashSet<>();

  @Override
  public void persist(FormMetadata metaData) {
    store.add(metaData);
  }

  @Override
  public Optional<FormMetadata> fetch(FormKey key) {
    return store.stream()
        .filter(metadata -> metadata.getKey().equals(key))
        .findFirst();
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
