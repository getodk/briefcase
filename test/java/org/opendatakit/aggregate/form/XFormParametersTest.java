package org.opendatakit.aggregate.form;

import static org.junit.Assert.assertThat;
import static org.opendatakit.aggregate.form.XFormParametersTest.MatcherProvider.EQUAL_TO;
import static org.opendatakit.aggregate.form.XFormParametersTest.MatcherProvider.GREATER_THAN;
import static org.opendatakit.aggregate.form.XFormParametersTest.MatcherProvider.LESS_THAN;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class XFormParametersTest {
  private static final XFormParameters formAVersionA = new XFormParameters("a", "a");
  private static final XFormParameters formAVersionB = new XFormParameters("a", "b");
  private static final XFormParameters formAVersionNull = new XFormParameters("a", null);
  private static final XFormParameters formAVersionEmpty = new XFormParameters("a", "");
  private static final XFormParameters formBVersionA = new XFormParameters("b", "a");

  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public XFormParameters left;

  @Parameterized.Parameter(value = 2)
  public XFormParameters right;

  @Parameterized.Parameter(value = 3)
  // We can't use a method reference with Parameterized members
  // That's why we need the enum
  public MatcherProvider matcherProvider;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"{a, a} is equal to {a, a}", formAVersionA, formAVersionA, EQUAL_TO},
        {"{a, a} is less than {a, a}", formAVersionA, formAVersionB, LESS_THAN},
        {"{a, b} is greater than {a, b}", formAVersionB, formAVersionA, GREATER_THAN},
        {"{a, a} is less than {a, a}", formAVersionA, formAVersionNull, LESS_THAN},
        {"{a, null} is greater than {a, null}", formAVersionNull, formAVersionA, GREATER_THAN},
        {"{a, a} is less than {a, a}", formAVersionA, formAVersionEmpty, LESS_THAN},
        {"{a, empty} is greater than {a, empty}", formAVersionEmpty, formAVersionA, GREATER_THAN},
        {"{a, a} is less than {a, a}", formAVersionA, formBVersionA, LESS_THAN},
        {"{b, a} is greater than {b, a}", formBVersionA, formAVersionA, GREATER_THAN},
        {"{a, b} is equal to {a, b}", formAVersionB, formAVersionB, EQUAL_TO},
        {"{a, b} is less than {a, b}", formAVersionB, formAVersionNull, LESS_THAN},
        {"{a, null} is greater than {a, null}", formAVersionNull, formAVersionB, GREATER_THAN},
        {"{a, b} is less than {a, b}", formAVersionB, formAVersionEmpty, LESS_THAN},
        {"{a, empty} is greater than {a, empty}", formAVersionEmpty, formAVersionB, GREATER_THAN},
        {"{a, b} is less than {a, b}", formAVersionB, formBVersionA, LESS_THAN},
        {"{b, a} is greater than {b, a}", formBVersionA, formAVersionB, GREATER_THAN},
        {"{a, null} is equal to {a, null}", formAVersionNull, formAVersionNull, EQUAL_TO},
        {"{a, null} is equal to {a, null}", formAVersionNull, formAVersionEmpty, EQUAL_TO},
        {"{a, empty} is equal to {a, empty}", formAVersionEmpty, formAVersionNull, EQUAL_TO},
        {"{a, null} is less than {a, null}", formAVersionNull, formBVersionA, LESS_THAN},
        {"{b, a} is greater than {b, a}", formBVersionA, formAVersionNull, GREATER_THAN},
        {"{a, empty} is equal to {a, empty}", formAVersionEmpty, formAVersionEmpty, EQUAL_TO},
        {"{a, empty} is less than {a, empty}", formAVersionEmpty, formBVersionA, LESS_THAN},
        {"{b, a} is greater than {b, a}", formBVersionA, formAVersionEmpty, GREATER_THAN},
        {"{b, a} is equal to {b, a}", formBVersionA, formBVersionA, EQUAL_TO}
    });
  }

  @Test
  public void compareTo() {
    assertThat(left, matcherProvider.apply(right));
  }

  public enum MatcherProvider {
    EQUAL_TO(Matchers::comparesEqualTo),
    GREATER_THAN(Matchers::greaterThan),
    LESS_THAN(Matchers::lessThan);

    private final Function<XFormParameters, Matcher<XFormParameters>> matcherProvider;

    MatcherProvider(Function<XFormParameters, Matcher<XFormParameters>> matcherProvider) {
      this.matcherProvider = matcherProvider;
    }

    public Matcher<XFormParameters> apply(XFormParameters value) {
      return matcherProvider.apply(value);
    }
  }
}
