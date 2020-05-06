package org.opendatakit.briefcase.export;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExportToCsvExplodeRandomizedChoiceListTest {
  private ExportToCsvScenario scenario;

  @Before
  public void setUp() {
    scenario = ExportToCsvScenario.setUp("choice-lists-randomize");
  }

  @After
  public void tearDown() {
    scenario.tearDown();
  }

  @Test
  public void exportWithSplitSelectMultiple_hasStableColumnOrder() {
    scenario.runExportExplodedChoiceLists();
    scenario.assertSameContent();
  }
}
