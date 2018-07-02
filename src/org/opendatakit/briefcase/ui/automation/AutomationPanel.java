package org.opendatakit.briefcase.ui.automation;

import static org.opendatakit.briefcase.util.FindDirectoryStructure.isWindows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.automation.AutomationConfiguration;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
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
        generateScript("automation.bat",  config);
      else
        generateScript("automation.sh", config);
    });
  }

  private void generateScript(String scriptName, AutomationConfiguration configuration) {
    Path scriptDirPath = configuration.getScriptLocation().orElseThrow(() -> new BriefcaseException("No script location directory defined"));
    String storageDir = appPreferences.getBriefcaseDir().orElseThrow(() -> new BriefcaseException("")).getParent().toString();
    String template = "java -jar briefcase.jar --export --form_id {0} --storage_directory " +
        storageDir + " --export_directory /tmp --export_filename {1}.csv";
    try {
      PrintWriter writer = new PrintWriter(scriptDirPath.toFile() + File.pathSeparator + scriptName);
      List<BriefcaseFormDefinition> forms = formCache.getForms();
      System.out.println(forms.size());
      System.out.println(storageDir);
      for (BriefcaseFormDefinition form: forms) {
        Object[] params = new Object[]{form.getFormId(), form.getFormName()};
        String command = MessageFormat.format(template, params);
        writer.println(command);
        writer.close();
      }
      writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
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
