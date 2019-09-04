package org.opendatakit.briefcase;

import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.toURI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Test;
import org.opendatakit.briefcase.export.DateRange;
import org.opendatakit.briefcase.export.ExportConfiguration;
import org.opendatakit.briefcase.export.ExportToCsv;
import org.opendatakit.briefcase.export.FormDefinition;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.form.FormMetadata;
import org.opendatakit.briefcase.model.form.InMemoryFormMetadataAdapter;
import org.opendatakit.briefcase.pull.aggregate.PullFromAggregateIntegrationTest;
import org.opendatakit.briefcase.reused.OverridableBoolean;
import org.opendatakit.briefcase.util.BadFormDefinition;

public class CustomFunctionTest {
  private static Path getPath(String fileName) {
    return Optional.ofNullable(PullFromAggregateIntegrationTest.class.getClassLoader().getResource("org/opendatakit/briefcase/" + fileName))
        .map(url -> Paths.get(toURI(url)))
        .orElseThrow(RuntimeException::new);
  }

  @Test
  public void name() {
    FormDefinition.from(getPath("custom-function-form.xml"));
  }

  @Test
  public void full_regression_test() throws IOException, BadFormDefinition {
    Path exportDir = createTempDirectory("briefcase-test-export-dir-");
    Path sourceFormFile = getPath("custom-function-form.xml");
    Path briefcaseDir = createTempDirectory("briefcase-test-storage-dir-");
    Path formFile = briefcaseDir.resolve("Custom function form").resolve("Custom function form.xml");
    createDirectories(formFile.getParent());
    Files.copy(sourceFormFile, formFile);

    FormMetadata formMetadata = FormMetadata.from(formFile);
    InMemoryFormMetadataAdapter formMetadataPort = new InMemoryFormMetadataAdapter();
    formMetadataPort.persist(formMetadata);
    BriefcaseFormDefinition localFormDef = BriefcaseFormDefinition.resolveAgainstBriefcaseDefn(formFile.toFile(), false, briefcaseDir.toFile());
    FormStatus formStatus = new FormStatus(localFormDef);

    ExportConfiguration configuration = new ExportConfiguration(
        Optional.<String>empty(),
        Optional.of(exportDir),
        Optional.<Path>empty(),
        DateRange.empty(),
        OverridableBoolean.FALSE,
        OverridableBoolean.TRUE,
        OverridableBoolean.TRUE,
        OverridableBoolean.FALSE,
        OverridableBoolean.FALSE,
        OverridableBoolean.FALSE,
        OverridableBoolean.FALSE
    );
    ExportToCsv.export(formMetadataPort, formMetadata, formStatus, FormDefinition.from(localFormDef), briefcaseDir, configuration);
  }
}
