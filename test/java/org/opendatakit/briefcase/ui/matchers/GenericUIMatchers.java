package org.opendatakit.briefcase.ui.matchers;

import javax.swing.JDialog;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class GenericUIMatchers {
  public static Matcher<Object> visible() {
    return new BaseMatcher<Object>() {
      @Override
      public boolean matches(Object item) {
        if (item instanceof DialogFixture)
          return ((DialogFixture) item).target().isVisible();
        if (item instanceof JDialog)
          return ((JDialog) item).isVisible();
        if (item instanceof JFileChooserFixture)
          return ((JFileChooserFixture) item).target().isVisible();
        throw new RuntimeException("Invalid matcher for " + item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("visible");
      }
    };
  }
}
