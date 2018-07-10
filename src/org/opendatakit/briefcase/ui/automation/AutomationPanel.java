package org.opendatakit.briefcase.ui.automation;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.opendatakit.briefcase.util.FindDirectoryStructure.isWindows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.automation.AutomationConfiguration;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.util.FormCache;

public class AutomationPanel {
  public static final String TAB_NAME = "Automation";

  private final AutomationPanelForm view;
  private final FormCache formCache;
  private final BriefcasePreferences appPreferences;

  public AutomationPanel(AutomationPanelForm view, BriefcasePreferences appPreferences, FormCache formCache) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.formCache = formCache;
    this.appPreferences = appPreferences;

    view.onGenerate(config -> {
      System.out.println(config.getScriptLocation());
      if (isWindows())
        generateScript("automation.bat", config);
      else
        generateScript("automation.sh", config);
    });
  }

  private void generateScript(String scriptName, AutomationConfiguration configuration) {
    Path scriptDirPath = configuration.getScriptLocation().orElseThrow(BriefcaseException::new);
    Path storageDir = appPreferences.getBriefcaseDir().orElseThrow(BriefcaseException::new).getParent();
    String template = "java -jar {0} --export --form_id {1} --storage_directory {2} --export_directory {3} --export_filename {4}.csv";

    List<String> scriptLines = formCache.getForms()
        .stream()
        .map(form -> MessageFormat.format(
            template,
            "briefcase.jar",
            form.getFormId(),
            storageDir.toString(),
            "/tmp",
            form.getFormName()
        ))

        .collect(Collectors.toList());
    try {
      Files.write(
          scriptDirPath.resolve(scriptName),
          scriptLines,
          CREATE,
          TRUNCATE_EXISTING
      );
    } catch (IOException e) {
      throw new BriefcaseException(e);
    }

  }

  public static AutomationPanel from(BriefcasePreferences appPreferences, FormCache formCache) {
    return new AutomationPanel(
        new AutomationPanelForm(),
        appPreferences,
        formCache
    );
  }

  public JPanel getContainer() {
    return view.container;
  }
}
