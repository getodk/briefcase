package org.opendatakit.briefcase.operations;

import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.XmlDocumentFetchException;
import org.opendatakit.briefcase.util.RetrieveAvailableFormsFromServer;
import org.opendatakit.briefcase.util.ServerConnectionTest;
import org.opendatakit.briefcase.util.TransferFromServer;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;

public class PullFormFromAggregate {
  private static final Log LOGGER = LogFactory.getLog(PullFormFromAggregate.class);
  private static final Param<Void> PULL_AGGREGATE = Param.flag("pa", "pull-aggregate", "Pull form from an Aggregate instance");
  private static final Param<String> ODK_USERNAME = Param.arg("u", "odk_username", "ODK Username");
  private static final Param<String> ODK_PASSWORD = Param.arg("p", "odk_password", "ODK Password");
  private static final Param<String> AGGREGATE_SERVER = Param.arg("s", "aggregate_url", "Aggregate server URL");

  public static Operation PULL_FORM_FROM_AGGREGATE = Operation.of(
      PULL_AGGREGATE,
      args -> pullFormFromAggregate(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(ODK_USERNAME),
          args.get(ODK_PASSWORD),
          args.get(AGGREGATE_SERVER)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, ODK_USERNAME, ODK_PASSWORD, AGGREGATE_SERVER)
  );

  private static void pullFormFromAggregate(String storageDir, String formid, String username, String password, String server) {
    bootCache(storageDir);
    TerminationFuture terminationFuture = new TerminationFuture();
    ServerConnectionInfo sci = new ServerConnectionInfo(server, username, password.toCharArray());

    ServerConnectionTest backgroundAction = new ServerConnectionTest(sci, terminationFuture, true);

    backgroundAction.run();
    boolean isSuccessful = backgroundAction.isSuccessful();

    if (!isSuccessful) {
      String errorString = backgroundAction.getErrorReason();
      LOGGER.error(errorString);
      System.exit(1);
    }

    terminationFuture.reset();

    RetrieveAvailableFormsFromServer source = new RetrieveAvailableFormsFromServer(sci, terminationFuture);
    try {
      source.doAction();
    } catch (XmlDocumentFetchException | ParsingException e) {
      LOGGER.error("failed to retrieve forms", e);
    }

    List<FormStatus> statuses = source.getAvailableForms();
    FormStatus toDl = null;
    boolean found = false;
    for (int i = 0; i < statuses.size(); i++) {
      FormStatus fs = statuses.get(i);
      if (formid.equals(fs.getFormDefinition().getFormId())) {
        found = true;
        toDl = fs;
      }
    }
    if (!found) {
      LOGGER.error("form ID doesn't exist on server");
      System.exit(0);
    }

    LOGGER.info("beginning download: " + toDl.getFormName());
    terminationFuture.reset();
    List<FormStatus> formsToTransfer = new ArrayList<FormStatus>();
    formsToTransfer.add(toDl);

    TransferFromServer serverSource = new TransferFromServer(sci, terminationFuture, formsToTransfer);

    boolean status = serverSource.doAction();
  }
}
