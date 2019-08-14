package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FormMetadataPort {

  <T> T query(Function<FormMetadataPort, T> query);

  void execute(Consumer<FormMetadataPort> command);

  FormMetadataPort syncWithFilesAt(Path storageRoot);

  void flush();

  void persist(FormMetadata formMetadata);

  Optional<FormMetadata> fetch(FormKey key);
}
