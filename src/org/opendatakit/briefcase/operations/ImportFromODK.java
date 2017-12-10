package org.opendatakit.briefcase.operations;

import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferFromODK;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;

public class ImportFromODK {

  private static final Param<Void> IMPORT = Param.flag("i", "import-odk", "Import from ODK");
  private static final Param<String> ODK_DIR = Param.arg("od", "odk_directory", "ODK directory");

  public static final Operation IMPORT_FROM_ODK = Operation.of(
      IMPORT,
      args -> importODK(
          args.get(STORAGE_DIR),
          args.get(ODK_DIR)
      ),
      Arrays.asList(STORAGE_DIR, ODK_DIR)
  );

  public static void importODK(String storageDir, String odkDir) {
    bootCache(storageDir);
    TerminationFuture terminationFuture = new TerminationFuture();
    List<FormStatus> statuses = new ArrayList<FormStatus>();
    File odk = new File(odkDir);
    List<OdkCollectFormDefinition> forms = FileSystemUtils.getODKFormList(odk);
    for (OdkCollectFormDefinition form : forms) {
      statuses.add(new FormStatus(FormStatus.TransferType.GATHER, form));
    }

    TransferFromODK source = new TransferFromODK(odk, terminationFuture, statuses);
    source.doAction();
  }
}
