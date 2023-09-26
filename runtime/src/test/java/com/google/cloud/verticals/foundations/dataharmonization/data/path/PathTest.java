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

package com.google.cloud.verticals.foundations.dataharmonization.data.path;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultContainer;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.FakeContainerA;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.FakeContainerB;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mockito;

/** Tests for Path. */
@RunWith(JUnit4.class)
public class PathTest {

  /** Creates a Blob (Container / Array) that returns itself for all gets. */
  private static Blob infiniteBlob() {
    Blob blob = mock(Blob.class, Answers.CALLS_REAL_METHODS);
    when(blob.getElement(anyInt())).thenReturn(blob);
    when(blob.getField(anyString())).thenReturn(blob);
    when(blob.setElement(anyInt(), any())).thenReturn(blob);
    when(blob.setField(anyString(), any())).thenReturn(blob);
    return blob;
  }

  @Test
  public void parse_emptyString_returnsEmptyPath() {
    assertEquals(Path.empty(), Path.parse(null));
    assertEquals(Path.empty(), Path.parse(""));
  }

  @Test
  public void parse_singleDelimiterPath_returnsEmptyPath() {
    assertEquals(Path.empty(), Path.parse("."));
  }

  @Test
  public void parse_wildcard_returnsWildcard() {
    Path got = Path.parse("xyz[*].foo");
    assertThat(got.getSegments())
        .containsExactly(new Field("xyz"), new Wildcard(false), new Field("foo"));
  }

  @Test
  public void parse_wildcardWithoutBrackets_returnsField() {
    Path got = Path.parse("xyz*.foo");
    assertThat(got.getSegments())
        .containsExactly(new Field("xyz*"), new Field("foo"));
  }

  @Test
  public void parse_wildcardStartWithoutBrackets_returnsField() {
    Path got = Path.parse("x*yz*.foo");
    assertThat(got.getSegments())
        .containsExactly(new Field("x*yz*"), new Field("foo"));
  }

  @Test
  public void parse_escapeAtStart_returnsField() {
    Path got = Path.parse("\\[foo");
    assertThat(got.getSegments())
        .containsExactly(new Field("[foo"));
  }

  @Test
  public void get_emptyPath_returnsSource() {
    Data source = mock(Data.class);
    Data got = Path.empty().get(source);
    assertEquals(source, got);
  }

  @Test
  public void get_singleSegmentPath_callsGetField() {
    Path path = Path.parse("one");
    Blob source = infiniteBlob();

    path.get(source);

    verify(source).getField("one");
  }

  @Test
  public void get_multipleSegmentPath_callsGetX() {
    Path path = Path.parse("one.two[3].four.five[6][7]");
    Blob source = infiniteBlob();
    InOrder inOrder = Mockito.inOrder(source);

    path.get(source);

    inOrder.verify(source).getField("one");
    inOrder.verify(source).getField("two");
    inOrder.verify(source).getElement(3);
    inOrder.verify(source).getField("four");
    inOrder.verify(source).getField("five");
    inOrder.verify(source).getElement(6);
    inOrder.verify(source).getElement(7);
  }

  @Test
  public void get_multipleSegmentPathOnNull_returnsNull() {
    Path path = Path.parse("one.two[3].four.five[6][7]");

    Data got = path.get(NullData.instance);
    assertEquals(NullData.instance, got);
  }

  @Test
  public void get_escapedPath_callsGetWithEscapesRemoved() {
    Path path = Path.parse("\\\\x\\.x\\[x\\]");
    Blob source = infiniteBlob();

    path.get(source);

    verify(source).getField("\\x.x[x]");
  }

  @Test
  public void get_singleWildcard_doesNotFlatten() {
    Path path = Path.parse("[*].field");
    Data fake = mock(Data.class);
    Array source = arrayOf(containerOf(arrayOf(fake, 10)), 10);

    Data got = path.get(source);
    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [[fake, ... (10x)], ... (10x)]
        arrayOf(arrayOf(fake, 10), 10).stream()
            .map(a -> a.asArray().stream().toArray(Data[]::new))
            .toArray(Data[][]::new),
        got.asArray().stream()
            .map(a -> a.asArray().stream().toArray(Data[]::new))
            .toArray(Data[][]::new));
  }

  @Test
  public void get_manyWildcards_flattens() {
    Path path = Path.parse("[*].field[*]");
    Data fake = mock(Data.class);
    Array source = arrayOf(containerOf(arrayOf(fake, 10)), 10);

    Data got = path.get(source);
    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [fake, ... (100x)]
        arrayOf(fake, 100).stream().toArray(Data[]::new),
        got.asArray().stream().toArray(Data[]::new));
  }

  @Test
  public void get_nestedWildcardsOnNestedArrays_returnsField() {
    Path path = Path.parse("[*][*].field");
    Data fake = mock(Data.class);
    // data = [
    //         [{field: fake}, {field: fake}, ... (10x)],
    //         [{field: fake}, {field: fake}, ... (10x)],
    //         ... (10x)]
    Array data = arrayOf(arrayOf(containerOf(fake), 10), 10);
    Data got = path.get(data);

    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [fake, fake, ... (100x)]
        arrayOf(fake, 100).stream().toArray(Data[]::new),
        got.asArray().stream().toArray(Data[]::new));
  }

  @Test
  public void get_wildcardOnNonUniformArray_throws() {
    Data fake = mock(Data.class);
    // nonUniform = [[fake], {field: fake}]
    Array nonUniform = arrayOf(arrayOf(fake), containerOf(fake));

    Path fieldPath = Path.parse("[*].foo");
    Exception got =
        assertThrows(UnsupportedOperationException.class, () -> fieldPath.get(nonUniform));
    assertThat(got).hasMessageThat().contains("Attempted to key into non-container Array");
    assertThat(got).hasMessageThat().contains("with field foo");

    Path indexPath = Path.parse("[*][0]");
    got = assertThrows(UnsupportedOperationException.class, () -> indexPath.get(nonUniform));
    assertThat(got).hasMessageThat().contains("Attempted to index into non-array Container");
    assertThat(got).hasMessageThat().contains("with index [0]");
  }

  @Test
  public void get_onArrayOfContainers_returnsField() {
    Path path = Path.parse("[*].field");
    Data fake = mock(Data.class);
    // data = [{field: fake}, {field: fake}, {field: fake}]

    Array data = arrayOf(containerOf(fake), 10);
    Data got = path.get(data);

    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [fake, fake, ...]
        arrayOf(fake, 10).stream().toArray(Data[]::new),
        got.asArray().stream().toArray(Data[]::new));
  }

  @Test
  public void get_nestedWildcardsOnDeeplyNestedArrays_returnsField() {
    Path path = Path.parse("[*].field1[*].field2");
    Data fake = mock(Data.class);
    // data = [
    //         {field1: [
    //                  {field2: fake}, {field2: fake}, ... (10x)
    //                 ]},
    //         {field1: [
    //                  {field2: fake}, {field2: fake}, ... (10x)
    //                 ]}
    //          ... (10x)
    //        ]
    Array data = arrayOf(containerOf(arrayOf(containerOf(fake), 10)), 10);
    Data got = path.get(data);

    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [fake, ... (100x)]
        arrayOf(fake, 100).stream().toArray(Data[]::new),
        got.asArray().stream().toArray(Data[]::new));
  }

  @Test
  public void get_nestedWildcardsOnMultipleIntermediateContainers_returnsItems() {
    Path path = Path.parse("[*].field1.field2[*]");
    // Have 5 unique items so we can assert order.
    Data[] flatItems =
        new Data[] {
          mock(Data.class), mock(Data.class), mock(Data.class), mock(Data.class), mock(Data.class)
        };

    // data = [
    //         {
    //            field1: {
    //              field2: [ flatItems[0], flatItems[1] ],
    //            }
    //         },
    //         {
    //            field1: {
    //              field2: [ flatItems[2], flatItems[3] ],
    //            }
    //         },
    //         {
    //            field1: {
    //              field2: [ flatItems[4] ],
    //            }
    //         },
    //        ]
    Array data =
        arrayOf(
            containerOf(containerOf(arrayOf(flatItems[0], flatItems[1]))),
            containerOf(containerOf(arrayOf(flatItems[2], flatItems[3]))),
            containerOf(containerOf(arrayOf(flatItems[4]))));
    Data got = path.get(data);
    assertDCAPEquals(got, arrayOf(flatItems));
  }

  @Test
  public void get_onArrayOfArrays_returnsElement() {
    Path path = Path.parse("[*][0]");
    Data fake = mock(Data.class);
    // data = [[fake, x], [fake, y], [fake, z], ... (x10)]

    Array data = arrayOf(arrayOf(fake, mock(Data.class)), 10);
    Data got = path.get(data);

    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [fake, fake, ...]
        arrayOf(fake, 10).stream().toArray(Data[]::new),
        got.asArray().stream().toArray(Data[]::new));
  }

  @Test
  public void set_emptyPath_returnsSource() {
    Data source = mock(Data.class);
    Data got = Path.empty().set(testDTI(), mock(Data.class), source);
    assertEquals(source, got);
  }

  @Test
  public void set_singleSegmentPath_callsSetField() {
    Path path = Path.parse("one");
    Data value = mock(Primitive.class);
    Blob blob = infiniteBlob();

    path.set(testDTI(), blob, value);

    verify(blob).setField("one", value);
  }

  @Test
  public void set_multipleSegmentPath_callsSetX() {
    Path path = Path.parse("one.two[3].four.five[6][7]");

    Data value = mock(Primitive.class);
    Blob blob = infiniteBlob();
    InOrder inOrder = Mockito.inOrder(blob);

    path.set(testDTI(), blob, value);

    // Inner most container should be augmented/created first.
    inOrder.verify(blob).setElement(7, value);
    inOrder.verify(blob).setElement(6, blob);
    inOrder.verify(blob).setField("five", blob);
    inOrder.verify(blob).setField("four", blob);
    inOrder.verify(blob).setElement(3, blob);
    inOrder.verify(blob).setField("two", blob);
    inOrder.verify(blob).setField("one", blob);
  }

  @Test
  public void set_multipleDataImplementations_fillsGapsWithDefaultImpl() {

    Container root = new FakeContainerA(ImmutableMap.of("some", testDTI().primitiveOf(1.0)));
    Data newRoot = Path.parse("one.two").set(testDTI(), root, new FakeContainerB());
    newRoot = Path.parse("one.two.three.four").set(testDTI(), newRoot, testDTI().primitiveOf(1.0));

    assertEquals(FakeContainerA.class, newRoot.getClass());

    Data one = Path.parse("one").get(newRoot);
    assertEquals(DefaultContainer.class, one.getClass());

    Data two = Path.parse("one.two").get(newRoot);
    assertEquals(FakeContainerB.class, two.getClass());

    Data three = Path.parse("one.two.three").get(newRoot);
    assertEquals(DefaultContainer.class, three.getClass());

    Data four = Path.parse("one.two.three.four").get(newRoot);
    assertEquals(DefaultPrimitive.class, four.getClass());
  }

  @Test
  public void equals_samePath() {
    Path a = Path.parse("field[0][*][]");
    Path b = Path.parse("field[0][*][]");

    assertEquals(a, b);
    assertEquals(Path.empty(), Path.parse(""));
  }

  @Test
  public void equals_differentPath() {
    Path base = Path.parse("field[0][*][]");

    // Try various minor permutations
    assertNotEquals(base, Path.parse("field2[0][*][]"));
    assertNotEquals(base, Path.parse("[0][*][]"));
    assertNotEquals(base, Path.parse("field[1][*][]"));
    assertNotEquals(base, Path.parse("field[][*][]"));
    assertNotEquals(base, Path.parse("field[0][0][]"));
    assertNotEquals(base, Path.parse("field[0][][]"));
    assertNotEquals(base, Path.parse("field[0][*][0]"));
    assertNotEquals(base, Path.parse("field[0][*][*]"));
    assertNotEquals(base, Path.parse("field[0][*]"));
  }

  @Test
  public void toString_empty() {
    Path emptyPath = Path.empty();

    assertEquals("", emptyPath.toString());
  }

  @Test
  public void toString_singleSegment() {
    String singleField = ".field";
    String singleIndex = "[0]";
    String singleWildcard = "[*]";

    assertEquals(singleField, Path.parse(singleField).toString());
    assertEquals(singleIndex, Path.parse(singleIndex).toString());
    assertEquals(singleWildcard, Path.parse(singleWildcard).toString());
  }

  @Test
  public void toString_multipleSegmentPermutations() {
    String path1 = "[*].field1[0].field2";
    String path2 = "[0].field1[*].field2";
    String path3 = ".field1[0].field2[*]";
    String path4 = ".field1[0][*].field2[]";
    String path5 = "[*][0].field1.field2";

    assertEquals(path1, Path.parse(path1).toString());
    assertEquals(path2, Path.parse(path2).toString());
    assertEquals(path3, Path.parse(path3).toString());
    assertEquals(path4, Path.parse(path4).toString());
    assertEquals(path5, Path.parse(path5).toString());
  }

  @Test
  public void getPathToLastNonLeafSegment_emptyPath_returnsEmptyPath() {
    Path path = Path.parse("");
    Path lastNonLeafSegment = path.getPathToLastNonLeafSegment();
    assertThat(lastNonLeafSegment.getSegments()).isEmpty();
  }

  @Test
  public void getPathToLastNonLeafSegment_singleSegment_returnsEmptyPath() {
    Path path = Path.parse("field1");
    Path lastNonLeafSegment = path.getPathToLastNonLeafSegment();
    assertThat(lastNonLeafSegment.getSegments()).isEmpty();
  }

  @Test
  public void getPathToLastNonLeafSegment_multipleSegments_returnsPathToLastNonLeaf() {
    Path path = Path.parse("field1.field2.field3");
    Path lastNonLeafSegment = path.getPathToLastNonLeafSegment();

    Path expectedPath = Path.parse("field1.field2");
    assertThat(lastNonLeafSegment).isEqualTo(expectedPath);
  }

  @Test
  public void parse_trailingSlash_throws() {
    VerifyException ex = assertThrows(VerifyException.class, () -> Path.parse("asd\\asd\\"));
    assertThat(ex).hasMessageThat().contains("trailing \\ in asd\\asd\\");
  }

  @Test
  public void parse_unclosedIndex_throws() {
    VerifyException ex = assertThrows(VerifyException.class, () -> Path.parse("foo.bar[123.baz"));
    assertThat(ex)
        .hasMessageThat()
        .contains("Expected ] after 123.baz in foo.bar[123.baz, found the end of the path");

    ex = assertThrows(VerifyException.class, () -> Path.parse("[123"));
    assertThat(ex)
        .hasMessageThat()
        .contains("Expected ] after 123 in [123, found the end of the path");

    ex = assertThrows(VerifyException.class, () -> Path.parse("asd["));
    assertThat(ex)
        .hasMessageThat()
        .contains("Expected ] after  in asd[, found the end of the path");
  }

  /** A mockable interface that has all of Container, Array, and Primitive methods. */
  private interface Blob extends Container, Array, Primitive {
    @Override
    default boolean isNullOrEmpty() {
      return false;
    }
  }
}
