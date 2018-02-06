package org.opendatakit.briefcase.ui.matchers;

import com.github.lgooddatepicker.components.DatePicker;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DatePickerMatchers {
  public static Matcher<DatePicker> empty() {
    return new TypeSafeMatcher<DatePicker>() {
      @Override
      protected boolean matchesSafely(DatePicker item) {
        return item != null && item.getDate() == null;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("empty");
      }
    };
  }
}
