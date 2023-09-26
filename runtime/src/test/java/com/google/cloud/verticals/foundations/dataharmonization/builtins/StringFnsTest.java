// Copyright 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.builtins.StringFns.split;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.StringFns.strJoin;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.StringFns.toLower;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.StringFns.toUpper;
import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockClosure;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for StringFns. */
@RunWith(JUnit4.class)
public class StringFnsTest {

  @Test
  public void split_noDelimiterMatches() {
    String inputString = "no/delimiter/here";
    String delimiter = ",";
    Array expected = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(inputString)));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(1, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void split_singleDelimiterMatch() {
    String inputString = "one/delimiter";
    String delimiter = "/";
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(testDTI().primitiveOf("one"), testDTI().primitiveOf("delimiter")));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(2, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void split_multipleDelimiterMatches() {
    String inputString = " a, number,of,different,delimiters";
    String delimiter = ",";
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(" a"),
                    testDTI().primitiveOf(" number"),
                    testDTI().primitiveOf("of"),
                    testDTI().primitiveOf("different"),
                    testDTI().primitiveOf("delimiters")));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(5, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void split_emptySubstrings() {
    String inputString = "some,,empty,,,substrings,";
    String delimiter = ",";
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf("some"),
                    testDTI().primitiveOf(""),
                    testDTI().primitiveOf("empty"),
                    testDTI().primitiveOf(""),
                    testDTI().primitiveOf(""),
                    testDTI().primitiveOf("substrings"),
                    testDTI().primitiveOf("")));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(7, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void split_trailingWhitespace() {
    String inputString = "trailing, ";
    String delimiter = ",";
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(testDTI().primitiveOf("trailing"), testDTI().primitiveOf(" ")));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(2, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void split_emptyInputString() {
    String inputString = "";
    String delimiter = ",";
    Array expected = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(inputString)));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(1, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void splitToIndividualChar_nonEmpty_returnsArrOfChar() {
    String inputString = "abc  d";
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf("a"),
                    testDTI().primitiveOf("b"),
                    testDTI().primitiveOf("c"),
                    testDTI().primitiveOf(" "),
                    testDTI().primitiveOf(" "),
                    testDTI().primitiveOf("d")));

    Array splitResult = split(new TestContext(), inputString);

    AssertUtil.assertDCAPEquals(expected, splitResult);
  }

  @Test
  public void split_emptyString_returnIndividualChar() {
    String inputString = "";
    Array expected = testDTI().arrayOf();

    Array splitResult = split(new TestContext(), inputString);

    AssertUtil.assertDCAPEquals(expected, splitResult);
  }

  @Test
  public void split_emptyDelimiter_returnsArrayOfIndividualChars() {
    String inputString = "  ab";
    String delimiter = "";
    Array expected =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf(" "),
                testDTI().primitiveOf(" "),
                testDTI().primitiveOf("a"),
                testDTI().primitiveOf("b"));
    Array splitResult = split(new TestContext(), inputString, delimiter);

    AssertUtil.assertDCAPEquals(expected, splitResult);
  }

  @Test
  public void split_multiCharacterDelimiter() {
    String inputString = "somestringhere";
    String delimiter = "ing";
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(testDTI().primitiveOf("somestr"), testDTI().primitiveOf("here")));

    Array splitResult = split(new TestContext(), inputString, delimiter);

    assertEquals(2, splitResult.size());
    assertEquals(expected, splitResult);
  }

  @Test
  public void toUpper_emptyString() {
    Primitive result = toUpper(new TestContext(), "");
    assertEquals(testDTI().primitiveOf(""), result);
  }

  @Test
  public void toUpper_mixedString() {
    Primitive result = toUpper(new TestContext(), "123 HapPy HalLoween!");
    assertEquals(testDTI().primitiveOf("123 HAPPY HALLOWEEN!"), result);
  }

  @Test
  public void toLower_emptyString() {
    Primitive result = toLower(new TestContext(), "");
    assertEquals(testDTI().primitiveOf(""), result);
  }

  @Test
  public void toLower_mixedString() {
    Primitive result = toLower(new TestContext(), "123 HapPy HalLoween!");
    assertEquals(testDTI().primitiveOf("123 happy halloween!"), result);
  }
  @Test
  public void join_emptyArray() {
    Primitive result = strJoin(new TestContext(), ", ", testDTI().arrayOf(ImmutableList.of()));
    AssertUtil.assertDCAPEquals(testDTI().primitiveOf(""), result);
  }

  @Test
  public void join_stringArray() {
    Array input =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf("a"), testDTI().primitiveOf("b"), testDTI().primitiveOf("c"));
    Primitive result = strJoin(new TestContext(), ", ", input);
    AssertUtil.assertDCAPEquals(testDTI().primitiveOf("a, b, c"), result);
  }

  @Test
  public void join_mixedArray() {
    Array input =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf(1.0),
                testDTI().primitiveOf("a"),
                testDTI().primitiveOf(false),
                NullData.instance,
                testDTI().arrayOf(testDTI().primitiveOf("foo")),
                testDTI().containerOf(ImmutableMap.of("f1key", testDTI().primitiveOf("f1val"))));
    Primitive result = strJoin(new TestContext(), ", ", input);
    AssertUtil.assertDCAPEquals(
        testDTI().primitiveOf("1, a, false, , [foo], {f1key=f1val}"), result);
  }

  @Test
  public void matchesRegex_matches() {
    String inputString = "123";
    String patternString = "\\d+";
    assertTrue(StringFns.matchesRegex(new TestContext(), inputString, patternString).bool());
  }

  @Test
  public void matchesRegex_notMatches() {
    String inputString = "abc";
    String patternString = "\\d+";
    assertFalse(StringFns.matchesRegex(new TestContext(), inputString, patternString).bool());
  }

  @Test
  public void matchesRegex_null_notMatches() {
    String inputString = null;
    String patternString = "\\d+";
    assertFalse(StringFns.matchesRegex(new TestContext(), inputString, patternString).bool());
  }

  @Test
  public void extractRegex_oneMatch() {
    String input = "123abcd";
    String patternString = "b.{2}";
    Primitive expected = testDTI().primitiveOf("bcd");
    assertEquals(expected, StringFns.extractRegex(new TestContext(), input, patternString));
  }

  @Test
  public void extractRegex_multipleMatch_returnFirstOne() {
    String input = "abcd \n 123 efgh 456";
    String patternString = "\\d+";
    Primitive expected = testDTI().primitiveOf("123");
    assertEquals(expected, StringFns.extractRegex(new TestContext(), input, patternString));
  }

  @Test
  public void extractRegex_noMatch_returnNullData() {
    String input = "abcdefg";
    String patternString = "\\d+";
    Primitive expected = NullData.instance;
    assertEquals(expected, StringFns.extractRegex(new TestContext(), input, patternString));
  }

  @Test
  public void extractRegex_inputNull_returnNullData() {
    String input = null;
    String patternString = "\\d+";
    Primitive expected = NullData.instance;
    assertEquals(expected, StringFns.extractRegex(new TestContext(), input, patternString));
  }

  @Test
  public void extractRegex_patternNull_returnNullData() {
    String input = "abcd";
    String patternString = null;
    Primitive expected = NullData.instance;
    assertEquals(expected, StringFns.extractRegex(new TestContext(), input, patternString));
  }

  @Test
  public void extractRegexFormatted_emptyPattern_returnNullData() {
    String input = "abcdefg";
    String patternString = "";
    Array expected = NullData.instance;
    assertEquals(
        expected,
        StringFns.extractRegex(new TestContext(), input, patternString, MockClosure.noop()));
  }

  @Test
  public void extractRegexFormatted_noMatch_returnNullData() {
    String input = "abcdefg";
    String patternString = "\\d+";
    Array expected = NullData.instance;
    assertEquals(
        expected,
        StringFns.extractRegex(new TestContext(), input, patternString, MockClosure.noop()));
  }

  @Test
  public void extractRegexFormatted_oneMatch() {
    String input = "123abcd";
    String patternString = "b.{2}";
    Array expected =
        testDTI()
            .arrayOf(
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            "zero", testDTI().primitiveOf("bcd"), "one", NullData.instance)));
    Array got =
        StringFns.extractRegex(
            new TestContext(),
            input,
            patternString,
            new MockClosure(
                1,
                (args, ctx) ->
                    testDTI()
                        .containerOf(
                            ImmutableMap.of("zero", args.get(0).asContainer().getField("0")))));
    assertEquals(expected, got);
  }

  @Test
  public void extractRegexFormatted_unmatchedGroup() {
    String input = "123abcd";
    String patternString = "b.{2}(x)?";
    Array expected =
        testDTI()
            .arrayOf(testDTI().containerOf(ImmutableMap.of("zero", testDTI().primitiveOf("bcd"))));
    Array got =
        StringFns.extractRegex(
            new TestContext(),
            input,
            patternString,
            new MockClosure(
                1,
                (args, ctx) ->
                    testDTI()
                        .containerOf(
                            ImmutableMap.of(
                                "zero",
                                args.get(0).asContainer().getField("0"),
                                "one",
                                args.get(0).asContainer().getField("1")))));
    assertEquals(expected, got);
  }

  @Test
  public void extractRegexFormatted_emptyStringGroup() {
    String input = "123abcd";
    String patternString = "b.{2}(x*)";
    Array expected =
        testDTI()
            .arrayOf(
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            "zero", testDTI().primitiveOf("bcd"), "one", NullData.instance)));
    Array got =
        StringFns.extractRegex(
            new TestContext(),
            input,
            patternString,
            new MockClosure(
                1,
                (args, ctx) ->
                    testDTI()
                        .containerOf(
                            ImmutableMap.of(
                                "zero",
                                args.get(0).asContainer().getField("0"),
                                "one",
                                args.get(0).asContainer().getField("1")))));
    assertEquals(expected, got);
  }

  @Test
  public void extractRegexFormatted_multipleMatches() {
    String input = "123abcdaaabxy";
    String patternString = "b(.{2})";
    Array expected =
        testDTI()
            .arrayOf(
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            "zero",
                            testDTI().primitiveOf("bcd"),
                            "one",
                            testDTI().primitiveOf("cd"))),
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            "zero",
                            testDTI().primitiveOf("bxy"),
                            "one",
                            testDTI().primitiveOf("xy"))));
    Array got =
        StringFns.extractRegex(
            new TestContext(),
            input,
            patternString,
            new MockClosure(
                1,
                (args, ctx) ->
                    testDTI()
                        .containerOf(
                            ImmutableMap.of(
                                "zero",
                                args.get(0).asContainer().getField("0"),
                                "one",
                                args.get(0).asContainer().getField("1")))));
    assertEquals(expected, got);
  }

  @Test
  public void serializeJson_valid() {
    Container container =
        testDTI().containerOf(ImmutableMap.of("test", testDTI().primitiveOf("val")));
    assertEquals(
        "{\"test\":\"val\"}", StringFns.serializeJson(new TestContext(), container).string());
  }

  @Test
  public void deserializeJson_valid() {
    Container container = testDTI().emptyContainer();
    container = container.setField("test", testDTI().primitiveOf("val"));
    assertEquals(container, StringFns.deserializeJson("{\"test\":\"val\"}"));
  }

  @Test
  public void base64encode_emptyString() {
    String input = "";
    String b64 = StringFns.base64encode(new TestContext(), input).string();
    assertThat(b64).isEmpty();
  }

  @Test
  public void base64decode_emptyString() {
    String b64 = "";
    String output = StringFns.base64decode(new TestContext(), b64).string();
    assertThat(output).isEmpty();
  }

  @Test
  public void base64encode_nullString() {
    String b64 = StringFns.base64encode(new TestContext(), null).string();
    assertThat(b64).isEmpty();
  }

  @Test
  public void base64decode_nullString() {
    String output = StringFns.base64decode(new TestContext(), null).string();
    assertThat(output).isEmpty();
  }

  @Test
  public void base64encode_someString() {
    String input = "Hello\n\uD83E\uDDD0";
    String b64 = StringFns.base64encode(new TestContext(), input).string();
    assertThat(b64).isEqualTo("SGVsbG8K8J+nkA==");
  }

  @Test
  public void base64decode_someString() {
    String b64 = "SGVsbG8K8J+nkA==";
    String output = StringFns.base64decode(new TestContext(), b64).string();
    assertThat(output).isEqualTo("Hello\n\uD83E\uDDD0");
  }

  @Test
  public void explicitEmptyString_valid() {
    Primitive empty = StringFns.explicitEmptyString(new TestContext());
    assertThat(empty.string()).isEmpty();
    assertThat(empty.isNullOrEmpty()).isFalse();
    assertThat(empty.bool()).isNull();
    assertThat(empty.num()).isNull();
    assertThat(empty.deepCopy()).isEqualTo(empty);
  }
}
