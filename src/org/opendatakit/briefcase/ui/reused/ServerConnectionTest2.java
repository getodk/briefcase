package org.opendatakit.briefcase.ui.reused;

import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransmissionException;
import org.opendatakit.briefcase.operations.ServerConnectionTestException;
import org.opendatakit.briefcase.util.ServerConnectionTest;
import org.opendatakit.briefcase.util.ServerFetcher2;

public class ServerConnectionTest2 implements Runnable {
  private final ServerConnectionInfo info;
  private final boolean asTarget;

  private String errorReason = null;
  private boolean isSuccessful = false;

  public ServerConnectionTest2(ServerConnectionInfo info, boolean asTarget) {
    this.info = info;
    this.asTarget = asTarget;
  }

  public static void testPull(ServerConnectionInfo transferSettings) {
    ServerConnectionTest test = new ServerConnectionTest(transferSettings, new TerminationFuture(), false);
    test.run();
    if (!test.isSuccessful())
      throw new ServerConnectionTestException();
  }

  @Override
  public void run() {
    try {
      if (asTarget) {
        ServerUploader2.testServerUploadConnection(info);
      } else {
        ServerFetcher2.testServerDownloadConnection(info);
      }
      isSuccessful = true;
    } catch (TransmissionException ex) {
      errorReason = ex.getMessage();
      isSuccessful = false;
    }
  }

  public String getErrorReason() {
    return errorReason;
  }

  public boolean isSuccessful() {
    return isSuccessful;
  }
}
