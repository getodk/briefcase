package org.opendatakit.briefcase.operations;

import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.Common.bootCache;

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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMReader;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.ExportToCsv;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;

public class Export {
  private static final Log LOGGER = LogFactory.getLog(Export.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
  private static final Param<Void> EXPORT = Param.flag("e", "export", "Export a form");
  private static final Param<String> EXPORT_DIR = Param.arg("ed", "export_directory", "Export directory");
  private static final Param<String> FILE = Param.arg("file", "export_filename", "Filename for export operation");
  private static final Param<Date> START = Param.arg("start", "export_start_date", "Export start date", Export::toDate);
  private static final Param<Date> END = Param.arg("end", "export_end_date", "Export end date", Export::toDate);
  private static final Param<Void> EXCLUDE_MEDIA = Param.flag("exme", "exclude_media_export", "Exclude media in export");
  private static final Param<Void> OVERWRITE = Param.flag("ow", "overwrite_csv_export", "Overwrite files during export");
  private static final Param<String> PEM_FILE = Param.arg("pem", "pem_file", "PEM file for form decryption");

  public static Date toDate(String s) {
    try {
      return DATE_FORMAT.parse(s);
    } catch (ParseException e) {
      LOGGER.error("bad date range", e);
      return null;
    }
  }


  public static Operation EXPORT_FORM = Operation.of(
      EXPORT,
      args -> export(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(FILE),
          args.get(EXPORT_DIR),
          args.getOrNull(START),
          args.getOrNull(END),
          !args.has(EXCLUDE_MEDIA),
          args.has(OVERWRITE),
          args.getOptional(PEM_FILE)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, FILE, EXPORT_DIR),
      Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END)
  );

  public static void export(String storageDir, String formid, String fileName, String exportPath, Date startDateString, Date endDateString, boolean exportMedia, boolean overwrite, Optional<String> pemKeyFile) {
    bootCache(storageDir);
    BriefcaseFormDefinition formDefinition = null;
    List<BriefcaseFormDefinition> forms = FileSystemUtils.getBriefcaseFormList();
    for (int i = 0; i < forms.size(); i++) {
      BriefcaseFormDefinition x = forms.get(i);
      if (formid.equals(x.getFormId())) {
        formDefinition = x;
        break;
      }
    }

    if (formDefinition == null) {
      LOGGER.error("Form not found");
      return;
    }

    if (formDefinition.isFileEncryptedForm() || formDefinition.isFieldEncryptedForm()) {
      File pemFile;
      if (!pemKeyFile.isPresent()) {
        LOGGER.error("Briefcase action failed: No specified PrivateKey file for encrypted form");
        return;
      }
      pemFile = new File(pemKeyFile.get());
      if (!pemFile.exists()) {
        LOGGER.error("Briefcase action failed: No PrivateKey file for encrypted form");
        return;
      }

      String errorMsg = null;
      boolean success = false;
      for (; ; ) /* this only executes once... */ {
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(
              new FileInputStream(pemFile), "UTF-8"));
          PEMReader rdr = new PEMReader(br);
          Object o = rdr.readObject();
          try {
            rdr.close();
          } catch (IOException e) {
            // ignore.
          }
          if (o == null) {
            errorMsg = "The supplied file is not in PEM format.";
            System.err.println(errorMsg);
            break;
          }
          PrivateKey privKey;
          if (o instanceof KeyPair) {
            KeyPair kp = (KeyPair) o;
            privKey = kp.getPrivate();
          } else if (o instanceof PrivateKey) {
            privKey = (PrivateKey) o;
          } else {
            privKey = null;
          }
          if (privKey == null) {
            errorMsg = "The supplied file does not contain a private key.";
            System.err.println(errorMsg);
            break;
          }
          formDefinition.setPrivateKey(privKey);
          success = true;
          break;
        } catch (IOException e) {
          System.err.println("The supplied PEM file could not be parsed.");
          e.printStackTrace();
          break;
        }
      }
      if (!success) {
        EventBus.publish(new ExportProgressEvent(errorMsg));
        EventBus.publish(new ExportFailedEvent(formDefinition));
        return;
      }
    }

    TerminationFuture terminationFuture = new TerminationFuture();
    terminationFuture.reset();
    File dir = new File(exportPath);
    LOGGER.info("exporting to : " + dir.getAbsolutePath());
    ExportToCsv exp = new ExportToCsv(dir, formDefinition, terminationFuture, fileName, exportMedia, overwrite, startDateString, endDateString);
    exp.doAction();
  }
}
