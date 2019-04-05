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

package org.opendatakit.briefcase.ui.reused.source;

import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Cursor.getPredefinedCursor;
import static java.awt.Desktop.getDesktop;
import static java.util.stream.Collectors.toList;
import static javax.swing.SwingUtilities.invokeLater;
import static org.opendatakit.briefcase.reused.job.Job.run;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;
import static org.opendatakit.briefcase.ui.reused.UI.removeAllMouseListeners;

import java.awt.Container;
import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.pull.FormInstaller;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.pull.PullForm;
import org.opendatakit.briefcase.pull.PullResult;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.DeferredValue;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.reused.job.JobsRunner;
import org.opendatakit.briefcase.transfer.TransferForms;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.ui.reused.MouseAdapterBuilder;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Source<T> {
  Logger log = LoggerFactory.getLogger(Source.class);

  static void clearAllPreferences(BriefcasePreferences prefs) {
    Aggregate.clearPreferences(prefs);
    CustomDir.clearPreferences(prefs);
    FormInComputer.clearPreferences(prefs);
  }

  static Source<RemoteServer> aggregatePull(Http http, Consumer<Source> consumer) {
    return new Source.Aggregate(http, server -> server.testPull(http), "Data Viewer", consumer);
  }

  static Source<RemoteServer> aggregatePush(Http http, Consumer<Source> consumer) {
    return new Source.Aggregate(http, server -> server.testPush(http), "Form Manager", consumer);
  }

  static Source<Path> customDir(Consumer<Source> consumer) {
    return new Source.CustomDir(consumer);
  }

  static Source<FormStatus> formInComputer(Consumer<Source> consumer) {
    return new Source.FormInComputer(consumer);
  }

  void onSelect(Container container);

  void set(T t);

  boolean accepts(Object o);

  DeferredValue<List<FormStatus>> getFormList();

  void storePreferences(BriefcasePreferences prefs, boolean storePasswords);

  JobsRunner pull(TransferForms forms, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate);

  void push(TransferForms forms, TerminationFuture terminationFuture);

  boolean canBeReloaded();

  String getDescription();

  void decorate(JLabel label);

  class Aggregate implements Source<RemoteServer> {
    private final Http http;
    private RemoteServer.Test serverTester;
    private String requiredPermission;
    private final Consumer<Source> consumer;
    private RemoteServer server;

    Aggregate(Http http, RemoteServer.Test serverTester, String requiredPermission, Consumer<Source> consumer) {
      this.http = http;
      this.serverTester = serverTester;
      this.requiredPermission = requiredPermission;
      this.consumer = consumer;
    }

    @Override
    public void onSelect(Container ignored) {
      RemoteServerDialog dialog = RemoteServerDialog.empty(serverTester, requiredPermission);
      dialog.onConnect(this::set);
      dialog.getForm().setVisible(true);
    }

    @Override
    public void set(RemoteServer server) {
      this.server = server;
      consumer.accept(this);
    }

    @Override
    public boolean accepts(Object o) {
      return o instanceof RemoteServer;
    }

    @Override
    public DeferredValue<List<FormStatus>> getFormList() {
      return DeferredValue.of(() -> server.getFormsList(http).stream()
          .map(FormStatus::new)
          .collect(toList()));
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      server.storePreferences(prefs, storePasswords);
    }

    static void clearPreferences(BriefcasePreferences prefs) {
      prefs.removeAll(RemoteServer.PREFERENCE_KEYS);
    }

    @Override
    public JobsRunner pull(TransferForms forms, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate) {
      Http reusableHttp = http.reusingConnections();

      return new JobsRunner<PullResult>()
          .onError(e -> {
            log.error("Error pulling forms", e);
            EventBus.publish(new PullEvent.Failure());
          })
          .onSuccess(results -> {
            results.forEach(result -> forms.setLastPullCursor(result.getForm(), result.getLastCursor()));
            EventBus.publish(new PullEvent.Success(forms, server.asServerConnectionInfo()));
          })
          .launchAsync(forms.map(form -> PullForm.pull(reusableHttp, server, briefcaseDir, includeIncomplete, EventBus::publish, form)));
    }

    @Override
    public void push(TransferForms forms, TerminationFuture terminationFuture) {
      TransferAction.transferBriefcaseToServer(server.asServerConnectionInfo(), terminationFuture, forms, http, server);
    }

    @Override
    public boolean canBeReloaded() {
      return true;
    }

    @Override
    public String getDescription() {
      return server.getBaseUrl().toString();
    }

    private static void uncheckedBrowse(URL url) {
      try {
        getDesktop().browse(url.toURI());
      } catch (URISyntaxException | IOException e) {
        throw new BriefcaseException(e);
      }
    }

    @Override
    public void decorate(JLabel label) {
      label.setText("<html><a href=\"" + server.getBaseUrl().toString() + "\">" + getDescription() + "</a></html>");
      label.setCursor(getPredefinedCursor(HAND_CURSOR));
      removeAllMouseListeners(label);
      label.addMouseListener(new MouseAdapterBuilder()
          .onClick(__ -> invokeLater(() -> uncheckedBrowse(server.getBaseUrl())))
          .build());
    }

    @Override
    public String toString() {
      return "Aggregate server";
    }
  }

  class CustomDir implements Source<Path> {
    private final Consumer<Source> consumer;
    private Path path;

    CustomDir(Consumer<Source> consumer) {
      this.consumer = consumer;
    }

    static boolean isValidCustomDir(Path f) {
      return Files.exists(f) && Files.isDirectory(f) && !isUnderBriefcaseFolder(f.toFile()) && Files.exists(f.resolve("forms")) && Files.isDirectory(f.resolve("forms"));
    }

    @Override
    public void onSelect(Container container) {
      FileChooser
          .directory(container, Optional.empty())
          .choose()
          // TODO Changing the FileChooser to handle Paths instead of Files would improve this code and it's also coherent with the modernization (use NIO2 API) of this basecode
          .ifPresent(file -> {
            if (isValidCustomDir(file.toPath()))
              set(file.toPath());
            else {
              errorMessage(
                  "Wrong directory",
                  "The selected directory doesn't look like an ODK Collect storage directory. Please select another directory."
              );
            }
          });
    }

    @Override
    public void set(Path path) {
      this.path = path;
      consumer.accept(this);
    }

    @Override
    public boolean accepts(Object o) {
      return o instanceof Path;
    }

    @Override
    public DeferredValue<List<FormStatus>> getFormList() {
      return DeferredValue.of(() -> FileSystemUtils.getODKFormList(path.toFile()).stream()
          .map(FormStatus::new)
          .collect(toList()));
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      // No prefs to store
    }

    static void clearPreferences(BriefcasePreferences prefs) {
      // No prefs to clear
    }

    @Override
    public JobsRunner pull(TransferForms forms, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate) {
      return JobsRunner.launchAsync(
          run(jobStatus -> TransferAction.transferODKToBriefcase(briefcaseDir, path.toFile(), jobStatus, forms))
      );
    }

    @Override
    public void push(TransferForms forms, TerminationFuture terminationFuture) {
      throw new BriefcaseException("Can't push to a Collect directory");
    }

    @Override
    public boolean canBeReloaded() {
      return false;
    }

    @Override
    public String getDescription() {
      return path.toString();
    }

    @Override
    public void decorate(JLabel label) {
      label.setText(getDescription());
      label.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      removeAllMouseListeners(label);
    }

    @Override
    public String toString() {
      return "Collect directory";
    }
  }

  class FormInComputer implements Source<FormStatus> {
    private final Consumer<Source> consumer;
    private Path path;
    private FormStatus form;

    FormInComputer(Consumer<Source> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void onSelect(Container container) {
      Optional<Path> selectedFile = FileChooser
          .file(container, Optional.empty(), file -> Files.isDirectory(file.toPath()) ||
              (Files.isRegularFile(file.toPath()) && file.toPath().getFileName().toString().endsWith(".xml")), "XML file")
          .choose()
          // TODO Changing the FileChooser to handle Paths instead of Files would improve this code and it's also coherent with the modernization (use NIO2 API) of this basecode
          .map(File::toPath);

      // Shortcircuit if the user has cancelled
      if (!selectedFile.isPresent())
        return;

      try {
        path = selectedFile.get();
        set(new FormStatus(new OdkCollectFormDefinition(path.toFile())));
      } catch (BadFormDefinition e) {
        errorMessage("Wrong file", "Bad form definition file. Please select another file.");
      }
    }

    @Override
    public void set(FormStatus form) {
      this.form = form;
      consumer.accept(this);
    }

    @Override
    public boolean accepts(Object o) {
      return o instanceof Path;
    }

    @Override
    public DeferredValue<List<FormStatus>> getFormList() {
      return DeferredValue.of(() -> Collections.singletonList(form));
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      // No prefs to store
    }

    static void clearPreferences(BriefcasePreferences prefs) {
      // No prefs to clear
    }

    @Override
    public JobsRunner pull(TransferForms forms, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete, boolean resumeLastPull, Optional<LocalDate> startFromDate) {
      return JobsRunner.launchAsync(
          run(jobStatus -> FormInstaller.install(briefcaseDir, form))
      );
    }

    @Override
    public void push(TransferForms forms, TerminationFuture terminationFuture) {
      throw new BriefcaseException("Can't push to this source");
    }

    @Override
    public boolean canBeReloaded() {
      return false;
    }

    @Override
    public String getDescription() {
      return String.format("%s at %s", form.getFormName(), path.toString());
    }

    @Override
    public void decorate(JLabel label) {
      label.setText(getDescription());
      label.setCursor(getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      removeAllMouseListeners(label);
    }

    @Override
    public String toString() {
      return "Form definition";
    }
  }
}

