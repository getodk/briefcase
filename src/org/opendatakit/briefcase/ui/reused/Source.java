package org.opendatakit.briefcase.ui.reused;

import static org.opendatakit.briefcase.ui.StorageLocation.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isODKInstancesParentFolder;
import static org.opendatakit.briefcase.util.FileSystemUtils.isUnderODKFolder;

import java.awt.Container;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Source<T> {

  private final String label;
  private final BiConsumer<Container, Consumer<T>> onSelectCallback;

  public Source(String label, BiConsumer<Container, Consumer<T>> onSelectCallback) {
    this.label = label;
    this.onSelectCallback = onSelectCallback;
  }

  public static final Source<AggregateServerConnectionConfiguration> AGGREGATE = new Source<>("ODK Aggregate Server",
      (container, callback) -> {
        AggregateServerConnectionDialog dialog = AggregateServerConnectionDialog.empty(__ -> true);
        dialog.onConnect(callback);
        dialog.getForm().setVisible(true);
      });

  public static final Source<Path> CUSTOM_DIR = new Source<>("Custom ODK Directory",
      (container, callback) -> {
        FileChooser fc = FileChooser.directory(container,
            Optional.empty(),
            f -> f.exists() && f.isDirectory() && !isUnderBriefcaseFolder(f) && !isUnderODKFolder(f) && isODKInstancesParentFolder(f),
            "Check for valid ODK Directories");
        fc.choose().ifPresent(file -> callback.accept(Paths.get(file.getAbsolutePath())));
      });

  void onSelect(Container container, Consumer<T> callback) {
    onSelectCallback.accept(container, callback);
  }

  @Override
  public String toString() {
    return label;
  }
}

