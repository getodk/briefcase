package org.opendatakit.briefcase.reused.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.model.OverridableBoolean.FALSE;
import static org.opendatakit.briefcase.reused.model.OverridableBoolean.TRUE;
import static org.opendatakit.briefcase.reused.model.OverridableBoolean.empty;
import static org.opendatakit.briefcase.reused.model.OverridableBoolean.from;

import java.util.Arrays;
import org.junit.Test;
import org.opendatakit.briefcase.reused.model.OverridableBoolean;

public class OverridableBooleanTest {

  @Test
  public void returns_a_default_value_if_there_is_no_value() {
    assertThat(empty().get(true), is(true));
  }

  @Test
  public void returns_its_main_value() {
    assertThat(FALSE.get(true), is(false));
  }

  @Test
  public void has_a_factory_from_a_boolean() {
    assertThat(OverridableBoolean.of(true).get(false), is(true));
  }

  @Test
  public void can_change_its_value() {
    assertThat(TRUE.set(false), is(FALSE));
  }

  @Test
  public void resolves_a_value_falling_back_to_a_default() {
    assertThat(empty().resolve(true), is(true));
  }

  @Test
  public void resolves_to_its_main_value_when_it_is_present() {
    assertThat(FALSE.resolve(true), is(false));
  }

  @Test
  public void resolves_to_an_override_value_when_it_is_not_undetermined() {
    assertThat(empty().overrideWith(TriStateBoolean.FALSE).resolve(true), is(false));
    assertThat(TRUE.overrideWith(TriStateBoolean.FALSE).resolve(true), is(false));
    assertThat(FALSE.overrideWith(TriStateBoolean.TRUE).resolve(false), is(true));
  }

  @Test
  public void can_be_serialized_and_deserialized() {
    Arrays.asList(
        empty(),                                            // Serialized as ",UNDETERMINED"
        TRUE,                                               // Serialized as "TRUE,UNDETERMINED"
        FALSE,                                              // Serialized as "FALSE,UNDETERMINED"
        empty().overrideWith(TriStateBoolean.TRUE),         // Serialized as ",TRUE"
        empty().overrideWith(TriStateBoolean.FALSE),        // Serialized as ",FALSE"
        empty().overrideWith(TriStateBoolean.UNDETERMINED), // redundant case: same as empty()
        TRUE.overrideWith(TriStateBoolean.TRUE),            // Serialized as "TRUE,TRUE"
        TRUE.overrideWith(TriStateBoolean.FALSE),           // Serialized as "TRUE,FALSE"
        TRUE.overrideWith(TriStateBoolean.UNDETERMINED),    // Serialized as "TRUE,UNDETERMINED"
        FALSE.overrideWith(TriStateBoolean.TRUE),           // Serialized as "FALSE,TRUE"
        FALSE.overrideWith(TriStateBoolean.FALSE),          // Serialized as "FALSE,FALSE"
        FALSE.overrideWith(TriStateBoolean.UNDETERMINED)    // Serialized as "FALSE,UNDETERMINED"
    ).forEach(value -> assertThat(from(value.serialize()), is(value)));
  }

  @Test
  public void knows_when_it_is_empty() {
    Arrays.asList(
        Pair.of(empty(), true), // Only empty cases
        Pair.of(TRUE, false),
        Pair.of(FALSE, false),
        Pair.of(empty().overrideWith(TriStateBoolean.TRUE), false),
        Pair.of(empty().overrideWith(TriStateBoolean.FALSE), false),
        Pair.of(empty().overrideWith(TriStateBoolean.UNDETERMINED), true), // Only empty cases
        Pair.of(TRUE.overrideWith(TriStateBoolean.TRUE), false),
        Pair.of(TRUE.overrideWith(TriStateBoolean.FALSE), false),
        Pair.of(TRUE.overrideWith(TriStateBoolean.UNDETERMINED), false),
        Pair.of(FALSE.overrideWith(TriStateBoolean.TRUE), false),
        Pair.of(FALSE.overrideWith(TriStateBoolean.FALSE), false),
        Pair.of(FALSE.overrideWith(TriStateBoolean.UNDETERMINED), false)
    ).forEach(pair -> assertThat(pair.getLeft().isEmpty(), is(pair.getRight())));
  }

  @Test
  public void can_fall_back_to_another_instance() {
    assertThat(from(",TRUE").fallingBackTo(from("FALSE,UNDETERMINED")), is(from("FALSE,TRUE")));
    assertThat(from("TRUE,UNDETERMINED").fallingBackTo(from(",FALSE")), is(from("TRUE,FALSE")));
  }
}
