package org.opendatakit.briefcase.reused.model.transfer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.reused.model.transfer.CentralServer.cleanUrl;

import org.junit.Test;

public class CentralServerTest {
  @Test
  public void knows_how_to_clean_copied_and_pasted_Aggregate_URLs_from_a_browser() {
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org/"),
        is("https://sandbox.central.opendatakit.org/")
    );
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org"),
        is("https://sandbox.central.opendatakit.org")
    );
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org/#/"),
        is("https://sandbox.central.opendatakit.org")
    );
    assertThat(
        cleanUrl("https://sandbox.central.opendatakit.org/#/projects/8"),
        is("https://sandbox.central.opendatakit.org")
    );
  }
}
