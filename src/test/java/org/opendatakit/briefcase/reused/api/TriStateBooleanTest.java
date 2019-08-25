package org.opendatakit.briefcase.reused.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.api.TriStateBoolean.FALSE;
import static org.opendatakit.briefcase.reused.api.TriStateBoolean.TRUE;
import static org.opendatakit.briefcase.reused.api.TriStateBoolean.UNDETERMINED;
import static org.opendatakit.briefcase.reused.api.TriStateBoolean.from;

import org.junit.Test;

public class TriStateBooleanTest {
  @Test
  public void it_parses_the_names_of_its_members_like_valueof() {
    assertThat(from(UNDETERMINED.name()), is(UNDETERMINED));
    assertThat(from(TRUE.name()), is(TRUE));
    assertThat(from(FALSE.name()), is(FALSE));
  }

  @Test
  public void it_parses_legacy_trueish_values() {
    // These values come from old enums we used before normalizing tristate booleans
    assertThat(from("INHERIT"), is(UNDETERMINED));
    assertThat(from("PULL"), is(TRUE));
    assertThat(from("DONT_PULL"), is(FALSE));
    assertThat(from("EXPORT_MEDIA"), is(TRUE));
    assertThat(from("DONT_EXPORT_MEDIA"), is(FALSE));
  }
}
