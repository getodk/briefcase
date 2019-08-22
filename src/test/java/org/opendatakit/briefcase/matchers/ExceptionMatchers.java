package org.opendatakit.briefcase.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ExceptionMatchers {
  public static TypeSafeMatcher<Runnable> throwsException(Class<? extends Throwable> exceptionClass) {
    return new TypeSafeMatcher<Runnable>() {
      private Throwable actualException;

      @Override
      protected boolean matchesSafely(Runnable item) {
        try {
          item.run();
          return false;
        } catch (Throwable t) {
          actualException = t;
          return t.getClass().equals(exceptionClass);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a runnable that throws " + exceptionClass);
      }

      @Override
      protected void describeMismatchSafely(Runnable item, Description mismatchDescription) {
        if (actualException == null)
          mismatchDescription.appendText("the runnable didn't throw any exception");
        else
          mismatchDescription.appendText("the runnable threw " + actualException.getClass());
      }
    };
  }
}
