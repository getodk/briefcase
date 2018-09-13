package org.opendatakit.common.cli;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import org.junit.Test;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class ParamTest {

  @Test
  public void knows_if_it_is_a_flag_or_an_arg() {
    Param<String> someArg = Param.arg("short", "long", "some description");
    assertThat(someArg.isArg(), is(true));
    assertThat(someArg.isFlag(), is(false));

    Param<Void> flag = Param.flag("short", "long", "some description");
    assertThat(flag.isArg(), is(false));
    assertThat(flag.isFlag(), is(true));
  }

  @Test
  public void it_has_a_factory_for_iso8601_date_args() {
    Param<LocalDate> localDate = Param.localDate("start", "export_start_date", "Export start date");
    assertThat(localDate.map("2018-01-20"), is(LocalDate.of(2018, 1, 20)));
  }

  @Test(expected = BriefcaseException.class)
  public void throws_when_invalid_input_dates_are_received() {
    Param<LocalDate> localDate = Param.localDate("start", "export_start_date", "Export start date");
    localDate.map("2018.01.20");
  }

  @Test
  public void normalizes_input_slashes_to_hyphens() {
    Param<LocalDate> localDate = Param.localDate("start", "export_start_date", "Export start date");
    assertThat(localDate.map("2018/01/20"), is(LocalDate.of(2018, 1, 20)));
  }
}