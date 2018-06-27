package org.opendatakit.briefcase.ui.automation;

import static org.opendatakit.briefcase.util.FindDirectoryStructure.isWindows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JPanel;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.opendatakit.briefcase.automation.AutomationConfiguration;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class AutomationPanel {
  public static final String TAB_NAME = "Automation";

  private final AutomationPanelForm view;
  private final AutomationConfiguration configuration;


  public AutomationPanel(AutomationPanelForm view, AutomationConfiguration configuration) {
    AnnotationProcessor.process(this);
    this.view = view;
    this.configuration = configuration;

    view.onSelectScriptDir(path -> configuration.setScriptLocation(path));

    view.onGenerate(() -> {
      if (isWindows())
        generateScript("automation.bat", "echo %JAVA_HOME%");
      else
        generateScript("automation.sh", "echo $JAVA_HOME");
    });
  }

  private void generateScript(String scriptName, String commands) {
    Path scriptDirPath = configuration.getScriptLocation().orElseThrow(() -> new BriefcaseException("No script location directory defined"));
    Path scriptFilePath = Paths.get(scriptDirPath.toString() + scriptName);
    try {
      Files.write(scriptFilePath, commands.getBytes());
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  public static AutomationPanel from() {
    return new AutomationPanel(
        new AutomationPanelForm(),
        AutomationConfiguration.empty()
    );
  }

  public JPanel getContainer() {
    return view.container;
  }
}
