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
package org.opendatakit.briefcase.matchers;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
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

  public static Matcher<Object> containsText(String expectedText) {
    return new BaseMatcher<Object>() {
      @Override
      public boolean matches(Object item) {
        if (item instanceof JTextField)
          return ((JTextField) item).getText().contains(expectedText);
        if (item instanceof JLabel)
          return ((JLabel) item).getText().contains(expectedText);
        if (item instanceof JTextPane)
          return ((JTextPane) item).getText().contains(expectedText);
        if (item instanceof JCheckBox)
          return ((JCheckBox) item).getText().contains(expectedText);
        throw new RuntimeException("Invalid matcher for " + item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a component containing text ").appendValue(expectedText);
      }

      @Override
      public void describeMismatch(Object item, Description description) {
        if (item instanceof JTextField)
          description.appendText("a component containing text ").appendValue(((JTextField) item).getText());
        if (item instanceof JLabel)
          description.appendText("a component containing text ").appendValue(((JLabel) item).getText());
        if (item instanceof JTextPane)
          description.appendText("a component containing text ").appendValue(((JTextPane) item).getText());
        if (item instanceof JCheckBox)
          description.appendText("a component containing text ").appendValue(((JCheckBox) item).getText());
      }
    };
  }
}
