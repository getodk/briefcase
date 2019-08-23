package org.opendatakit.briefcase.model.form;

import static org.opendatakit.briefcase.reused.UncheckedFiles.walk;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.function.Consumer;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.LegacyPrefs;

public class FormMetadataCommands {
  public static Consumer<FormMetadataPort> upsert(FormKey key, Path formFile) {
    return port -> port.persist(port.fetch(key)
        .orElseGet(() -> FormMetadata.empty(key))
        .withFormFile(formFile));

  }

  public static Consumer<FormMetadataPort> upsert(FormKey key, Path formFile, Cursor cursor) {
    return port -> port.persist(port.fetch(key)
        .orElseGet(() -> FormMetadata.empty(key))
        .withFormFile(formFile)
        .withCursor(cursor));
  }

  public static Consumer<FormMetadataPort> updateLastExportedSubmission(FormMetadata formMetadata, OffsetDateTime submissionDate) {
    return port -> port.persist(port
        .fetch(formMetadata.getKey())
        .orElseThrow(BriefcaseException::new)
        .withLastExportedSubmissionDate(submissionDate));
  }

  public static Consumer<FormMetadataPort> cleanAllCursors() {
    return port -> port.persist(port.fetchAll().map(FormMetadata::withoutCursor));
  }

  public static Consumer<FormMetadataPort> syncWithFilesAt(Path workspaceLocation) {
    return port -> walk(workspaceLocation.resolve("forms"))
        // select XML files that are not submissions
        .filter(path -> !path.getFileName().toString().equals("submission.xml")
            && path.getFileName().toString().endsWith(".xml"))

        // select XML files that look like forms by parsing them
        // and looking for key parts that all forms must have
        .filter(path -> isAForm(XmlElement.from(path)))

        // Build a FormMetadata from the form file
        .map(FormMetadata::from)

        // Try to recover any missing cursor from the legacy Java prefs system
        .map(metadata -> {
          if (!metadata.getCursor().isEmpty())
            return metadata;
          return LegacyPrefs.readCursor(metadata.getKey().getId())
              .map(metadata::withCursor)
              .orElse(metadata);
        })

        // Write updated metadata.json files
        .forEach(port::persist);
  }

  private static boolean isAForm(XmlElement root) {
    return root.getName().equals("html")
        && root.findElements("head", "title").size() == 1
        && root.findElements("head", "model", "instance").size() >= 1
        && root.findElements("body").size() == 1;
  }
}
