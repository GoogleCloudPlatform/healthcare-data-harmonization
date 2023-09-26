/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.utils;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockDataset;
import java.util.stream.IntStream;
import org.junit.Assert;

/** Utility methods for comparing Data objects in tests. */
// TODO(): Add tests for these methods.
public final class AssertUtil {
  private AssertUtil() {}

  /**
   * Asserts that two {@link Data}s are <i>semantically equal</i>. This will recursively check array
   * elements and container fields, but will not care about the actual class implementing the
   * interface.
   */
  public static void assertDCAPEquals(Data expected, Data actual) {
    assertDCAPEquals("", expected, actual);
  }

  private static void assertDCAPEquals(String path, Data expected, Data actual) {
    if (expected == null || actual == null) {
      Assert.assertEquals(
          String.format("null mismatch at %s", path), expected == null, actual == null);
      return;
    }

    boolean matchedAtLeastOneDACPInterface = false;
    if (expected.isContainer() && actual.isContainer()) {
      assertDCAPEquals(path, expected.asContainer(), actual.asContainer());
      matchedAtLeastOneDACPInterface = true;
    }
    if (expected.isArray() && actual.isArray()) {
      assertDCAPEquals(path, expected.asArray(), actual.asArray());
      matchedAtLeastOneDACPInterface = true;
    }
    if (expected.isPrimitive() && actual.isPrimitive()) {
      assertDCAPEquals(path, expected.asPrimitive(), actual.asPrimitive());
      matchedAtLeastOneDACPInterface = true;
    }
    if (expected.isDataset() && actual.isDataset()) {
      if (expected instanceof MockDataset && actual instanceof MockDataset) {
        assertDCAPEquals(
            path,
            ((MockDataset) expected).getBackingArray(),
            ((MockDataset) actual).getBackingArray());
      } else {
        Assert.assertEquals(String.format("dataset mismatch at %s", path), expected, actual);
      }
      matchedAtLeastOneDACPInterface = true;
    }

    if (!matchedAtLeastOneDACPInterface) {
      Assert.assertEquals(String.format("mismatch at %s", path), expected, actual);
    }
  }

  private static void assertDCAPEquals(String path, Container expected, Container actual) {
    Assert.assertEquals(
        String.format("fields mismatch at %s", path), expected.fields(), actual.fields());
    expected
        .fields()
        .forEach(
            af ->
                assertDCAPEquals(
                    String.format("%s.%s", path, af), expected.getField(af), actual.getField(af)));
  }

  private static void assertDCAPEquals(String path, Array expected, Array actual) {
    Assert.assertEquals(
        String.format("array sizes mismatch at %s", path), expected.size(), actual.size());
    IntStream.range(0, expected.size())
        .forEach(
            i ->
                assertDCAPEquals(
                    String.format("%s[%d]", path, i),
                    expected.getElement(i),
                    actual.getElement(i)));
  }

  private static void assertDCAPEquals(String path, Primitive expected, Primitive actual) {
    Assert.assertEquals(
        String.format("boolean mismatch at %s", path), expected.bool(), actual.bool());
    Assert.assertEquals(
        String.format("string mismatch at %s", path), expected.string(), actual.string());
    Assert.assertEquals(String.format("number mismatch at %s", path), expected.num(), actual.num());
  }
}
