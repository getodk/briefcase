package org.opendatakit.briefcase.delivery;

import static java.lang.Integer.parseInt;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.opendatakit.briefcase.reused.http.RequestBuilder.url;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setDefaultExportConfiguration;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.opendatakit.briefcase.Launcher;
import org.opendatakit.briefcase.operations.export.ExportConfiguration;
import org.opendatakit.briefcase.operations.transfer.SourceOrTarget;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.api.Optionals;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.http.Credentials;
import org.opendatakit.briefcase.reused.model.DateRange;
import org.opendatakit.briefcase.reused.model.OverridableBoolean;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands;
import org.opendatakit.briefcase.reused.model.transfer.AggregateServer;
import org.opendatakit.briefcase.reused.model.transfer.CentralServer;

public class LegacyPrefs {
  public static final Pattern FORM_NAME_EXTRACTOR = Pattern.compile("^(.+?)_pull_source_.+$");
  private final Map<LegacyPreferenceKey, String> prefs;

  public LegacyPrefs(Map<LegacyPreferenceKey, String> prefs) {
    this.prefs = prefs;
  }

  public static LegacyPrefs read() {
    try {
      return new LegacyPrefs(extract().collect(toMap(Pair::getLeft, Pair::getRight)));
    } catch (BackingStoreException e) {
      throw new BriefcaseException("Can't extract legacy preferences", e);
    }
  }

  private static Stream<Pair<LegacyPreferenceKey, String>> extract() throws BackingStoreException {
    Stream<Pair<LegacyPreferenceKey, String>> appScopedPrefs = extract(
        Preferences.userNodeForPackage(Launcher.class),
        "/org/opendatakit/briefcase"
    );

    Preferences userRootNode = Preferences.userRoot();
    Stream<Pair<LegacyPreferenceKey, String>> classScopedPrefs = Stream
        .of(userRootNode.node("").childrenNames())
        // Do no evil, and iterate just what we need
        .filter(nodeName -> nodeName.startsWith("org.opendatakit"))
        .flatMap(nodeName -> extract(userRootNode.node("/" + nodeName), "/" + nodeName));

    return Stream.of(appScopedPrefs, classScopedPrefs).flatMap(identity());
  }

  private static Stream<Pair<LegacyPreferenceKey, String>> extract(Preferences node, String currentName) {
    try {
      Stream<Pair<LegacyPreferenceKey, String>> prefs = Stream
          .of(node.keys())
          .map(keyName -> Pair.of(
              new LegacyPreferenceKey(currentName, keyName),
              Optional.ofNullable(node.get(keyName, null)).filter(not(String::isBlank))
          ))
          .filter(pair -> pair.getRight().isPresent())
          .map(pair -> pair.map(identity(), Optional::get));

      Stream<Pair<LegacyPreferenceKey, String>> childrenPrefs = Stream
          .of(node.childrenNames())
          .map(nodeName -> extract(node.node(nodeName), currentName + "/" + nodeName))
          .flatMap(identity());

      return Stream.of(prefs, childrenPrefs)
          .flatMap(identity())
          .filter(pair -> pair.getLeft().isRelevant());
    } catch (BackingStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public static void importLegacyPrefs(Container container, LegacyPrefs legacyPrefs) {
    importAppScopedPrefs(container, legacyPrefs);
    importPullAndPushPrefs(container, legacyPrefs);
    importDefaultExportConf(container, legacyPrefs);
    importCustomExportConfs(container, legacyPrefs);
  }

  private static void importCustomExportConfs(Container container, LegacyPrefs legacyPrefs) {
    LegacyPrefs exportConfs = legacyPrefs.filter(p -> p.getLeft().nodeName.equals("/org/opendatakit/briefcase/model") && p.getLeft().keyName.contains("_pull_source"));

    Set<String> formIds = exportConfs
        .map(p -> {
          Matcher matcher = FORM_NAME_EXTRACTOR.matcher(p.getLeft().keyName);
          return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.<String>empty();
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toSet());

    Stream<FormMetadata> updatedFormMetadata = formIds.stream()
        .map(formId -> OptionalProduct.all(
            container.formMetadata.fetchWithFormIdWithoutPullSource(formId),
            readSource(formId + "_pull_source", exportConfs)
        ))
        .map(op -> op.map(FormMetadata::withPullSource))
        .filter(Optional::isPresent)
        .map(Optional::get);

    container.formMetadata.persist(updatedFormMetadata);
  }

  private static void importDefaultExportConf(Container container, LegacyPrefs legacyPrefs) {
    ExportConfiguration.Builder defaultExportConfBuilder = ExportConfiguration.Builder.empty();
    legacyPrefs.getValue("exportDir").map(Paths::get).ifPresent(defaultExportConfBuilder::setExportDir);
    legacyPrefs.getValue("pemFile").map(Paths::get).ifPresent(defaultExportConfBuilder::setPemFile);
    legacyPrefs.getValues("startDate", "endDate").map((start, end) -> DateRange.from(LocalDate.parse(start), LocalDate.parse(end))).ifPresent(defaultExportConfBuilder::setDateRange);
    setOverridableBoolean(legacyPrefs, "pullBefore", defaultExportConfBuilder::setStartFromLast);
    setOverridableBoolean(legacyPrefs, "overwriteExistingFiles", defaultExportConfBuilder::setOverwriteFiles);
    setOverridableBoolean(legacyPrefs, "exportMedia", defaultExportConfBuilder::setExportAttachments);
    setOverridableBoolean(legacyPrefs, "splitSelectMultiples", defaultExportConfBuilder::setSplitSelectMultiples);
    setOverridableBoolean(legacyPrefs, "includeGeoJsonExport", defaultExportConfBuilder::setIncludeGeoJsonExport);
    setOverridableBoolean(legacyPrefs, "removeGroupNames", defaultExportConfBuilder::setRemoveGroupNames);

    ExportConfiguration defaultExportConf = defaultExportConfBuilder.build();
    if (defaultExportConf.isValid())
      container.preferences.execute(setDefaultExportConfiguration(defaultExportConf));
  }

  private static void importPullAndPushPrefs(Container container, LegacyPrefs legacyPrefs) {
    Stream.of(
        readSource("pull_source", legacyPrefs.filter(p1 -> p1.getLeft().nodeName.equals("/org.opendatakit.briefcase.ui.pull.PullPanel")))
            .map(PreferenceCommands::setCurrentSource),
        // We were using (mistakenly) the same keys in both panels
        readSource("pull_source", legacyPrefs.filter(p -> p.getLeft().nodeName.equals("/org.opendatakit.briefcase.ui.push.PushPanel")))
            .map(PreferenceCommands::setCurrentTarget)
    ).filter(Optional::isPresent).map(Optional::get)
        .forEach(container.preferences::execute);
  }

  private static void importAppScopedPrefs(Container container, LegacyPrefs legacyPrefs) {
    Stream.of(
        legacyPrefs.getValues("briefcaseProxyHost", "briefcaseProxyPort")
            .map((host, port) -> new HttpHost(host, parseInt(port)))
            .map(PreferenceCommands::setHttpProxy),
        legacyPrefs.getValue("maxHttpConnections")
            .map(Integer::parseInt)
            .map(PreferenceCommands::setMaxHttpConnections),
        legacyPrefs.getValue("briefcaseResumeLastPull")
            .map(Boolean::parseBoolean)
            .map(PreferenceCommands::setStartPullFromLast),
        legacyPrefs.getValue("briefcaseStorePasswordsConsent")
            .map(Boolean::parseBoolean)
            .map(PreferenceCommands::setRememberPasswords),
        legacyPrefs.getValue("briefcaseTrackingConsent")
            .map(Boolean::parseBoolean)
            .map(PreferenceCommands::setTrackingConsent)
    ).filter(Optional::isPresent).map(Optional::get)
        .forEach(container.preferences::execute);
  }

  private static Optional<SourceOrTarget> readSource(String prefix, LegacyPrefs prefs) {
    return Optionals.race(
        prefs.getValues(prefix + "_central_url", prefix + "_central_project_id", prefix + "_central_username", prefix + "_central_password")
            .map((url, projectId, username, password) -> CentralServer.of(url(url), parseInt(projectId), Credentials.from(username, password))),
        // Watch the order! Aggregate.authenticated with credentials must come before Aggregate.normal
        prefs.getValues(prefix + "_aggregate_url", prefix + "_aggregate_username", prefix + "_central_password")
            .map((url, username, password) -> AggregateServer.authenticated(url(url), Credentials.from(username, password))),
        prefs.getValue(prefix + "_aggregate_url")
            .map(url -> AggregateServer.normal(url(url)))
    );
  }

  private static void setOverridableBoolean(LegacyPrefs legacyPrefs, String keyName, Consumer<OverridableBoolean> setter) {
    OverridableBoolean pullBefore = legacyPrefs.getValue(keyName)
        .map(OverridableBoolean::from)
        .orElse(OverridableBoolean.empty());
    if (!pullBefore.isEmpty())
      setter.accept(pullBefore);
  }

  public static boolean prefsDetected() {
    return !LegacyPrefs.read().isEmpty();
  }

  public boolean isEmpty() {
    return prefs.isEmpty();
  }

  public Optional<String> getValue(String keyName) {
    return prefs.entrySet().stream()
        .filter(e -> e.getKey().keyName.equals(keyName))
        .findFirst()
        .map(Map.Entry::getValue);
  }

  public OptionalProduct.OptionalProduct2<String, String> getValues(String keyNameA, String keyNameB) {
    return OptionalProduct.all(
        getValue(keyNameA),
        getValue(keyNameB)
    );
  }

  public OptionalProduct.OptionalProduct3<String, String, String> getValues(String keyNameA, String keyNameB, String keyNameC) {
    return OptionalProduct.all(
        getValue(keyNameA),
        getValue(keyNameB),
        getValue(keyNameC)
    );
  }

  public OptionalProduct.OptionalProduct4<String, String, String, String> getValues(String keyNameA, String keyNameB, String keyNameC, String keyNameD) {
    return OptionalProduct.all(
        getValue(keyNameA),
        getValue(keyNameB),
        getValue(keyNameC),
        getValue(keyNameD)
    );
  }

  public boolean hasKey(String keyName) {
    return prefs.entrySet().stream().anyMatch(e -> e.getKey().keyName.equals(keyName));
  }

  public boolean hasKeys(String... keyNames) {
    return Stream.of(keyNames).map(this::hasKey).reduce(true, Boolean::logicalAnd);
  }

  public <T> Stream<T> map(Function<Pair<LegacyPreferenceKey, String>, T> mapper) {
    return getPrefsStream().map(mapper);
  }

  public LegacyPrefs filter(Predicate<Pair<LegacyPreferenceKey, String>> predicate) {
    return new LegacyPrefs(getPrefsStream().filter(predicate).collect(toMap(Pair::getLeft, Pair::getRight)));
  }

  public void forEach(Consumer<Pair<LegacyPreferenceKey, String>> consumer) {
    getPrefsStream().forEach(consumer);
  }

  private Stream<Pair<LegacyPreferenceKey, String>> getPrefsStream() {
    return prefs.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
  }
}

