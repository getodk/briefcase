package org.opendatakit.briefcase.ui.reused;

import static java.awt.Desktop.getDesktop;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class SwingUtils {
  /**
   * Opens a tab in the default desktop web browser with the provided URL
   */
  public static void uncheckedBrowse(URL url) {
    try {
      getDesktop().browse(url.toURI());
    } catch (URISyntaxException | IOException e) {
      throw new BriefcaseException(e);
    }
  }
}
