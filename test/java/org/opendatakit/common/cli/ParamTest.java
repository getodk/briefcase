package org.opendatakit.common.cli;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import org.junit.Test;
import org.opendatakit.briefcase.reused.BriefcaseException;

public class ParamTest {

  @Test(expected = BriefcaseException.class)
  public void throwBriefcaseExceptionForInvalidDateFormat() {
    Param<LocalDate> localDate = Param.localDate("start", "export_start_date", "Export start date");
    localDate.map("2018.01.20");
  }

  @Test
  public void acceptsIso8601Dates() {
    Param<LocalDate> localDate = Param.localDate("start", "export_start_date", "Export start date");
    assertThat(localDate.map("2018-01-20"), is(LocalDate.of(2018, 1, 20)));
  }

  @Test
  public void normalizesInputSlashesToHyphens() {
    Param<LocalDate> localDate = Param.localDate("start", "export_start_date", "Export start date");
    assertThat(localDate.map("2018/01/20"), is(LocalDate.of(2018, 1, 20)));
  }
}