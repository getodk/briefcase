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

package org.opendatakit.briefcase.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.util.StringUtils.stripIllegalChars2;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class ExtraStringUtilsTest {
  @Parameterized.Parameter(value = 0)
  public String testCase;

  @Parameterized.Parameter(value = 1)
  public String input;

  @Parameterized.Parameter(value = 2)
  public String expected;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Null character \\u0000", "hello\u0000world", "helloworld"},
        {"Control character \\n", "hello\nworld", "helloworld"},
        {"Restricted code /", "hello/world", "helloworld"},
        {"Restricted code ?", "hello?world", "helloworld"},
        {"Restricted code <", "hello<world", "helloworld"},
        {"Restricted code >", "hello>world", "helloworld"},
        {"Restricted code \\", "hello\\world", "helloworld"},
        {"Restricted code *", "hello*world", "helloworld"},
        {"Restricted code |", "hello|world", "helloworld"},
        {"Restricted code \"", "hello\"world", "helloworld"},
        {"Restricted code :", "hello:world", "helloworld"},
        {"Restricted prefix .", ".hello world", ".hello world"},
        {"Restricted prefix ..", "..hello world", "..hello world"},
        {"Restricted prefix \" \"", " hello world", " hello world"},
        {"Restricted prefix \"  \"", "  hello world", "  hello world"},
        {"Restricted suffix .", "hello world.", "hello world"},
        {"Restricted suffix ..", "hello world..", "hello world"},
        {"Restricted suffix \" \"", "hello world ", "hello world"},
        {"Restricted suffix \"  \"", "hello world  ", "hello world"},
        {"Relative path .", ".", ""},
        {"Relative path ..", "..", ""},
        {"Relative path ./", "./", ""},
        {"Relative path ../", "../", ""},
        {"Relative path /..", "/..", ""},
        {"Relative path /../", "/../", ""},
        {"Reserved Windows filename CON", "CON", ""},
        {"Reserved Windows filename PRN", "PRN", ""},
        {"Reserved Windows filename AUX", "AUX", ""},
        {"Reserved Windows filename NUL", "NUL", ""},
        {"Reserved Windows filename COM1", "COM1", ""},
        {"Reserved Windows filename COM2", "COM2", ""},
        {"Reserved Windows filename COM3", "COM3", ""},
        {"Reserved Windows filename COM4", "COM4", ""},
        {"Reserved Windows filename COM5", "COM5", ""},
        {"Reserved Windows filename COM6", "COM6", ""},
        {"Reserved Windows filename COM7", "COM7", ""},
        {"Reserved Windows filename COM8", "COM8", ""},
        {"Reserved Windows filename COM9", "COM9", ""},
        {"Reserved Windows filename LPT1", "LPT1", ""},
        {"Reserved Windows filename LPT2", "LPT2", ""},
        {"Reserved Windows filename LPT3", "LPT3", ""},
        {"Reserved Windows filename LPT4", "LPT4", ""},
        {"Reserved Windows filename LPT5", "LPT5", ""},
        {"Reserved Windows filename LPT6", "LPT6", ""},
        {"Reserved Windows filename LPT7", "LPT7", ""},
        {"Reserved Windows filename LPT8", "LPT8", ""},
        {"Reserved Windows filename LPT9", "LPT9", ""},
        {"Reserved Windows filename con", "con", ""},
        {"Reserved Windows filename prn", "prn", ""},
        {"Reserved Windows filename aux", "aux", ""},
        {"Reserved Windows filename nul", "nul", ""},
        {"Reserved Windows filename com1", "com1", ""},
        {"Reserved Windows filename com2", "com2", ""},
        {"Reserved Windows filename com3", "com3", ""},
        {"Reserved Windows filename com4", "com4", ""},
        {"Reserved Windows filename com5", "com5", ""},
        {"Reserved Windows filename com6", "com6", ""},
        {"Reserved Windows filename com7", "com7", ""},
        {"Reserved Windows filename com8", "com8", ""},
        {"Reserved Windows filename com9", "com9", ""},
        {"Reserved Windows filename lpt1", "lpt1", ""},
        {"Reserved Windows filename lpt2", "lpt2", ""},
        {"Reserved Windows filename lpt3", "lpt3", ""},
        {"Reserved Windows filename lpt4", "lpt4", ""},
        {"Reserved Windows filename lpt5", "lpt5", ""},
        {"Reserved Windows filename lpt6", "lpt6", ""},
        {"Reserved Windows filename lpt7", "lpt7", ""},
        {"Reserved Windows filename lpt8", "lpt8", ""},
        {"Reserved Windows filename lpt9", "lpt9", ""},
        {"Supports UTF-8 non-alphanumeric filenames", "უნივერსიტეტის გამოკითხვა", "უნივერსიტეტის გამოკითხვა"},
    });
  }

  @Test
  public void replaces_illegal_chars() {
    assertThat(stripIllegalChars2(input), is(expected));
  }
}
