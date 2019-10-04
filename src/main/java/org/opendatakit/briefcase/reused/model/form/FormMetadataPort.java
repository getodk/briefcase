package org.opendatakit.briefcase.reused.model.form;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;

public interface FormMetadataPort {

  void flush();

  <T> T query(Function<FormMetadataPort, T> query);

  void execute(Consumer<FormMetadataPort> command);

  void persist(FormMetadata formMetadata);

  void persist(Stream<FormMetadata> formMetadata);

  Optional<FormMetadata> fetch(FormKey key);

  Stream<FormMetadata> fetchAll();

  void forgetPullSources();

  ExportConfiguration getExportConfiguration(FormKey formKey);

  Optional<OffsetDateTime> getLastExportDateTime(FormKey formKey);

  Optional<FormMetadata> fetchWithFormIdWithoutPullSource(String formId);
}
