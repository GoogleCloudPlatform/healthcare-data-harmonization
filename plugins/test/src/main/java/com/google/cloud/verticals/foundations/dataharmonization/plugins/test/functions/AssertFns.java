/*
 * Copyright 2022 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Math.max;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Field;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Index;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.PathSegment;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/** This class contains an example of a static function. */
public final class AssertFns {

  private static final String PAST_END_OF_ARRAY = "<past end of array>";
  private static final String MISSING = "<not present>";

  private AssertFns() {}

  /** Throws an exception if the given value is not true. Returns null otherwise. */
  @PluginFunction
  public static NullData assertTrue(RuntimeContext context, Boolean bool) {
    RunnerFns.setAssertionMade(context);
    if (bool != null && bool) {
      return NullData.instance;
    }

    throw new VerifyException(String.format("Expected true but was %s", bool));
  }

  /** Throws an exception if the given value is not null/empty. Returns null otherwise. */
  @PluginFunction
  public static NullData assertNull(RuntimeContext context, Data data) {
    RunnerFns.setAssertionMade(context);
    if (data.isNullOrEmpty()) {
      return NullData.instance;
    }

    throw new VerifyException(String.format("Expected null but was %s", data));
  }

  /**
   * Throws an exception describing the difference between the two given data, if there is any. If
   * they are the same returns null.
   *
   * <p>An example diff between <code>{"a": {"b": [1, 2, 3]}, "c": 1}</code> and <code>
   * {"a": {"b": [1, 5, 6, 7]}, "c": "one"}</code> might look like:
   *
   * <pre><code>
   * -want, +got
   *  a.b[1]: -2 +5
   *  a.b[2]: -3 +6
   *  a.b[3]: -past end of array +7
   *  c: -1 +"one"
   * </code></pre>
   */
  @PluginFunction
  public static NullData assertEquals(RuntimeContext context, Data want, Data got) {
    RunnerFns.setAssertionMade(context);
    ImmutableList<Diff> diffs = diff(want, got, new ArrayDeque<>(), new ArrayList<>());
    if (diffs.isEmpty()) {
      return NullData.instance;
    }

    throw new VerifyException(
        String.format("-want, +got\n%s", diffs.stream().map(d -> " " + d).collect(joining("\n"))));
  }

  /**
   * Throws an exception describing the difference between the two given data, if there is any. If
   * they are the same returns null. Takes an extra param for fields to ignore.
   *
   */
  @PluginFunction
  public static NullData assertEquals(
      RuntimeContext context, Data want, Data got, Array fieldsToIgnore) {
    ImmutableList<String> fieldsToIgnoreList =
        fieldsToIgnore.stream()
            .map(Object::toString)
            .collect(toImmutableList());

    RunnerFns.setAssertionMade(context);
    ImmutableList<Diff> diffs = diff(want, got, new ArrayDeque<>(), fieldsToIgnoreList);
    if (diffs.isEmpty()) {
      return NullData.instance;
    }

    throw new VerifyException(
        String.format("-want, +got\n%s", diffs.stream().map(d -> " " + d).collect(joining("\n"))));
  }

  public static ImmutableList<Diff> diff(
      Data want, Data got, Deque<PathSegment> segments, List<String> fieldsToIgnore) {
    if (want.isNullOrEmpty() && !got.isNullOrEmpty()) {
      return ImmutableList.of(new Diff(NullData.instance, got, Path.of(segments)));
    }
    if (!want.isNullOrEmpty() && got.isNullOrEmpty()) {
      return ImmutableList.of(new Diff(want, NullData.instance, Path.of(segments)));
    }
    if (want.isContainer() && got.isContainer()) {
      return diff(want.asContainer(), got.asContainer(), segments, fieldsToIgnore);
    }
    if (want.isArray() && got.isArray()) {
      return diff(want.asArray(), got.asArray(), segments, fieldsToIgnore);
    }
    if (want.isPrimitive() && got.isPrimitive()) {
      return diff(want.asPrimitive(), got.asPrimitive(), segments);
    }
    return ImmutableList.of(new Diff(want, got, Path.of(segments)));
  }

  public static ImmutableList<Diff> diff(
      Array want, Array got, Deque<PathSegment> segments, List<String> fieldsToIgnore) {
    int max = max(want.size(), got.size());
    ImmutableList.Builder<Diff> diffs = ImmutableList.builder();

    for (int i = 0; i < max; i++) {
      segments.add(new Index(i));
      if (i >= want.size()) {
        diffs.add(new Diff(null, got.getElement(i), Path.of(segments)));
      } else if (i >= got.size()) {
        diffs.add(new Diff(want.getElement(i), null, Path.of(segments)));
      } else {
        diffs.addAll(diff(want.getElement(i), got.getElement(i), segments, fieldsToIgnore));
      }
      segments.removeLast();
    }

    return diffs.build();
  }

  public static ImmutableList<Diff> diff(
      Container want, Container got, Deque<PathSegment> segments, List<String> fieldsToIgnore) {
    Set<String> wantFields = want.fields();
    Set<String> gotFields = got.fields();
    // Filter out some fields
    Set<String> fieldsToIgnoreSet = new HashSet<>(fieldsToIgnore);
    ImmutableList<String> allFields = Stream.concat(wantFields.stream(), gotFields.stream())
        .distinct()
        .filter(fieldName -> !fieldsToIgnoreSet.contains(fieldName))
        .sorted()
        .collect(toImmutableList());

    ImmutableList.Builder<Diff> diffs = ImmutableList.builder();
    for (String field : allFields) {
      segments.add(new Field(field));
      if (!wantFields.contains(field)) {
        diffs.add(new Diff(null, got.getField(field), Path.of(segments)));
      } else if (!gotFields.contains(field)) {
        diffs.add(new Diff(want.getField(field), null, Path.of(segments)));
      } else {
        diffs.addAll(diff(want.getField(field), got.getField(field), segments, fieldsToIgnore));
      }
      segments.removeLast();
    }

    return diffs.build();
  }

  private static ImmutableList<Diff> diff(
      Primitive want, Primitive got, Deque<PathSegment> segments) {
    if (!Objects.equals(want.string(), got.string())
        || !Objects.equals(want.num(), got.num())
        || !Objects.equals(want.bool(), got.bool())) {
      return ImmutableList.of(new Diff(want, got, Path.of(segments)));
    }
    return ImmutableList.of();
  }

  private static class Diff {
    private final Data want;
    private final Data got;
    private final Path path;

    /**
     * Create a new diff between data at the given path. A null reference indicates that the data
     * was missing (field did not exist or array index was past the end of the array). This is not
     * the same as an NullData, which is just a null value.
     */
    private Diff(Data want, Data got, Path path) {
      this.want = want;
      this.got = got;
      this.path = path;
    }

    @Override
    public String toString() {
      if ((want != null && want.isDataset() && !want.isNullOrEmpty())
          || (got != null && got.isDataset() && !got.isNullOrEmpty())) {
        return String.format("%s - comparing datasets is not supported.", path);
      }

      return String.format(
          "%s -%s +%s",
          path, want == null ? missingStr() : str(want), got == null ? missingStr() : str(got));
    }

    private String missingStr() {
      if (path.getSegments().isEmpty()) {
        return MISSING;
      }
      return Iterables.getLast(path.getSegments()).isIndex() ? PAST_END_OF_ARRAY : MISSING;
    }

    private String str(Data data) {
      return data.isNullOrEmpty() ? MISSING : JsonSerializerDeserializer.dataToJsonString(data);
    }
  }
}
