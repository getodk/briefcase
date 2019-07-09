package org.opendatakit.briefcase.ui.reused;

import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.awt.event.KeyEvent;
import java.time.OffsetDateTime;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.Before;
import org.junit.Test;

public class UiLinkedFieldValidatorTest {
  private JTextField fieldA;
  private JTextField fieldB;
  private JLabel labelA;
  private JLabel labelB;

  @Before
  public void setUp() {
    fieldA = new JTextField();
    labelA = new JLabel();
    labelA.setForeground(BLACK);
    fieldB = new JTextField();
    labelB = new JLabel();
    labelB.setForeground(BLACK);
  }


  @Test
  public void linked_fields_must_be_both_empty_or_both_filled_to_be_valid() {
    UiFieldValidator validatorA = UiFieldValidator.of(fieldA, labelA);
    UiFieldValidator validatorB = UiFieldValidator.of(fieldB, labelB);
    UiLinkedFieldValidator linkedValidator = UiLinkedFieldValidator.of(validatorA, validatorB);

    fieldA.setText("");
    fieldB.setText("");
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);
    assertThat(linkedValidator.isValid(), is(true));

    fieldA.setText("foo");
    fieldB.setText("");
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);
    assertThat(linkedValidator.isValid(), is(false));

    fieldA.setText("");
    fieldB.setText("bar");
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);
    assertThat(linkedValidator.isValid(), is(false));

    fieldA.setText("foo");
    fieldB.setText("bar");
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);
    assertThat(linkedValidator.isValid(), is(true));
  }

  @Test
  public void invalid_linked_fields_get_red_labels() {
    UiFieldValidator validatorA = UiFieldValidator.of(fieldA, labelA);
    UiFieldValidator validatorB = UiFieldValidator.of(fieldB, labelB);
    UiLinkedFieldValidator.of(validatorA, validatorB);

    fieldA.setText("foo");
    fieldB.setText("");
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);
    assertThat(labelA.getForeground(), is(RED));
    assertThat(labelB.getForeground(), is(RED));
  }

  @Test
  public void valid_linked_fields_get_black_labels() {
    UiFieldValidator validatorA = UiFieldValidator.of(fieldA, labelA);
    UiFieldValidator validatorB = UiFieldValidator.of(fieldB, labelB);
    UiLinkedFieldValidator.of(validatorA, validatorB);

    // Produce a RED label situation (see invalid_linked_fields_get_red_labels())
    fieldA.setText("foo");
    fieldB.setText("");
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);

    fieldB.setText("bar");
    simulateKeyStroke(fieldB);
    assertThat(labelA.getForeground(), is(BLACK));
    assertThat(labelB.getForeground(), is(BLACK));
  }

  @Test
  public void linked_fields_validator_has_lower_precedence_over_field_validators() {
    // fieldA will only be valid if it contains "foo"
    UiFieldValidator validatorA = UiFieldValidator.of(fieldA, labelA, s -> s.equals("foo"));
    // fieldB will only be valid if it contains "bar"
    UiFieldValidator validatorB = UiFieldValidator.of(fieldB, labelB, s -> s.equals("bar"));
    UiLinkedFieldValidator.of(validatorA, validatorB);

    fieldA.setText("foo");
    fieldB.setText("baz"); // This value is not valid
    simulateKeyStroke(fieldA);
    simulateKeyStroke(fieldB);
    assertThat(labelA.getForeground(), is(BLACK));
    assertThat(labelB.getForeground(), is(RED));
  }


  private void simulateKeyStroke(JTextField field) {
    field.getKeyListeners()[0].keyReleased(new KeyEvent(field, KEY_RELEASED, OffsetDateTime.now().toInstant().toEpochMilli(), 0, 65, 'a', 1));
  }
}
