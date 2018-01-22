package org.opendatakit.briefcase.ui.matchers;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class SwingMatchers {

  public static Matcher<JComponent> visible() {
    return new TypeSafeMatcher<JComponent>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("visible");
      }

      @Override
      protected boolean matchesSafely(JComponent item) {
        return item != null && item.isVisible();
      }
    };
  }


  public static Matcher<JTextComponent> empty() {
    return new TypeSafeMatcher<JTextComponent>() {
      @Override
      protected boolean matchesSafely(JTextComponent item) {
        return item != null && item.getText().isEmpty();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("empty");
      }

      @Override
      protected void describeMismatchSafely(JTextComponent item, Description mismatchDescription) {
        mismatchDescription.appendText("not empty: \"" + item.getText() + "\"");
      }
    };
  }

  public static Matcher<JComponent> enabled() {
    return new TypeSafeMatcher<JComponent>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("enabled");
      }

      @Override
      protected boolean matchesSafely(JComponent item) {
        return item != null && item.isEnabled();
      }
    };
  }


}
