package org.opendatakit.briefcase.operations;

import static org.opendatakit.briefcase.operations.Common.DIR;
import static org.opendatakit.briefcase.operations.Common.FORM_ID;
import static org.opendatakit.briefcase.operations.Common.STORAGE_DIR;
import static org.opendatakit.briefcase.operations.Common.bootCache;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportProgressPercentageEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.util.ExportAction;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.common.cli.Operation;
import org.opendatakit.common.cli.Param;
import org.opendatakit.common.pubsub.SimplePubSub;

public class Export {
  private static final Log LOGGER = LogFactory.getLog(Export.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
  private static final Function<String, Date> TO_DATE = s -> {
    try {
      return DATE_FORMAT.parse(s);
    } catch (ParseException e) {
      LOGGER.error("bad date range", e);
      return null;
    }
  };

  private static final Param<Void> EXPORT = Param.flag("e", "export", "Export a form");
  private static final Param<String> FILE = Param.arg("file", "filename", "Filename for export operation");
  private static final Param<Date> START = Param.arg("start", "start-date", "Export start date", TO_DATE);
  private static final Param<Date> END = Param.arg("end", "end-date", "Export end date", TO_DATE);
  private static final Param<Void> EXCLUDE_MEDIA = Param.flag("exme", "exclude-media", "Exclude media in export");
  private static final Param<Void> OVERWRITE = Param.flag("ow", "overwrite", "Overwrite files during export");
  private static final Param<String> PEM_FILE = Param.arg("pem", "pem-file", "PEM file for form decryption");


  public static Operation EXPORT_FORM = Operation.of(
      EXPORT,
      args -> export(
          args.get(STORAGE_DIR),
          args.get(FORM_ID),
          args.get(FILE),
          args.get(DIR),
          args.getOrNull(START),
          args.getOrNull(END),
          !args.has(EXCLUDE_MEDIA),
          args.has(OVERWRITE),
          args.getOptional(PEM_FILE)
      ),
      Arrays.asList(STORAGE_DIR, FORM_ID, FILE, DIR),
      Arrays.asList(PEM_FILE, EXCLUDE_MEDIA, OVERWRITE, START, END)
  );

  private static void export(String storageDir, String formid, String fileName, String exportPath, Date startDateString, Date endDateString, boolean exportMedia, boolean overwrite, Optional<String> pemKeyFile) {
    SimplePubSub pubSub = new SimplePubSub();
    bootCache(storageDir);
    BriefcaseFormDefinition formDefinition = getFormDefinition(formid);
    File pemFile = pemKeyFile.map(File::new).orElse(null);

    pubSub.subscribe(ExportProgressEvent.class, System.out::println);
    pubSub.subscribe(ExportProgressPercentageEvent.class, System.out::println);
    pubSub.subscribe(ExportFailedEvent.class, System.err::println);
    pubSub.subscribe(ExportSucceededEvent.class, System.out::println);
    pubSub.subscribe(ExportSucceededWithErrorsEvent.class, System.out::println);

    new ExportAction(pubSub, Runnable::run).export(
        new File(exportPath),
        ExportType.CSV,
        formDefinition,
        pemFile,
        new TerminationFuture(),
        startDateString,
        endDateString
    );
  }

  private static BriefcaseFormDefinition getFormDefinition(String formid) {
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
      System.err.println("Form not found");
      System.exit(1);
    }
    return formDefinition;
  }
}
