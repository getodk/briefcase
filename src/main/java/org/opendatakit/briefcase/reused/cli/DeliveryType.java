package org.opendatakit.briefcase.reused.cli;

import static org.opendatakit.briefcase.delivery.LegacyPrefs.importLegacyPrefs;
import static org.opendatakit.briefcase.delivery.LegacyPrefsStatus.IGNORED;
import static org.opendatakit.briefcase.delivery.LegacyPrefsStatus.IMPORTED;
import static org.opendatakit.briefcase.delivery.LegacyPrefsStatus.UNDECIDED;
import static org.opendatakit.briefcase.reused.model.preferences.PreferenceCommands.setLegacyPrefsStatus;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.opendatakit.briefcase.delivery.LegacyPrefs;
import org.opendatakit.briefcase.delivery.cli.launchgui.LegacyPrefsDecisionDialogForm;
import org.opendatakit.briefcase.delivery.cli.launchgui.WorkspaceLocationDialogForm;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.Container;

public interface DeliveryType {
  DeliveryType CLI = new Cli();
  DeliveryType GUI = new Gui();

  void promptWorkspaceLocation(Container container, Consumer<Path> onWorkspaceLocation);

  void promptLegacyPrefsDecision(Container container);

  class Cli implements DeliveryType {
    @Override
    public void promptWorkspaceLocation(Container container, Consumer<Path> onWorkspaceLocation) {
      throw new BriefcaseException("Can't set workspace interactively with the CLI. Please, use the -wl CLI arg to set a workspace location");
    }

    @Override
    public void promptLegacyPrefsDecision(Container container) {
      System.out.println("Legacy preferences from Briefcase v1 have been detected but the import workflow is not implemented in the CLI. If you would like to import preferences from Briefcase v1, run the GUI instead");
      container.preferences.execute(setLegacyPrefsStatus(UNDECIDED));
    }
  }

  class Gui implements DeliveryType {
    @Override
    public void promptWorkspaceLocation(Container container, Consumer<Path> onWorkspaceLocation) {
      new WorkspaceLocationDialogForm(
          container.workspace,
          path -> onWorkspaceLocation.accept(path.orElseThrow(BriefcaseException::new))
      ).open();
    }

    @Override
    public void promptLegacyPrefsDecision(Container container) {
      LegacyPrefs legacyPrefs = LegacyPrefs.read();
      new LegacyPrefsDecisionDialogForm(
          () -> {
            importLegacyPrefs(container, legacyPrefs);
            container.preferences.execute(setLegacyPrefsStatus(IMPORTED));
          },
          () -> container.preferences.execute(setLegacyPrefsStatus(IGNORED)),
          () -> container.preferences.execute(setLegacyPrefsStatus(UNDECIDED))
      ).open();
    }


  }

}
