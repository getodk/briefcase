package org.opendatakit.briefcase.delivery.ui.reused.validation;

import static java.awt.Color.BLACK;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.delivery.ui.reused.validation.UiFieldValidator.COLOR_NOT_VALID;
import static org.opendatakit.briefcase.delivery.ui.reused.validation.UiFieldValidator.COLOR_VALID;
import static org.opendatakit.briefcase.delivery.ui.reused.validation.UiFieldValidator.REQUIRED;

import java.awt.event.KeyEvent;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.Before;
import org.junit.Test;

public class UiFieldValidatorTest {

  private JTextField field;
  private JLabel label;

  @Before
  public void setUp() {
    field = new JTextField();
    label = new JLabel();
    label.setForeground(BLACK);
  }

  @Test
  public void knows_when_a_field_goes_dirty() {
    UiFieldValidator validator = UiFieldValidator.of(field, label);

    // Fields are not dirty by default
    simulateKeyStroke(field);
    assertThat(validator.isDirty(), is(false));

    // Fields get dirty when they get a value for the first time
    field.setText("bar");
    simulateKeyStroke(field);
    assertThat(validator.isDirty(), is(true));

    // Clearing out the field won't make it less dirty
    field.setText("");
    simulateKeyStroke(field);
    assertThat(validator.isDirty(), is(true));
  }

  @Test
  public void invalid_pristine_fields_wont_have_a_red_label() {
    UiFieldValidator validator = UiFieldValidator.of(field, label, s -> s.equals("foo"));

    simulateKeyStroke(field);
    assertThat(validator.isDirty(), is(false)); // aka "pristine"
    assertThat(validator.isValid(), is(false));
    assertThat(label.getForeground(), is(COLOR_VALID));
  }

  @Test
  public void invalid_dirty_fields_have_a_red_label() {
    UiFieldValidator validator = UiFieldValidator.of(field, label, s -> s.equals("foo"));

    field.setText("bar");
    simulateKeyStroke(field);
    assertThat(validator.isDirty(), is(true));
    assertThat(validator.isValid(), is(false));
    assertThat(label.getForeground(), is(COLOR_NOT_VALID));
  }

  @Test
  public void valid_dirty_fields_have_a_black_label() {
    UiFieldValidator validator = UiFieldValidator.of(field, label, s -> s.equals("foo"));

    field.setText("foo");
    simulateKeyStroke(field);
    assertThat(validator.isDirty(), is(true));
    assertThat(validator.isValid(), is(true));
    assertThat(label.getForeground(), is(COLOR_VALID));
  }

  @Test
  public void calls_a_callback_each_time_there_is_a_change() {
    AtomicInteger changeCallbackCalledTimes = new AtomicInteger(0);
    JTextField field = new JTextField();
    JLabel label = new JLabel();
    label.setForeground(BLACK);
    UiFieldValidator validator = UiFieldValidator.of(field, label, s -> s.equals("foo")).onChange(changeCallbackCalledTimes::getAndIncrement);

    simulateKeyStroke(field);
    assertThat(validator.isValid(), is(false));
    assertThat(changeCallbackCalledTimes.get(), is(0)); // no change in internal status yet

    field.setText("bar");
    simulateKeyStroke(field);
    assertThat(validator.isValid(), is(false));
    assertThat(changeCallbackCalledTimes.get(), is(1)); // goes from "pristine" to "dirty"

    field.setText("foo");
    simulateKeyStroke(field);
    assertThat(validator.isValid(), is(true));
    assertThat(changeCallbackCalledTimes.get(), is(2)); // goes from "invalid" to "valid"

    field.setText("bar");
    simulateKeyStroke(field);
    assertThat(validator.isValid(), is(false));
    assertThat(changeCallbackCalledTimes.get(), is(3)); // goes from "valid" to "invalid"
  }

  @Test
  public void not_REQUIRED_fields_should_be_valid_by_default() {
    // i.e. even if the user doesn't input any value on those fields
    assertThat(UiFieldValidator.of(field, label, REQUIRED).isValid(), is(false));
    assertThat(UiFieldValidator.of(field, label).isValid(), is(true));
  }

  private void simulateKeyStroke(JTextField field) {
    field.getKeyListeners()[0].keyReleased(new KeyEvent(field, KEY_RELEASED, OffsetDateTime.now().toInstant().toEpochMilli(), 0, 65, 'a', 1));
  }
}
