/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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

  private static final Param<Void> IMPORT = Param.flag("pc", "pull_collect", "Pull from Collect");
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
