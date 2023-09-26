/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link TranspilerHelper#preprocessString(String)}. */
@RunWith(Parameterized.class)
public class PreprocessStringTest {
  private final String input;
  private final String expected;

  public PreprocessStringTest(String input, String expected) {
    this.input = input;
    this.expected = expected;
  }

  @Parameters(name = "preprocessString - {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"", ""},
          {"''", ""},
          {"one", "one"},
          {"'one'", "one"},
          {"'one\\'two'", "one'two"},
          {"'one.two'", "one\\.two"},
          {"'one\\\\two'", "one\\\\two"},
          {"'one two'", "one two"},
          {"'one\\xtwo'", "onextwo"},
          {"'one\\'two\\'three'", "one'two'three"},
          {"one\\ two", "one two"},
          {"one\\.two", "one\\.two"},
          {"one\\[two", "one\\[two"},
          {"one\\]two", "one\\]two"},
          {"one\\-two", "one-two"},
          {"one\\-two\\-three", "one-two-three"},
          {"\"\\x\\y\\z\\\"xyz\"", "xyz\"xyz"},
          {"\"\\n\\r\\t\\\\\"", "\n\r\t\\"},
          {"\"\\\\\\\\nope\"", "\\\\nope"}
        });
  }

  @Test
  public void test() {
    String got = TranspilerHelper.preprocessString(input);
    assertEquals(expected, got);
  }
}
