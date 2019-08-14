package org.opendatakit.briefcase.model.form;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileSystemFormMetadataAdapter implements FormMetadataPort {

  public static FormMetadataPort at(Path storageRoot) {
    return null;
  }

  @Override
  public <T> T query(Function<FormMetadataPort, T> query) {
    return null;
  }

  @Override
  public void execute(Consumer<FormMetadataPort> command) {

  }

  @Override
  public void syncWithFilesAt(Path storageRoot) {

  }

  @Override
  public void flush() {

  }
}
