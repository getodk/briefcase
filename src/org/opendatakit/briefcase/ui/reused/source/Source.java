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

import static org.opendatakit.briefcase.model.FormStatus.TransferType.GATHER;
import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;

import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.model.FormStatus;
import org.opendatakit.briefcase.model.OdkCollectFormDefinition;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.briefcase.ui.pull.FormInstaller;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;

public interface Source<T> {

  static boolean isValidCustomDir(Path f) {
    return Files.exists(f) && Files.isDirectory(f) && !isUnderBriefcaseFolder(f.toFile()) && Files.exists(f.resolve("forms")) && Files.isDirectory(f.resolve("forms"));
  }

  static void clearAllPreferences(BriefcasePreferences prefs) {
    Aggregate.clearPreferences(prefs);
    CustomDir.clearPreferences(prefs);
    FormInComputer.clearPreferences(prefs);
  }

  void onSelect(Container container);

  void set(T t);

  boolean accepts(Object o);

  List<FormStatus> getFormList();

  void storePreferences(BriefcasePreferences prefs, boolean storePasswords);

  void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir);

  void push(List<FormStatus> forms, TerminationFuture terminationFuture);

  String getDescription();

  class Aggregate implements Source<RemoteServer> {
    private final Http http;
    private final Consumer<Source> consumer;
    private RemoteServer server;

    Aggregate(Http http, Consumer<Source> consumer) {
      this.http = http;
      this.consumer = consumer;
    }

    @Override
    public void onSelect(Container ignored) {
      RemoteServerDialog dialog = RemoteServerDialog.empty(http);
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
    public List<FormStatus> getFormList() {
      return server.getFormsList(http).stream()
          .map(rfd -> new FormStatus(GATHER, rfd))
          .collect(Collectors.toList());
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      server.storePreferences(prefs, storePasswords);
    }

    public static void clearPreferences(BriefcasePreferences prefs) {
      prefs.removeAll(RemoteServer.PREFERENCE_KEYS);
    }

    @Override
    public void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir) {
      TransferAction.transferServerToBriefcase(server.asServerConnectionInfo(), terminationFuture, forms, briefcaseDir);
    }

    @Override
    public void push(List<FormStatus> forms, TerminationFuture terminationFuture) {
      TransferAction.transferBriefcaseToServer(server.asServerConnectionInfo(), terminationFuture, forms, http, server);
    }

    @Override
    public String getDescription() {
      return server.getBaseUrl().toString();
    }

    @Override
    public String toString() {
      return "Aggregate";
    }
  }

  class CustomDir implements Source<Path> {
    private final Consumer<Source> consumer;
    private Path path;

    CustomDir(Consumer<Source> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void onSelect(Container container) {
      FileChooser
          .directory(container, Optional.empty(), f -> isValidCustomDir(f.toPath()), "ODK Collect Folders")
          .choose()
          // TODO Changing the FileChooser to handle Paths instead of Files would improve this code and it's also coherent with the modernization (use NIO2 API) of this basecode
          .ifPresent(file -> set(file.toPath()));
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
    public List<FormStatus> getFormList() {
      return FileSystemUtils.getODKFormList(path.toFile()).stream()
          .map(formDef -> new FormStatus(GATHER, formDef))
          .collect(Collectors.toList());
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      // No prefs to store
    }

    public static void clearPreferences(BriefcasePreferences prefs) {
      // No prefs to clear
    }

    @Override
    public void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir) {
      TransferAction.transferODKToBriefcase(briefcaseDir, path.toFile(), terminationFuture, forms);
    }

    @Override
    public void push(List<FormStatus> forms, TerminationFuture terminationFuture) {
      throw new BriefcaseException("Can't push to a Custom Directory");
    }

    @Override
    public String getDescription() {
      return path.toString();
    }

    @Override
    public String toString() {
      return "Custom Directory";
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
        set(new FormStatus(GATHER, new OdkCollectFormDefinition(path.toFile())));
      } catch (BadFormDefinition e) {
        ODKOptionPane.showErrorDialog(container, "Bad form definition file. Please select another file.", "Wrong file");
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
    public List<FormStatus> getFormList() {
      return Collections.singletonList(form);
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      // No prefs to store
    }

    public static void clearPreferences(BriefcasePreferences prefs) {
      // No prefs to clear
    }

    @Override
    public void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir) {
      SwingUtilities.invokeLater(() -> FormInstaller.install(briefcaseDir, form));
    }

    @Override
    public void push(List<FormStatus> forms, TerminationFuture terminationFuture) {
      throw new BriefcaseException("Can't push to this source");
    }

    @Override
    public String getDescription() {
      return String.format("%s at %s", form.getFormName(), path.toString());
    }

    @Override
    public String toString() {
      return "Individual form in the computer";
    }
  }
}

