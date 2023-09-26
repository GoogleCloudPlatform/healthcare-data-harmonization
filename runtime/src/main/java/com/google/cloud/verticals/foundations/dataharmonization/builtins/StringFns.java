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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.arrayOf;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.ExplicitEmptyString;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper functions for strings. */
public final class StringFns {

  /**
   * Splits a string using the provided delimiter. Does not trim empty strings or trailing
   * whitespace characters. When the delimiter is null or empty, returns an array of individual
   * characters.
   *
   * @param str String to split.
   * @param delimiter String to use to split the input string.
   * @return {@link Array} of substrings of the input String which are partitioned by the provided
   *     delimiter.
   */
  @PluginFunction
  public static Array split(RuntimeContext ctx, String str, String delimiter) {
    if (delimiter.isEmpty()) {
      return split(ctx, str);
    }
    List<Data> splitArr = new ArrayList<>();
    Iterable<String> splitIterable = Splitter.on(delimiter).split(str);
    for (String s : splitIterable) {
      splitArr.add(ctx.getDataTypeImplementation().primitiveOf(s));
    }
    return arrayOf(ctx, splitArr.toArray(new Data[0]));
  }

  /**
   * Splits the String {@code str} into individual characters.
   *
   * @param str string to split.
   * @return {@link Array} of individual characters in the input string.
   */
  @PluginFunction
  public static Array split(RuntimeContext ctx, String str) {
    List<Data> chars = new ArrayList<>();
    for (int i = 0; i < str.length(); i++) {
      chars.add(ctx.getDataTypeImplementation().primitiveOf(String.valueOf(str.charAt(i))));
    }
    return ctx.getDataTypeImplementation().arrayOf(chars);
  }

  /**
   * Joins the given {@code Array} using the delimiter. The array is cast to its string expression.
   *
   * @param delimiter String to use to join the given array
   * @param components {@link Array} of data to join.
   * @return the join result as a string {@link Primitive}.
   */
  @PluginFunction
  public static Primitive strJoin(RuntimeContext ctx, String delimiter, Array components) {
    return ctx.getDataTypeImplementation()
        .primitiveOf(components.stream().map(Object::toString).collect(joining(delimiter)));
  }

  /**
   * Formats a string using the given format and arguments.
   *
   * @param format A format string, using {@link String#format(String, Object...)} conventions.
   * @param args Arguments to fill into the placeholders.
   */
  @PluginFunction
  public static Primitive strFmt(RuntimeContext ctx, String format, Data... args) {
    return ctx.getDataTypeImplementation().primitiveOf(String.format(format, (Object[]) args));
  }

  /** Converts all letters in the provided string to upper case. */
  @PluginFunction
  public static Primitive toUpper(RuntimeContext ctx, String str) {
    return ctx.getDataTypeImplementation().primitiveOf(Ascii.toUpperCase(str));
  }

  /** Converts all letters in the provided string to lower case. */
  @PluginFunction
  public static Primitive toLower(RuntimeContext ctx, String str) {
    return ctx.getDataTypeImplementation().primitiveOf(Ascii.toLowerCase(str));
  }

  /**
   * Returns true iff the {@link Primitive} string matches the Primitive string regex pattern.
   *
   * @param str Primitive string input to match.
   * @param regex Primitive string regex pattern.
   * @return Primitive boolean, true iff input string matches input regex pattern
   */
  @PluginFunction
  public static Primitive matchesRegex(RuntimeContext ctx, String str, String regex) {
    if (str == null) {
      return ctx.getDataTypeImplementation().primitiveOf(false); // cannot match null
    }
    return ctx.getDataTypeImplementation().primitiveOf(str.matches(regex));
  }

  /**
   * Returns the substring in {@code input} that first matches {@code pattern}, or null if there's
   * no match for {@code pattern} in {@code input}.
   *
   * @param ctx {@link RuntimeContext} to obtain {@link
   *     com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation} from.
   * @param input string input to match.
   * @param pattern string regex pattern.
   * @return the first match of {@code pattern} in {@code input}. NullData if either input or
   *     pattern is null or there is no match.
   */
  @PluginFunction
  public static Primitive extractRegex(RuntimeContext ctx, String input, String pattern) {
    if (input == null || pattern == null) {
      return NullData.instance;
    }
    Matcher m = Pattern.compile(pattern).matcher(input);
    if (m.find()) {
      return ctx.getDataTypeImplementation().primitiveOf(m.group());
    }
    return NullData.instance;
  }

  /**
   * Finds all matches of the given regex pattern in the given input, and passes each match to the
   * given formatter. Returns a result of the formatters in an array.
   *
   * <p>The parameter passed to formatter - $ - contains each group from the regex as a field. That
   * is, group 0 (the whole match) is $.0, group 1 is $.1, etc. Groups are not accessible by name.
   *
   * <p>Note: If the pattern is an empty string, this method will always return null (i.e. an empty
   * array).
   *
   * <p>Note: Only the last match of a repeated group will be returned.
   *
   * <p>Example:
   *
   * <pre><code>
   * var pattern: "a(b+)(c+)d"
   * var input: "abbcccd______abbbbccccccd"
   *
   * extractRegex(input, pattern, $.1) == ["bb", "bbbb"]
   * extractRegex(input, pattern, $.2) == ["ccc", "cccccc"]
   * extractRegex(input, pattern, {
   *   whole: $.0
   *   bees: $.1
   *   cees: $.2
   * }) == [
   *   {
   *     bees: "bb",
   *     cees: "ccc",
   *     whole: "abbcccd"
   *   },
   *   {
   *     bees: "bbbb",
   *     cees: "cccccc",
   *     whole: "abbbbccccccd"
   *   }
   * ]
   * </code></pre>
   *
   * @param ctx The current RuntimeContext.
   * @param input The input string to match against.
   * @param pattern The regex pattern to match.
   * @param formatter A closure with free variable $, where $ is a container with a field for each
   *     group, as in $.0 containing the whole match (group 0), $.1 containing group 1 (if it exists
   *     in the pattern), etc.
   * @return An array where each element is the result of calling formatter on each match.
   */
  @PluginFunction
  public static Array extractRegex(
      RuntimeContext ctx, String input, String pattern, Closure formatter) {
    if (Strings.isNullOrEmpty(pattern)) {
      return NullData.instance;
    }
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(input);
    DataTypeImplementation dti = ctx.getDataTypeImplementation();
    ImmutableList<Data> matches =
        m.results()
            .map(StringFns::groupNumToMatch)
            .map(
                gnm ->
                    gnm.entrySet().stream()
                        .collect(
                            toImmutableMap(
                                e -> e.getKey().toString(),
                                e -> e.getValue().map(dti::primitiveOf).orElse(NullData.instance))))
            .map(dti::containerOf)
            .map(c -> formatter.bindNextFreeParameter(c).execute(ctx))
            .collect(toImmutableList());

    return dti.arrayOf(matches);
  }

  private static ImmutableMap<Integer, Optional<String>> groupNumToMatch(MatchResult result) {
    ImmutableMap.Builder<Integer, Optional<String>> map = ImmutableMap.builder();
    for (int i = 0; i <= result.groupCount(); i++) {
      map.put(i, Optional.ofNullable(result.group(i)));
    }
    return map.buildOrThrow();
  }

  /**
   * Serializes the {@link Data} input to JSON string.
   *
   * @param data The data to serialize.
   * @return A string representing the JSON serialization of the input data.
   */
  @PluginFunction
  public static Primitive serializeJson(RuntimeContext ctx, Data data) {
    return ctx.getDataTypeImplementation()
        .primitiveOf(JsonSerializerDeserializer.dataToJsonString(data));
  }

  /**
   * Deserializes a JSON string to the Whistle {@code Data} format.
   *
   * <pre><code>
   * var inputJson: "\{\"a\":\"a-value\",\"b\":\"b-value\"\}"
   * deserializeJson(inputJson)
   *
   * // Returns the following:
   * // {
   * //   a: "a-value"
   * //   b: "b-value"
   * // }
   * </code></pre>
   *
   * @param json the JSON {@code string} to deserialize
   * @return the deserialized Whistle {@code Data}
   */
  @PluginFunction
  public static Data deserializeJson(String json) {
    return JsonSerializerDeserializer.jsonToData(json);
  }

  /**
   * Encodes the given {@code String} to UTF-8 bytes and then to base64.
   *
   * <p>An empty string/null input will return an empty string.
   *
   * <pre><code>
   * // Encodes "this is a string" to base64.
   * // After calling `base64encode()`, the value of `string1` is "dGhpcyBpcyBhIHN0cmluZw==".
   * string1: base64encode("this is a string")
   *
   * var data1: {
   *     array1: [1, 2, 3]
   *     number1: 123
   *     nested: {
   *         number2: 456
   *     }
   * }
   *
   * // Encodes the structured `data1` {@code Container}, then serializes it to JSON.
   * // After calling `base64encode()`, the value of `structure_enc` is
   * // "eyJhcnJheSI6WzEsMiwzXSwibmVzdGVkIjp7Im51bSI6MzIxfSwibnVtIjoxMjN9".
   * structure_enc: base64encode(serializeJson(structure))
   *
   * // Returns null.
   * base64encode("")
   * </code></pre>
   *
   * @param context The current RuntimeContext.
   * @param data The {@code string} to encode. If encoding structured data, such as a {@code
   *     Container}, first pass the data to the `serializeJson()` function to convert the
   *     structured data to a {@code string}.
   * @return {@link Primitive} {@code string} a base64-encoded version of the {@code data} input
   */
  @PluginFunction
  public static Primitive base64encode(RuntimeContext context, String data) {
    if (Strings.isNullOrEmpty(data)) {
      return context.getDataTypeImplementation().primitiveOf("");
    }
    return context
        .getDataTypeImplementation()
        .primitiveOf(Base64.getEncoder().encodeToString(data.getBytes(UTF_8)));
  }

  /**
   * Decodes the given base64 data to a {@code string}. The base64 encoded data must be a UTF-8
   * string.
   *
   * <pre><code>
   * // Decodes the input to a string. After calling `base64decode()`, the value of `string1`
   * // is "this is a string".
   * string1: base64decode("dGhpcyBpcyBhIHN0cmluZw==")
   *
   * // Decodes the base 64-encoded input and stores it in the `data1` data structure.
   * data1: deserializeJson(
   *              base64decode("eyJhcnJheTEiOlsxLDIsM10sIm5lc3RlZCI6eyJudW1iZXIyIjo0NTZ9LCJudW1iZXIxIjoxMjN9"))
   *
   * // After calling `base64decode()`, the value of `data1` is:
   *
   * var data1: {
   *     array1: [1, 2, 3]
   *     number1: 123
   *     nested: {
   *         number2: 456
   *     }
   * }
   * </code></pre>
   *
   * @param context The current RuntimeContext.
   * @param inputBase64 base64 data to decode
   * @return {@link Primitive} {@code string} a base64-decoded string version of the input data
   */
  @PluginFunction
  public static Primitive base64decode(RuntimeContext context, String inputBase64) {
    if (Strings.isNullOrEmpty(inputBase64)) {
      return context.getDataTypeImplementation().primitiveOf("");
    }
    return context
        .getDataTypeImplementation()
        .primitiveOf(new String(Base64.getDecoder().decode(inputBase64), UTF_8));
  }

  /**
   * Allows a user to explicitly output an empty string as a value for fields.
   *
   * <p>For example:
   *
   * <pre><code>
   * thisWillOutput: explicitEmptyString()
   * thisWillNotOutput: ""
   * </code></pre>
   *
   * @param context The current RuntimeContext.
   */
  @PluginFunction
  public static Primitive explicitEmptyString(RuntimeContext context) {
    return new ExplicitEmptyString();
  }

  private StringFns() {}
}
