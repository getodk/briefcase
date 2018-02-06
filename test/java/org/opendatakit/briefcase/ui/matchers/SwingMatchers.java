/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase.ui.matchers;

import com.github.lgooddatepicker.components.DatePicker;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class SwingMatchers {

  public static Matcher<JCheckBox> selected() {
    return new TypeSafeMatcher<JCheckBox>() {
      @Override
      protected boolean matchesSafely(JCheckBox item) {
        return item != null && item.isSelected();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("selected");
      }
    };
  }

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

  public static Matcher<JTextComponent> contains(String expectedText) {
    return new TypeSafeMatcher<JTextComponent>() {
      @Override
      protected boolean matchesSafely(JTextComponent item) {
        return item != null && item.getText().contains(expectedText);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("contains " + expectedText);
      }

      @Override
      protected void describeMismatchSafely(JTextComponent item, Description mismatchDescription) {
        mismatchDescription.appendText("actual value '" + item.getText() + "' doesn't contain '" + expectedText + "'");
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
