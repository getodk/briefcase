package org.opendatakit.briefcase.ui.reused;

import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isODKInstancesParentFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import java.awt.Container;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

public interface Source<T> {
  Source<AggregateServerConnectionConfiguration> AGGREGATE = new Source<AggregateServerConnectionConfiguration>() {
    @Override
    public void onSelect(Container container, Consumer<AggregateServerConnectionConfiguration> callback) {
      AggregateServerConnectionDialog dialog = AggregateServerConnectionDialog.empty(__ -> true);
      dialog.onConnect(callback);
      dialog.getForm().setVisible(true);
    }

    @Override
    public String toString() {
      return "ODK Aggregate Server";
    }
  };

  Source<Path> CUSTOM_DIR = new Source<Path>() {
    @Override
    public void onSelect(Container container, Consumer<Path> callback) {
      FileChooser fc = FileChooser.directory(container,
          Optional.empty(),
          f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f) && isODKInstancesParentFolder(f),
          "Check for valid ODK Directories");
      fc.choose().ifPresent(file -> callback.accept(Paths.get(file.getAbsolutePath())));
    }

    @Override
    public String toString() {
      return "Custom ODK Directory";
    }
  };

  void onSelect(Container container, Consumer<T> callback);
}

