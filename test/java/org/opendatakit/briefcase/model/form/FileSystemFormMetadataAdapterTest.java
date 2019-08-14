package org.opendatakit.briefcase.model.form;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAnd;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.PathMatchers.exists;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createDirectories;
import static org.opendatakit.briefcase.reused.UncheckedFiles.createTempDirectory;
import static org.opendatakit.briefcase.reused.UncheckedFiles.deleteRecursive;
import static org.opendatakit.briefcase.reused.UncheckedFiles.newInputStream;
import static org.opendatakit.briefcase.reused.UncheckedFiles.write;
import static org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.opendatakit.briefcase.export.XmlElement;
import org.opendatakit.briefcase.pull.aggregate.AggregateCursor;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.pull.aggregate.OnaCursor;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Pair;
import org.xmlpull.v1.XmlPullParserException;

public class FileSystemFormMetadataAdapterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private Path storageRoot;

  @Before
  public void setUp() {
    storageRoot = createTempDirectory("briefcase");
    createDirectories(storageRoot.resolve("forms"));
  }

  @After
  public void tearDown() {
    deleteRecursive(storageRoot);
  }

  @Test
  public void persists_metadata_by_creating_a_metadata_json_file_along_with_the_form_file() throws IOException {
    FormMetadataPort port = FileSystemFormMetadataAdapter.at(storageRoot);
    Path installedForm = installForm(storageRoot, "Some form", "some-form", Optional.empty());
    FormMetadata persistedMetadata = buildMetadataFrom(installedForm, Cursor.empty());
    port.persist(persistedMetadata);

    Path metadataFile = persistedMetadata.getStorageDirectory().resolve("metadata.json");
    assertThat(metadataFile, exists());
    // Sanity check of the json file's structure and data.
    // Further checks are make implicitly (using object equality) in other tests
    JsonNode root = MAPPER.readTree(metadataFile.toFile());
    assertThat(root.path("key").path("name").asText(), is("Some form"));
    assertThat(root.path("key").path("id").asText(), is("some-form"));
    assertThat(root.path("key").path("version").isNull(), is(true));
    assertThat(root.path("storageDirectory").asText(), startsWith(storageRoot.resolve("forms").toString()));
    assertThat(root.path("hasBeenPulled").asBoolean(), is(true));
    assertThat(root.path("cursor").path("type").asText(), is("empty"));
    assertThat(root.path("cursor").path("value").isNull(), is(true));
  }

  @Test
  public void persists_and_fetches_form_metadata() {
    // The filesystem-based adapter we're testing uses an in-memory cache of metadata
    // to avoid hitting the filesystem each time we need to fetch one.
    // In this tests, we persist and fetch a range of possible metadata objects with
    // all combinations of version and lastCursor, and we use different adapter instances
    // for persisting and fetching them to work around the in-memory cache.
    Arrays.<Pair<Supplier<Path>, Cursor>>asList(
        Pair.of(() -> installForm(storageRoot, "Form 1", "form-1", Optional.empty()), Cursor.empty()),
        Pair.of(() -> installForm(storageRoot, "Form 2", "form-2", Optional.of("20190701002")), Cursor.empty()),
        Pair.of(() -> installForm(storageRoot, "Form 3", "form-3", Optional.of("20190701003")), AggregateCursor.of(OffsetDateTime.now(), "uuid:" + UUID.randomUUID().toString())),
        Pair.of(() -> installForm(storageRoot, "Form 4", "form-4", Optional.of("20190701004")), OnaCursor.from("1234")),
        Pair.of(() -> installForm(storageRoot, "Form 5", "form-5", Optional.empty()), AggregateCursor.of(OffsetDateTime.now(), "uuid:" + UUID.randomUUID().toString())),
        Pair.of(() -> installForm(storageRoot, "Form 6", "form-6", Optional.empty()), OnaCursor.from("1234"))
    ).forEach(pair -> {
      // Clean the storage root to isolate side-effects of each iteration
      deleteRecursive(storageRoot);
      createDirectories(storageRoot.resolve("forms"));

      // Get the scenario settings of this test
      Supplier<Path> formInstaller = pair.getLeft();
      Cursor cursor = pair.getRight();

      // Install the form in the storage root, and get a metadata object from it
      Path formFile = formInstaller.get();
      FormMetadata metadataToBePersisted = buildMetadataFrom(formFile, cursor);

      // Persist metadata into the filesystem
      FormMetadataPort persistPort = FileSystemFormMetadataAdapter.at(storageRoot);
      persistPort.persist(metadataToBePersisted);

      // Can't use the same adapter because it would return the object stored
      // in its in-memory cache, and we want to force the adapter to go search for
      // it in the filesystem.
      FormMetadataPort fetchPort = FileSystemFormMetadataAdapter.at(storageRoot);
      Optional<FormMetadata> fetchedMetadata = fetchPort.fetch(metadataToBePersisted.getKey());
      assertThat(fetchedMetadata, isPresentAnd(is(metadataToBePersisted)));
    });
  }

  private Path installForm(Path storageRoot, String name, String id, Optional<String> version) {
    String form = String.format("" +
        "<?xml version=\"1.0\"?>\n" +
        "<h:html xmlns=\"http://www.w3.org/2002/xforms\" xmlns:h=\"http://www.w3.org/1999/xhtml\">\n" +
        "  <h:head>\n" +
        "    <h:title>" + name + "</h:title>\n" +
        "    <model>\n" +
        "      <instance>\n" +
        "        <data id=\"%s\"%s>\n" +
        "          <some-field/>\n" +
        "          <meta>\n" +
        "            <instanceID/>\n" +
        "          </meta>\n" +
        "        </data>\n" +
        "      </instance>\n" +
        "      <bind nodeset=\"/data/some-field\" type=\"text\"/>\n" +
        "      <bind calculate=\"concat('uuid:', uuid())\" nodeset=\"/data/meta/instanceID\" readonly=\"true()\" type=\"string\"/>\n" +
        "    </model>\n" +
        "  </h:head>\n" +
        "  <h:body>\n" +
        "    <input ref=\"/data/some-field\"/>\n" +
        "  </h:body>\n" +
        "</h:html>", id, version.map(v -> " version =\"" + v + "\"").orElse(""));
    Path formStorageDir = storageRoot.resolve("forms").resolve(name);
    Path formFile = formStorageDir.resolve(name + ".xml");
    createDirectories(formStorageDir);
    write(formFile, form);
    return formFile;
  }

  private static XmlElement readXml(InputStream in) {
    try (InputStreamReader ir = new InputStreamReader(in)) {
      Document doc = new Document();
      KXmlParser parser = new KXmlParser();
      parser.setInput(ir);
      parser.setFeature(FEATURE_PROCESS_NAMESPACES, true);
      doc.parse(parser);
      return XmlElement.of(doc);
    } catch (XmlPullParserException | IOException e) {
      throw new BriefcaseException(e);
    }
  }

  private static FormMetadata buildMetadataFrom(Path formFile, Cursor cursor) {
    XmlElement root = readXml(newInputStream(formFile));
    XmlElement mainInstance = root.findElements("head", "model", "instance").stream()
        .filter(e -> !e.hasAttribute("id") // It's not a secondary instance
            && e.childrenOf().size() == 1 // Just one child (sanity check: if there's a different number of children, we don't really know what's this element)
            && e.childrenOf().get(0).hasAttribute("id") // The child element has an id (sanity check: we can't build form metadata without the form's id)
        )
        .findFirst().orElseThrow(RuntimeException::new)
        .childrenOf().get(0);
    FormKey key = FormKey.of(
        root.findElements("head", "title").get(0).getValue(),
        mainInstance.getAttributeValue("id").orElseThrow(RuntimeException::new),
        mainInstance.getAttributeValue("version")
    );
    return new FormMetadata(key, formFile.getParent(), cursor.isEmpty(), cursor);
  }
}
