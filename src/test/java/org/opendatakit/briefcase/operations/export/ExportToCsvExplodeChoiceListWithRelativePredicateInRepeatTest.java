package org.opendatakit.briefcase.operations.export;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportToCsvExplodeChoiceListWithRelativePredicateInRepeatTest {
  private ExportToCsvScenario scenario;

  @Before
  public void setUp() {
    scenario = ExportToCsvScenario.setUp("choice-lists-predicate-relative-repeat");
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exports_forms_with_all_data_types() {
    scenario.runExportExplodedChoiceLists();
    scenario.assertSameContent();
    scenario.assertSameContentRepeats("", "group");
  }
}
