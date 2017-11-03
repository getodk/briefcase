/*
 * Copyright (C) 2014 University of Washington.
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


package org.opendatakit.briefcase.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMReader;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.aggregate.parser.BaseFormParserForJavaRosa;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.FormStatusEvent;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.ParsingException;
import org.opendatakit.briefcase.model.RetrieveAvailableFormsFailedEvent;
import org.opendatakit.briefcase.model.ServerConnectionInfo;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransferFailedEvent;
import org.opendatakit.briefcase.model.TransferSucceededEvent;
import org.opendatakit.briefcase.model.XmlDocumentFetchException;
import org.opendatakit.briefcase.util.ExportToCsv;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.RetrieveAvailableFormsFromServer;
import org.opendatakit.briefcase.util.ServerConnectionTest;
import org.opendatakit.briefcase.util.TransferFromODK;
import org.opendatakit.briefcase.util.TransferFromServer;

/**
 * 
 * Command line interface contributed by Nafundi
 * 
 * @author chartung@nafundi.com
 *
 */
public class BriefcaseCLI {

    private CommandLine mCommandline;

    private static final Log log = LogFactory.getLog(BaseFormParserForJavaRosa.class);

    public BriefcaseCLI(CommandLine cl) {
        mCommandline = cl;
    }

    public void run() {
        String username = mCommandline.getOptionValue(MainBriefcaseWindow.ODK_USERNAME);
        String password = mCommandline.getOptionValue(MainBriefcaseWindow.ODK_PASSWORD);
        String server = mCommandline.getOptionValue(MainBriefcaseWindow.AGGREGATE_URL);
        String formid = mCommandline.getOptionValue(MainBriefcaseWindow.FORM_ID);
        String storageDir = mCommandline.getOptionValue(MainBriefcaseWindow.STORAGE_DIRECTORY);
        String fileName = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_FILENAME);
        String exportPath = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_DIRECTORY);
        String startDateString = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_START_DATE);
        String endDateString = mCommandline.getOptionValue(MainBriefcaseWindow.EXPORT_END_DATE);
        // note that we invert incoming value
        boolean exportMedia = !mCommandline.hasOption(MainBriefcaseWindow.EXCLUDE_MEDIA_EXPORT);
        boolean overwrite = mCommandline.hasOption(MainBriefcaseWindow.OVERWRITE_CSV_EXPORT);
        String odkDir = mCommandline.getOptionValue(MainBriefcaseWindow.ODK_DIR);
        String pemKeyFile = mCommandline.getOptionValue(MainBriefcaseWindow.PEM_FILE);

        BriefcaseFormDefinition toExport = null;
        TerminationFuture terminationFuture = new TerminationFuture();

        BriefcasePreferences.setBriefcaseDirectoryProperty(storageDir);
        File f = new StorageLocation().getBriefcaseFolder();

        if (!f.exists()) {
            boolean success = f.mkdirs();
            if (success) {
                System.out.println("Successfully created directory. Using: " + f.getAbsolutePath());
            } else {
                System.err.println("Unable to create directory: " + f.getAbsolutePath());
                System.exit(0);
            }
        } else if (f.exists() && !f.isDirectory()) {
            System.err.println("Not a directory.  " + f.getAbsolutePath());
            System.exit(0);
        } else if (f.exists() && f.isDirectory()) {
            System.out.println("Directory found, using " + f.getAbsolutePath());
        }

        if (BriefcasePreferences.appScoped().getBriefcaseDirectoryOrNull() != null) {
            FileSystemUtils.createFormCacheInBriefcaseFolder();
        }

        if (odkDir != null) {
            // importing from ODK dir
            List<FormStatus> statuses = new ArrayList<FormStatus>();
            File odk = new File(odkDir);
            List<OdkCollectFormDefinition> forms = FileSystemUtils.getODKFormList(odk);
            for (OdkCollectFormDefinition form : forms) {
                statuses.add(new FormStatus(FormStatus.TransferType.GATHER, form));
            }

            TransferFromODK source = new TransferFromODK(odk, terminationFuture, statuses);
            source.doAction();
        } else if (server != null) {
            // importing from server
            ServerConnectionInfo sci = new ServerConnectionInfo(server, username,
                    password.toCharArray());

            ServerConnectionTest backgroundAction = new ServerConnectionTest(sci,
                    terminationFuture, true);

            backgroundAction.run();
            boolean isSuccessful = backgroundAction.isSuccessful();

            if (!isSuccessful) {
                String errorString = backgroundAction.getErrorReason();
                System.err.println(errorString);
                System.exit(1);
            }

            terminationFuture.reset();
            
            RetrieveAvailableFormsFromServer source = new RetrieveAvailableFormsFromServer(sci,
                    terminationFuture);
            try {
                source.doAction();
            } catch (XmlDocumentFetchException | ParsingException e) {
                System.err.println("failed to retrieve forms");
                e.printStackTrace();
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
                System.err.println("form ID doesn't exist on server");
                System.exit(0);
            }

            System.out.println("beginning download: " + toDl.getFormName());
            terminationFuture.reset();
            List<FormStatus> formsToTransfer = new ArrayList<FormStatus>();
            formsToTransfer.add(toDl);

            TransferFromServer serverSource = new TransferFromServer(sci, terminationFuture,
                    formsToTransfer);

            boolean status = serverSource.doAction();
        }

        if (exportPath != null) {
            List<BriefcaseFormDefinition> forms = FileSystemUtils.getBriefcaseFormList();
            for (int i = 0; i < forms.size(); i++) {
                BriefcaseFormDefinition x = forms.get(i);
                if (formid.equals(x.getFormId())) {
                    toExport = x;
                    break;
                }
            }

            if (toExport == null) {
                System.err.println("Form not found");
                return;
            }

            File pemFile = null;
            if (toExport.isFileEncryptedForm() || toExport.isFieldEncryptedForm()) {
                if (pemKeyFile == null) {
                    System.err.println("Briefcase action failed: No specified PrivateKey file for encrypted form");
                    return;
                }
                pemFile = new File(pemKeyFile);
                if (!pemFile.exists()) {
                    System.err.println("Briefcase action failed: No PrivateKey file for encrypted form");
                    return;
                }

                String errorMsg = null;
                boolean success = false;
                for (;;) /* this only executes once... */ {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                new FileInputStream(pemFile), "UTF-8"));
                        PEMReader rdr = new PEMReader(br);
                        Object o = rdr.readObject();
                        try {
                          rdr.close();
                        } catch ( IOException e ) {
                          // ignore.
                        }
                        if (o == null) {
                            ODKOptionPane.showErrorDialog(null,
                                    errorMsg = "The supplied file is not in PEM format.",
                                    "Invalid RSA Private Key");
                            break;
                        }
                        PrivateKey privKey;
                        if (o instanceof KeyPair) {
                            KeyPair kp = (KeyPair)o;
                            privKey = kp.getPrivate();
                        } else if (o instanceof PrivateKey) {
                            privKey = (PrivateKey)o;
                        } else {
                            privKey = null;
                        }
                        if (privKey == null) {
                            ODKOptionPane.showErrorDialog(null,
                                    errorMsg = "The supplied file does not contain a private key.",
                                    "Invalid RSA Private Key");
                            break;
                        }
                        toExport.setPrivateKey(privKey);
                        success = true;
                        break;
                    } catch (IOException e) {
                        String msg = "The supplied PEM file could not be parsed.";
                        System.err.println("msg");
                        e.printStackTrace();
                        ODKOptionPane.showErrorDialog(null,
                                errorMsg = msg,
                                "Invalid RSA Private Key");
                        break;
                    }
                }
                if (!success) {
                    EventBus.publish(new ExportProgressEvent(errorMsg));
                    EventBus.publish(new ExportFailedEvent(toExport));
                    return;
                }
            }

            Date startDate = null;
            Date endDate = null;
            DateFormat df = new SimpleDateFormat(MainBriefcaseWindow.DATE_FORMAT);

            try {
                if (startDateString != null) {
                    startDate = df.parse(startDateString);
                }
                if (endDateString != null) {
                    endDate = df.parse(endDateString);
                }
            } catch (ParseException e) {
                System.err.println("bad date range");
                e.printStackTrace();
            }

            terminationFuture.reset();
            File dir = new File(exportPath);
            System.out.println("exporting to : " + dir.getAbsolutePath());
            ExportToCsv exp = new ExportToCsv(dir, toExport, terminationFuture, fileName,
                    exportMedia, overwrite, startDate, endDate);
            exp.doAction();
        }

    }

    @EventSubscriber(eventClass = ExportProgressEvent.class)
    public void progress(ExportProgressEvent event) {
        System.out.println(event.getText());
    }

    @EventSubscriber(eventClass = ExportFailedEvent.class)
    public void failedCompletion(ExportFailedEvent event) {
        System.err.println("Failed.");
    }

    @EventSubscriber(eventClass = TransferFailedEvent.class)
    public void failedCompletion(TransferFailedEvent event) {
        System.err.println("Transfer Failed");
    }

    @EventSubscriber(eventClass = ExportSucceededEvent.class)
    public void successfulCompletion(ExportSucceededEvent event) {
        System.out.println("Succeeded.");
    }

    @EventSubscriber(eventClass = TransferSucceededEvent.class)
    public void successfulCompletion(TransferSucceededEvent event) {
        System.out.println("Transfer Succeeded");
    }

    @EventSubscriber(eventClass = FormStatusEvent.class)
    public void updateDetailedStatus(FormStatusEvent fse) {
        System.out.println(fse.getStatusString());
    }

    @EventSubscriber(eventClass = RetrieveAvailableFormsFailedEvent.class)
    public void formsAvailableFromServer(RetrieveAvailableFormsFailedEvent event) {
        System.err.println("Accessing the server failed with error: " + event.getReason());
    }

}
