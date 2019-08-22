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

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DialogMatchers {

  public static Matcher<JOptionPane> containsMessage(String message) {
    return new TypeSafeMatcher<JOptionPane>() {
      @Override
      protected boolean matchesSafely(JOptionPane item) {
        return extractMessage(item).contains(message);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("contains ").appendValue(message);
      }

      @Override
      protected void describeMismatchSafely(JOptionPane item, Description mismatchDescription) {
        mismatchDescription.appendValue(extractMessage(item)).appendText(" does not contain ").appendValue(message);
      }
    };
  }

  private static String extractMessage(JOptionPane item) {
    return ((JTextArea) ((JScrollPane) item.getMessage()).getViewport().getView()).getText();
  }

  public static Matcher<JOptionPane> errorDialog() {
    return buildTypeMatcher(ERROR_MESSAGE, "an error dialog");
  }

  public static Matcher<JOptionPane> warningDialog() {
    return buildTypeMatcher(WARNING_MESSAGE, "a warning dialog");
  }

  private static Matcher<JOptionPane> buildTypeMatcher(int messageTypeCode, String descriptionText) {
    return new TypeSafeMatcher<JOptionPane>() {
      @Override
      protected boolean matchesSafely(JOptionPane item) {
        return item.getMessageType() == messageTypeCode;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(descriptionText);
      }

      @Override
      protected void describeMismatchSafely(JOptionPane item, Description mismatchDescription) {
        mismatchDescription.appendText("is not " + descriptionText);
      }
    };
  }
}
