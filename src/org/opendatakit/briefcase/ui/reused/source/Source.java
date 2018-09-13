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

import static org.opendatakit.briefcase.ui.reused.FileChooser.isUnderBriefcaseFolder;
import static org.opendatakit.briefcase.ui.reused.UI.errorMessage;

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
import org.opendatakit.briefcase.pull.FormInstaller;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.RemoteServer;
import org.opendatakit.briefcase.reused.http.Http;
import org.opendatakit.briefcase.ui.reused.FileChooser;
import org.opendatakit.briefcase.util.BadFormDefinition;
import org.opendatakit.briefcase.util.FileSystemUtils;
import org.opendatakit.briefcase.util.TransferAction;

/**
 * This interface represents a source (for lack of a better name) for pulling
 * or pushing forms in the context of Briefcase's UI.
 * <p>
 * Currently, Briefcase supports:
 * <ul>
 * <li>Pulling from:<ul>
 * <li>An <b>Aggregate</b> server</li>
 * <li>A <b>Custom Directory</b> pointing to the ODK Collect storage</li>
 * </ul></li>
 * <li>Pushing to:<ul>
 * <li>An <b>Aggregate</b> server</li>
 * </ul></li>
 * </ul>
 * <p>
 * This interface follows the "sealed trait" pattern from Scala. This means:
 * <ul>
 * <li>No direct usage of subtypes of Source should be found outside this file.</li>
 * <li>Since this is Java, extracting subtypes to individual files is permitted but then,
 * the same rule should be applied to the namespace.</li>
 * <li>To obtain a new instance of Source, you need to use the static factories
 * and you can't cast the result to any subtype of Source.</li>
 * <li>No <code>instanceof</code> against any subtype is allowed.</li>
 * </ul>
 * <p>
 * Notes on the implementation:
 * <ul>
 * <li>The fact that the method {@link Source#push} is only implemented by {@link Source.Aggregate}
 * breaks Liskov's substitution principle, which hints about a flaw in the design of this
 * type hierarchy.</li>
 * <li>This interface should probably be segregated into two different PullSource and PushTarget
 * interfaces which, not only will improve naming, but also will prevent breaking Liskov.</li>
 * </ul>
 *
 * @param <T> the value type each Source produces when configured
 */
public interface Source<T> {
  /**
   * This method is required to let the calling site be unaware of the specific
   * subtype of {@link Source} it is dealing with.
   */
  static void clearAllPreferences(BriefcasePreferences prefs) {
    Aggregate.clearPreferences(prefs);
    CustomDir.clearPreferences(prefs);
    FormInComputer.clearPreferences(prefs);
  }

  /**
   * Factory of {@link Source} instances that deal with remote Aggregate servers
   * for pulling forms.
   *
   * @param consumer {@link Consumer} that would be applied the {@link RemoteServer}
   *                 instance configured by the user
   */
  static Source<RemoteServer> aggregatePull(Http http, Consumer<Source> consumer) {
    return new Source.Aggregate(http, server -> server.testPull(http), "Data Viewer", consumer);
  }

  /**
   * Factory of {@link Source} instances that deal with remote Aggregate servers
   * for pushing forms.
   *
   * @param consumer {@link Consumer} that would be applied the {@link RemoteServer}
   *                 instance configured by the user
   */
  static Source<RemoteServer> aggregatePush(Http http, Consumer<Source> consumer) {
    return new Source.Aggregate(http, server -> server.testPush(http), "Form Manager", consumer);
  }

  /**
   * Factory of {@link Source} instances that deal with custom filesystem directories
   * pointing to a ODK Collect storage directory.
   *
   * @param consumer {@link Consumer} that would be applied the {@link Path} once
   *                 the user selects it
   */
  static Source<Path> customDir(Consumer<Source> consumer) {
    return new Source.CustomDir(consumer);
  }

  /**
   * Factory of {@link Source} instances that deal with an individual form in
   * the user's computer
   *
   * @param consumer {@link Consumer} that would be applied the {@link FormStatus} once
   *                 the user selects its form definition file
   */
  static Source<FormStatus> formInComputer(Consumer<Source> consumer) {
    return new Source.FormInComputer(consumer);
  }

  /**
   * Launches whatever means this {@link Source} requires to let the user configure
   * its value.
   *
   * @param container {@link Container} object that could be used to pop dialogs up
   */
  // TODO Better replace this with an abstraction that deals with Briefcase dialogs
  void onSelect(Container container);

  /**
   * Sets the value of this source.
   */
  void set(T t);

  /**
   * Tests the given object <code>o</code> to check if it's compatible with the type
   * of values this {@link Source} is capable of storing.
   *
   * @return true if it is compatible, false otherwise
   */
  boolean accepts(Object o);

  /**
   * Returns the list of forms that this configured {@link Source} has access to.
   */
  List<FormStatus> getFormList();

  /**
   * Stores the value of this {@link Source} in the given {@link BriefcasePreferences}.
   *
   * @param storePasswords {@link Boolean} telling this source whether to store
   *                       passwords or not
   */
  void storePreferences(BriefcasePreferences prefs, boolean storePasswords);

  /**
   * Pulls forms to the Briefcase Storage Directory from this configured {@link Source}.
   *
   * @param forms             {@link List} of forms to be pulled
   * @param terminationFuture object that to make the operation cancellable
   * @param includeIncomplete when passed true, it enables requesting the incomplete
   *                          submissions. This needs to be supported by the selected source
   */
  void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete);

  /**
   * Pushes forms to this configured {@link Source}.
   *
   * @param forms             {@link List} of forms to be pulled
   * @param terminationFuture object that to make the operation cancellable
   */
  void push(List<FormStatus> forms, TerminationFuture terminationFuture);

  /**
   * Returns whether or not this {@link Source} supports reloading.
   *
   * @return true if it supports reload, false otherwise
   */
  boolean canBeReloaded();

  /**
   * Returns a textual description of this {@link Source}.
   */
  String getDescription();

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
    public List<FormStatus> getFormList() {
      return server.getFormsList(http).stream()
          .map(FormStatus::new)
          .collect(Collectors.toList());
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      server.storePreferences(prefs, storePasswords);
    }

    static void clearPreferences(BriefcasePreferences prefs) {
      prefs.removeAll(RemoteServer.PREFERENCE_KEYS);
    }

    @Override
    public void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete) {
      TransferAction.transferServerToBriefcase(server.asServerConnectionInfo(), terminationFuture, forms, briefcaseDir, pullInParallel, includeIncomplete);
    }

    @Override
    public void push(List<FormStatus> forms, TerminationFuture terminationFuture) {
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
    public List<FormStatus> getFormList() {
      return FileSystemUtils.getODKFormList(path.toFile()).stream()
          .map(FormStatus::new)
          .collect(Collectors.toList());
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      // No prefs to store
    }

    static void clearPreferences(BriefcasePreferences prefs) {
      // No prefs to clear
    }

    @Override
    public void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete) {
      TransferAction.transferODKToBriefcase(briefcaseDir, path.toFile(), terminationFuture, forms);
    }

    @Override
    public void push(List<FormStatus> forms, TerminationFuture terminationFuture) {
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
    public List<FormStatus> getFormList() {
      return Collections.singletonList(form);
    }

    @Override
    public void storePreferences(BriefcasePreferences prefs, boolean storePasswords) {
      // No prefs to store
    }

    static void clearPreferences(BriefcasePreferences prefs) {
      // No prefs to clear
    }

    @Override
    public void pull(List<FormStatus> forms, TerminationFuture terminationFuture, Path briefcaseDir, boolean pullInParallel, Boolean includeIncomplete) {
      SwingUtilities.invokeLater(() -> FormInstaller.install(briefcaseDir, form));
    }

    @Override
    public void push(List<FormStatus> forms, TerminationFuture terminationFuture) {
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
    public String toString() {
      return "Form definition";
    }
  }
}

