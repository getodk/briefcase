package org.opendatakit.briefcase.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class StringUtilsTest {
  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public String input;

  @Parameterized.Parameter(value = 2)
  public String expectedOutput;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"null input produces null output", null, null},
        {"Common punctuation chars get replaced by underscore", "abcdef.:;", "abcdef___"},
        {"Windows and *nix dir separators get replaced by underscore", "a\\b/c", "a_b_c"},
        {"Whitespace chars get normalized", "abcdef\t\n\r ", "abcdef    "},
        {"Unicode chars should get through", "シャンプー", "シャンプー"},
    });
  }

  @Test
  public void test_stripIllegalChars() {
    assertThat(stripIllegalChars(input), expectedOutput == null ? is(nullValue()) : is(expectedOutput));
  }
}
