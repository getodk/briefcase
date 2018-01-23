package org.opendatakit.briefcase.ui.export;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Test;
import org.opendatakit.briefcase.export.ExportConfiguration;

public class ExportConfigurationTest {
  @Test
  public void can_produce_a_new_configuration_that_falls_back_to_another_configuration() {
    ExportConfiguration baseConf = new ExportConfiguration(
        Optional.of(Paths.get("/some/path")),
        Optional.of(Paths.get("/some/file.pem")),
        Optional.of(LocalDate.of(2018, 1, 1)),
        Optional.of(LocalDate.of(2020, 1, 1))
    );

    ExportConfiguration testConf = ExportConfiguration.empty();
    assertThat(testConf.fallingBackTo(baseConf), is(baseConf));

    testConf.setExportDir(Paths.get("/some/other/path"));
    assertThat(testConf.fallingBackTo(baseConf).getExportDir(), isPresentAndIs(Paths.get("/some/other/path")));
    assertThat(testConf.fallingBackTo(baseConf).getPemFile(), is(baseConf.getPemFile()));
    assertThat(testConf.fallingBackTo(baseConf).getStartDate(), is(baseConf.getStartDate()));
    assertThat(testConf.fallingBackTo(baseConf).getEndDate(), is(baseConf.getEndDate()));

    testConf.setPemFile(Paths.get("/some/other/file.pem"));
    assertThat(testConf.fallingBackTo(baseConf).getPemFile(), isPresentAndIs(Paths.get("/some/other/file.pem")));
    assertThat(testConf.fallingBackTo(baseConf).getStartDate(), is(baseConf.getStartDate()));
    assertThat(testConf.fallingBackTo(baseConf).getEndDate(), is(baseConf.getEndDate()));

    testConf.setStartDate(LocalDate.of(2019, 1, 1));
    assertThat(testConf.fallingBackTo(baseConf).getStartDate(), isPresentAndIs(LocalDate.of(2019, 1, 1)));
    assertThat(testConf.fallingBackTo(baseConf).getEndDate(), is(baseConf.getEndDate()));

    testConf.setEndDate(LocalDate.of(2021, 1, 1));
    assertThat(testConf.fallingBackTo(baseConf), is(testConf));
  }
}