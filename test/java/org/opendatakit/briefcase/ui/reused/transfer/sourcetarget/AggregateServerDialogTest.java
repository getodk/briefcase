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
package org.opendatakit.briefcase.ui.reused.transfer.sourcetarget;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.matchers.DialogMatchers.containsMessage;
import static org.opendatakit.briefcase.matchers.DialogMatchers.errorDialog;
import static org.opendatakit.briefcase.matchers.SwingMatchers.disabled;
import static org.opendatakit.briefcase.matchers.SwingMatchers.editable;
import static org.opendatakit.briefcase.matchers.SwingMatchers.enabled;
import static org.opendatakit.briefcase.matchers.SwingMatchers.visible;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.found;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.notFound;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.ok;
import static org.opendatakit.briefcase.reused.http.response.ResponseHelpers.unauthorized;
import static org.opendatakit.briefcase.ui.SwingTestRig.uncheckedSleep;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JOptionPane;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Before;
import org.junit.Test;
import org.opendatakit.briefcase.matchers.GenericUIMatchers;
import org.opendatakit.briefcase.reused.http.HttpException;

public class AggregateServerDialogTest extends AssertJSwingJUnitTestCase {
  @Override
  protected void onSetUp() {
  }

  @Before
  public void beforeEachTest() {
    // Waiting some millis prevents interference between test methods
    // that assert about dialogs.
    uncheckedSleep(50);
  }

  @Test
  public void locks_the_UI_while_testing_the_server() {
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> {
      uncheckedSleep(150); // Some delay to let junit run the assertions
      return ok("").map(__ -> true);
    });
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    assertThat(page.cancelButton(), is(disabled()));
    assertThat(page.urlField(), is(not(editable())));
    assertThat(page.usernameField(), is(not(editable())));
    assertThat(page.passwordField(), is(not(editable())));
    assertThat(page.connectButton(), is(disabled()));
    assertThat(page.progressBar(), is(visible()));
  }

  @Test
  public void unlocks_the_UI_after_testing_the_server() {
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> ok("").map(__ -> false));
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    assertThat(page.cancelButton(), is(enabled()));
    assertThat(page.urlField(), is(editable()));
    assertThat(page.usernameField(), is(editable()));
    assertThat(page.passwordField(), is(editable()));
    assertThat(page.connectButton(), is(enabled()));
    assertThat(page.progressBar(), is(not(visible())));
  }

  @Test
  public void hides_when_the_configuration_is_ok() {
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> ok("").map(__ -> true));
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    assertThat(page.dialog(), is(not(GenericUIMatchers.visible())));
  }

  @Test
  public void launches_any_defined_callback_when_the_configuration_is_ok() {
    AtomicBoolean triggered = new AtomicBoolean(false);
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> ok("").map(__ -> true));
    page.component.onConnect(server -> triggered.set(true));
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    assertThat(triggered.get(), is(true));
  }

  @Test
  public void shows_error_dialog_when_the_server_responds_with_a_302() {
    // HTTP 302 Found means that the server is redirecting the request somewhere else
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> found());
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    JOptionPane errorDialog = page.errorDialog();
    assertThat(errorDialog, is(errorDialog()));
    assertThat(errorDialog, containsMessage("Redirection detected"));
  }

  @Test
  public void shows_error_dialog_when_the_server_responds_with_a_401() {
    // HTTP 401 Unauthorized means that credentials are required or that the provided credentials are wrong
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> unauthorized());
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    JOptionPane errorDialog = page.errorDialog();
    assertThat(errorDialog, is(errorDialog()));
    assertThat(errorDialog, containsMessage("Wrong credentials"));
  }

  @Test
  public void shows_error_dialog_when_the_server_responds_with_a_404() {
    // HTTP 404 Not Found means that the user has configured the wrong URL
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> notFound());
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    JOptionPane errorDialog = page.errorDialog();
    assertThat(errorDialog, is(errorDialog()));
    assertThat(errorDialog, containsMessage("Aggregate not found"));
  }

  @Test
  public void shows_error_dialog_when_an_http_exception_is_catched() {
    String expectedError = "Unknown host";
    AggregateServerDialogPageObject page = AggregateServerDialogPageObject.setUp(robot(), server -> {
      throw new HttpException(expectedError);
    });
    page.show();
    page.fillForm("http://it.does.not.matter");
    page.clickOnConnect();
    JOptionPane errorDialog = page.errorDialog();
    assertThat(errorDialog, is(errorDialog()));
    assertThat(errorDialog, containsMessage(expectedError));
  }
}
