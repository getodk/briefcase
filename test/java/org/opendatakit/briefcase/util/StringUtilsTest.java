package org.opendatakit.briefcase.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

  @Test
  public void stripIllegalChars_shouldReturnNullForNullInput() {
    Assert.assertNull(StringUtils.stripIllegalChars(null));
  }

  @Test
  public void stripIllegalChars_shouldRemovePunctuationChars() {
    String input = "abcdef.:;";
    String output = StringUtils.stripIllegalChars(input);

    Pattern pattern = Pattern.compile("\\p{Punct}");
    Matcher matcher = pattern.matcher(output);
    Assert.assertTrue(!matcher.matches());
    Assert.assertEquals("abcdef___", output);
  }

  @Test
  public void stripIllegalChars_shouldRemoveDirectorySeparatorChars() {
    String input = "a\\b/c";
    String output = StringUtils.stripIllegalChars(input);
    Assert.assertEquals("a_b_c", output);
  }

  @Test
  public void stripIllegalChars_shouldReplaceWhitespaceWithSpace() {
    String input = "abcdef\t\n\r";
    String output = StringUtils.stripIllegalChars(input);

    Pattern pattern = Pattern.compile("[\\t\\n\\x0B\\f\\r]");
    Matcher matcher = pattern.matcher(output);
    Assert.assertTrue(String.format("Input: %s, output: %s", input, output), !matcher.matches());
    Assert.assertEquals("abcdef   ", output);
  }

  @Test
  public void stripIllegalChars_shouldWorkWithUnicodeCharacters() {
    String input = ".:;%&シャンプー \n\r";
    String output = StringUtils.stripIllegalChars(input);

    Pattern pattern = Pattern.compile("\\p{Punct}");
    Matcher matcher = pattern.matcher(output);
    Assert.assertTrue(String.format("Input: %s, output: %s", input, output), !matcher.matches());

    pattern = Pattern.compile("[\\t\\n\\x0B\\f\\r]");
    matcher = pattern.matcher(output);
    Assert.assertTrue(String.format("Input: %s, output: %s", input, output), !matcher.matches());

    Assert.assertEquals("_____シャンプー   ", output);
  }

}
