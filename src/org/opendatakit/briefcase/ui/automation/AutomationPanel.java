package org.opendatakit.briefcase.ui.automation;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.isWindows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.automation.AutomationConfiguration;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.CacheUpdateEvent;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.source.Source;
import org.opendatakit.briefcase.util.FormCache;
import org.opendatakit.common.cli.Param;

public class AutomationPanel {
  public static final String TAB_NAME = "Automation";

  private final AutomationPanelForm view;
  private final TransferForms forms;
  private final FormCache formCache;
  private final BriefcasePreferences appPreferences;

  private Optional<Source<?>> pullSource;
  private Optional<Source<?>> pushSource;

  public AutomationPanel(AutomationPanelForm view, TransferForms forms, BriefcasePreferences tabPreferences, BriefcasePreferences appPreferences, FormCache formCache) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.forms = forms;
    this.formCache = formCache;
    this.appPreferences = appPreferences;

    view.onPullSource(pullSource -> this.pullSource = Optional.of(pullSource));

    view.onPushSource(pushSource -> this.pushSource = Optional.of(pushSource));

    view.onGenerate(config -> generateScript(isWindows() ? "automation.bat" : "automation.sh", config));
  }

  private void generateScript(String scriptName, AutomationConfiguration configuration) {
    Path scriptDirPath = configuration.getScriptLocation().orElseThrow(BriefcaseException::new);
    Path storageDir = appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new).getParent();
    String exportTemplate = "java -jar {0} --export --form_id {1} --storage_directory {2} --export_directory {3} --export_filename {4}.csv";
    String template = "java -jar {0} --form_id {1} --storage_directory {2}{3}";
    List<String> scriptLines = new ArrayList<>();

    String jarInstructions = writeBriefcaseJarToScript();
    scriptLines.add(jarInstructions);
    String briefcaseJar = isWindows() ? "%JAR%" : "$JAR";
    List<String> pullInstructions = forms.getSelectedForms()
        .stream()
        .filter(FormStatus::isPullSelected)
        .map(form -> MessageFormat.format(
            template,
            briefcaseJar,
            form.getFormDefinition().getFormId(),
            storageDir.toString(),
            getParams(pullSource.orElseThrow(BriefcaseException::new).getPullCliParams())
        )).collect(toList());
    scriptLines.addAll(pullInstructions);
    List<String> exportInstructions = forms.getSelectedForms()
        .stream()
        .filter(FormStatus::isExportSelected)
        .map(form -> MessageFormat.format(
            exportTemplate,
            briefcaseJar,
            form.getFormDefinition().getFormId(),
            storageDir.toString(),
            "/tmp",
            form.getFormName()
        ))
        .collect(Collectors.toList());

    // Add two blank lines before adding export instructions
    scriptLines.add("");
    scriptLines.add("");
    scriptLines.addAll(exportInstructions);

    List<String> pushInstructions = forms.getSelectedForms()
        .stream()
        .filter(FormStatus::isPushSelected)
        .map(form -> MessageFormat.format(
            template,
            briefcaseJar,
            form.getFormDefinition().getFormId(),
            storageDir.toString(),
            getParams(pushSource.orElseThrow(BriefcaseException::new).getPushCliParams())
        )).collect(toList());
    // Add two blank lines before adding push instructions
    scriptLines.add("");
    scriptLines.add("");
    scriptLines.addAll(pushInstructions);
    try {
      Files.write(
          scriptDirPath.resolve(scriptName),
          scriptLines,
          CREATE,
          TRUNCATE_EXISTING
      );
      view.showConfirmation();
    } catch (IOException e) {
      throw new BriefcaseException(e);
    }

  }

  private String writeBriefcaseJarToScript() {
    String jarPath = System.getProperty("java.class.path");
    if (isWindows())
      return "IF \"%1\"==\"\" SET \"JAR=" + jarPath + "\") ELSE ( SET \"JAR=%1\"\n\n";
    return "VAR=${1:-" + jarPath + "}\n\n";
  }

  private String getParams(Map<Param, String> keyValues) {
    StringBuilder commands = new StringBuilder("");
    keyValues.forEach((param, value) -> {
      String commandPair = " --" + param.getLongOption() + " " + value;
      commands.append(commandPair);
    });
    return commands.toString();
  }

  public static AutomationPanel from(Http http, BriefcasePreferences appPreferences, FormCache formCache) {
    TransferForms forms = TransferForms.from(toFormStatuses(formCache.getForms()));
    return new AutomationPanel(
        AutomationPanelForm.from(http, forms),
        forms,
        BriefcasePreferences.forClass(AutomationPanel.class),
        appPreferences,
        formCache
    );
  }

  public void updateForms() {
    forms.merge(toFormStatuses(formCache.getForms()));
    view.refresh();
  }

  @EventSubscriber(eventClass = CacheUpdateEvent.class)
  public void onCacheUpdateEvent(CacheUpdateEvent event) {
    updateForms();
    view.refresh();
  }

  @EventSubscriber(eventClass = FormStatusEvent.class)
  public void onCacheUpdateEvent(FormStatusEvent event) {
    view.refresh();
  }

  private static List<FormStatus> toFormStatuses(List<BriefcaseFormDefinition> formDefs) {
    return formDefs.stream()
        .map(formDefinition -> new FormStatus(EXPORT, formDefinition))
        .collect(toList());
  }

  public JPanel getContainer() {
    return view.container;
  }
}
