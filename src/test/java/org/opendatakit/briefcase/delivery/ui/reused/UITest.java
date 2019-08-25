package org.opendatakit.briefcase.delivery.ui.reused;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.delivery.ui.reused.UI.credentialsFromFields;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.junit.Test;
import org.opendatakit.briefcase.reused.http.Credentials;

public class UITest {
  @Test
  public void builds_optional_credentials_from_UI_fields() {
    assertThat(credentialsFromFields(username(), password()), isEmpty());
    assertThat(credentialsFromFields(username("some user"), password()), isEmpty());
    assertThat(credentialsFromFields(username(), password("some password")), isEmpty());
    assertThat(
        credentialsFromFields(username("some user"), password("some password")),
        isPresentAndIs(Credentials.from("some user", "some password"))
    );
  }

  private JPasswordField password() {
    return new JPasswordField();
  }

  private JPasswordField password(String value) {
    return new JPasswordField(value);
  }

  private JTextField username() {
    return new JTextField();
  }

  private JTextField username(String value) {
    return new JTextField(value);
  }


}
