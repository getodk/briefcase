package org.opendatakit.briefcase.ui.export;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendatakit.briefcase.model.form.FormMetadataPort;

public class InMemoryFormMetadataAdapter implements FormMetadataPort {

  @Override
  public void flush() {
  }

  @Override
  public void syncWithFilesAt(Path storageLocation) {
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
