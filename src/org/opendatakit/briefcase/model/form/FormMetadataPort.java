package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FormMetadataPort {

  <T> T query(Function<FormMetadataPort, T> query);

  void execute(Consumer<FormMetadataPort> command);

  void syncWithFilesAt(Path storageRoot);

  void flush();
}
