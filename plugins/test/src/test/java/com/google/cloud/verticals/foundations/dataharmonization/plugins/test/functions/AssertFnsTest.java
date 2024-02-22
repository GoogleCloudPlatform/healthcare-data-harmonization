/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.datasetOf;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for assert fns. */
@RunWith(JUnit4.class)
public class AssertFnsTest {
  private final RuntimeContext mockCtx = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
  private final Array fieldsToIgnore = testDTI().arrayOf();

  @Test
  public void assertEqual_diffPrimitives() {
    Primitive want = testDTI().primitiveOf("hello");
    Primitive got = testDTI().primitiveOf(1.);

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n  -\"hello\" +1");
  }

  @Test
  public void assertEqual_nulls() {
    Data want = NullData.instance;
    Data got = NullData.instance;

    assertThat(AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore).isNullOrEmpty()).isTrue();
  }

  @Test
  public void assertEqual_oneNull() {
    Data want = NullData.instance;
    Data got = testDTI().primitiveOf(1.);

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n  -<not present> +1");

    ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, got, want, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n  -1 +<not present>");
  }

  @Test
  public void assertEqual_diffContainers_sameKeyDiffValues() {
    Container want = json("{\"a\": 1}").asContainer();
    Container got = json("{\"a\": 2}").asContainer();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n .a -1 +2");
  }

  @Test
  public void assertEqual_diffNestedContainers_sameKeyDiffValues() {
    Container want = json("{\"a\": {\"b\": [{\"c\": 1}]}}").asContainer();
    Container got = json("{\"a\": {\"b\": [{\"c\": 2}]}}").asContainer();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n .a.b[0].c -1 +2");
  }

  @Test
  public void assertEqual_diffNestedContainers_diffKeysAtDiffLevels() {
    Container want = json("{\"a\": {\"b\": [1, 2, 3]}, \"c\": 1}").asContainer();
    Container got = json("{\"a\": {\"b\": [1, 5, 6, 7]}, \"c\": \"one\"}").asContainer();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo(
            "-want, +got\n"
                + " .a.b[1] -2 +5\n"
                + " .a.b[2] -3 +6\n"
                + " .a.b[3] -<past end of array> +7\n"
                + " .c -1 +\"one\"");
  }

  @Test
  public void assertEqual_diffContainers_missingKeyInGot() {
    Container want = json("{\"a\": 1, \"b\": 2}").asContainer();
    Container got = json("{\"a\": 1}").asContainer();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n .b -2 +<not present>");
  }

  @Test
  public void assertEqual_diffContainers_missingKeyInWant() {
    Container want = json("{\"a\": 1}").asContainer();
    Container got = json("{\"a\": 1, \"b\": 2}").asContainer();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n .b -<not present> +2");
  }

  @Test
  public void assertEqual_diffArrays_diffSingleItem() {
    Array want = json("[2]").asArray();
    Array got = json("[3]").asArray();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n [0] -2 +3");
  }

  @Test
  public void assertEqual_diffArrays_diffItems() {
    Array want = json("[1, 2, 3]").asArray();
    Array got = json("[1, 3, 3]").asArray();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n [1] -2 +3");
  }

  @Test
  public void assertEqual_diffArrays_missingItemInGot() {
    Array want = json("[1, 2, 3]").asArray();
    Array got = json("[1, 2]").asArray();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n" + " [2] -3 +<past end of array>");
  }

  @Test
  public void assertEqual_diffArrays_missingItemInWant() {
    Array want = json("[1, 2]").asArray();
    Array got = json("[1, 2, 3]").asArray();

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex).hasMessageThat().isEqualTo("-want, +got\n" + " [2] -<past end of array> +3");
  }

  @Test
  public void assertEqual_diffDatasets_incomparable() {
    Dataset want = datasetOf(testDTI().primitiveOf(1.));
    Dataset got = datasetOf(testDTI().primitiveOf(1.));

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo("-want, +got\n" + "  - comparing datasets is not supported.");
  }

  @Test
  public void assertEqual_nestedDatasets_incomparableInContext() {
    Dataset wantDs = datasetOf(testDTI().primitiveOf(1.));
    Dataset gotDs = datasetOf(testDTI().primitiveOf(1.));

    Container want = containerOf("dataset", wantDs);
    Container got = containerOf("dataset", gotDs);

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo("-want, +got\n" + " .dataset - comparing datasets is not supported.");
  }

  @Test
  public void assertEqual_diffTypes_incomparable() {
    Container want =
        testDTI()
            .containerOf(
                ImmutableMap.of("someField", testDTI().arrayOf(testDTI().primitiveOf("one"))));
    Container got =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "someField",
                    testDTI().containerOf(ImmutableMap.of("hello", testDTI().primitiveOf("two")))));

    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () -> AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo("-want, +got\n" + " .someField -[\"one\"] +{\"hello\":\"two\"}");
  }

  @Test
  public void assertEqual_equalData_returnsNull() {
    Data want = json("{\"a\": [1, 2, 3], \"b\": {\"c\": \"d\"}}").asContainer();
    Data got = json("{\"a\": [1, 2, 3], \"b\": {\"c\": \"d\"}}").asContainer();

    assertThat(AssertFns.assertEquals(mockCtx, want, got, fieldsToIgnore).isNullOrEmpty()).isTrue();
  }

  private static Data json(String json) {
    return new JsonSerializerDeserializer().deserialize(json.getBytes(UTF_8));
  }
}
