package org.opendatakit.briefcase.pull.aggregate;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.pull.aggregate.CursorHelpers.buildCursorXml;

import org.junit.Test;

public class CursorTest {
  @Test
  public void knows_how_to_build_cursor_objects_from_a_range_of_specific_formats() {
    assertThat(Cursor.from(buildCursorXml("2019-01-01T00:00:00.000Z")), is(instanceOf(AggregateCursor.class)));
    assertThat(Cursor.from("12345"), is(instanceOf(OnaCursor.class)));
  }

  @Test
  public void falls_back_to_an_opaque_cursor_implementation() {
    assertThat(Cursor.from("some opaque cursor"), is(instanceOf(OpaqueCursor.class)));
  }
}
