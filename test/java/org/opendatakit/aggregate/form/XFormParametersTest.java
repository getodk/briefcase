package org.opendatakit.aggregate.form;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class XFormParametersTest {
  XFormParameters aa = new XFormParameters("a", "a");
  XFormParameters ab = new XFormParameters("a", "b");
  XFormParameters aNull = new XFormParameters("a", null);
  XFormParameters aEmpty = new XFormParameters("a", "");
  XFormParameters ba = new XFormParameters("b", "a");

  @Test
  public void compareTo() {
    // aa
    assertThat(aa.compareTo(aa), is(0));

    assertThat(aa.compareTo(ab), is(-1));
    assertThat(ab.compareTo(aa), is(1));

    assertThat(aa.compareTo(aNull), is(-1));
    assertThat(aNull.compareTo(aa), is(1));

    assertThat(aa.compareTo(aEmpty), is(-1));
    assertThat(aEmpty.compareTo(aa), is(1));

    assertThat(aa.compareTo(ba), is(-1));
    assertThat(ba.compareTo(aa), is(1));

    // ab
    assertThat(ab.compareTo(ab), is(0));

    assertThat(ab.compareTo(aNull), is(-1));
    assertThat(aNull.compareTo(ab), is(1));

    assertThat(ab.compareTo(aEmpty), is(-1));
    assertThat(aEmpty.compareTo(ab), is(1));

    assertThat(ab.compareTo(ba), is(-1));
    assertThat(ba.compareTo(ab), is(1));

    // aNull
    assertThat(aNull.compareTo(aNull), is(0));

    assertThat(aNull.compareTo(aEmpty), is(0));
    assertThat(aEmpty.compareTo(aNull), is(0));

    assertThat(aNull.compareTo(ba), is(-1));
    assertThat(ba.compareTo(aNull), is(1));

    // aEmpty
    assertThat(aEmpty.compareTo(aEmpty), is(0));

    assertThat(aEmpty.compareTo(ba), is(-1));
    assertThat(ba.compareTo(aEmpty), is(1));

    // ba
    assertThat(ba.compareTo(ba), is(0));
  }
}