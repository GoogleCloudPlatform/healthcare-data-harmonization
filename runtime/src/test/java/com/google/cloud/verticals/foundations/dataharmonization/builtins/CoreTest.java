/*
 * Copyright 2021 Google LLC.
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

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockData;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.cloud.verticals.foundations.dataharmonization.utils.TestRuntimeContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Core builtins. */
@RunWith(JUnit4.class)
public class CoreTest {

  @Test
  public void unset_removeFromNull() {
    Data nullData = NullData.instance;
    assertEquals(NullData.instance, Core.unset(nullData.asContainer(), "field"));
  }

  @Test
  public void unset_doesNotContainFieldAfterRemoved() {
    Container container = testDTI().containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(1.0)));
    Truth.assertThat(container.fields()).contains("f1");
    Container result = Core.unset(container, "f1");
    Truth.assertThat(result.fields()).doesNotContain("f1");
  }

  @Test
  public void unsetIrrelevant_noop() {
    Container container = testDTI().containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(1.0)));
    Container result = Core.unset(container, "f2");
    AssertUtil.assertDCAPEquals(result, container);
  }

  @Test
  public void parseNum_withFloatingPoint() {
    Primitive expected = testDTI().primitiveOf(123.123);
    String input = "123.123";
    assertEquals(expected, Core.parseNum(new TestContext(), input));
  }

  @Test
  public void parseNum_withoutFloatingPoint() {
    Primitive expected = testDTI().primitiveOf(123.);
    String input = "123";
    assertEquals(expected, Core.parseNum(new TestContext(), input));
  }

  @Test
  public void parseNum_notParsableDouble_throws() {
    String input = "foo";
    assertThrows(NumberFormatException.class, () -> Core.parseNum(new TestContext(), input));
  }

  @Test
  public void tryParseNum_withFloatingPoint() {
    Primitive expected = testDTI().primitiveOf(123.123);
    Primitive input = testDTI().primitiveOf("123.123");
    assertEquals(expected, Core.tryParseNum(new TestContext(), input));
  }

  @Test
  public void tryParseNum_withoutFloatingPoint() {
    Primitive expected = testDTI().primitiveOf(123.);
    Primitive input = testDTI().primitiveOf("123");
    assertEquals(expected, Core.tryParseNum(new TestContext(), input));
  }

  @Test
  public void tryParseNum_notParsableDouble_throws() {
    Primitive input = testDTI().primitiveOf("foo");
    assertThrows(NumberFormatException.class, () -> Core.tryParseNum(new TestContext(), input));
  }

  @Test
  public void tryParseNum_withNum() {
    Primitive expected = testDTI().primitiveOf(123.);
    Primitive input = testDTI().primitiveOf(123.);
    assertEquals(expected, Core.tryParseNum(new TestContext(), input));
  }

  @Test
  public void tryParseNum_withBool() {
    Primitive input = testDTI().primitiveOf(true);
    assertThrows(IllegalArgumentException.class, () -> Core.tryParseNum(new TestContext(), input));
  }

  @Test
  public void types_arbitrary_containNoDupes() {
    Array array = mock(Array.class);
    Array types = Core.types(new TestContext(), array);
    assertThat(types.stream().collect(Collectors.toList())).containsNoDuplicates();
  }

  @Test
  public void types_arbitrary_containInterfaces() {
    Array types = Core.types(new TestContext(), MockData.arrayOf(mock(Data.class)));
    List<Data> typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList).contains(testDTI().primitiveOf("Array"));
    assertThat(typesList)
        .containsNoneOf(
            testDTI().primitiveOf("null"),
            testDTI().primitiveOf("Container"),
            testDTI().primitiveOf("Dataset"),
            testDTI().primitiveOf("Primitive"));

    types = Core.types(new TestContext(), MockData.containerOf(mock(Data.class)));
    typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList).contains(testDTI().primitiveOf("Container"));
    assertThat(typesList)
        .containsNoneOf(
            testDTI().primitiveOf("null"),
            testDTI().primitiveOf("Array"),
            testDTI().primitiveOf("Dataset"),
            testDTI().primitiveOf("Primitive"));

    types = Core.types(new TestContext(), testDTI().primitiveOf(123.));
    typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList).contains(testDTI().primitiveOf("Primitive"));
    assertThat(typesList)
        .containsNoneOf(
            testDTI().primitiveOf("null"),
            testDTI().primitiveOf("Array"),
            testDTI().primitiveOf("Container"),
            testDTI().primitiveOf("Dataset"));

    types = Core.types(new TestContext(), MockData.datasetOf(mock(Data.class)));
    typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList).contains(testDTI().primitiveOf("Dataset"));
    assertThat(typesList)
        .containsNoneOf(
            testDTI().primitiveOf("null"),
            testDTI().primitiveOf("Array"),
            testDTI().primitiveOf("Container"),
            testDTI().primitiveOf("Primitive"));
  }

  @Test
  public void types_null_containsNull() {
    Array types = Core.types(new TestContext(), NullData.instance);
    List<Data> typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList)
        .containsAtLeast(
            testDTI().primitiveOf("null"),
            testDTI().primitiveOf("Array"),
            testDTI().primitiveOf("Container"),
            testDTI().primitiveOf("Dataset"),
            testDTI().primitiveOf("Primitive"));

    types = Core.types(new TestContext(), testDTI().emptyContainer());
    typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList)
        .containsAtLeast(
            testDTI().primitiveOf("null"),
            testDTI().primitiveOf("Array"),
            testDTI().primitiveOf("Container"),
            testDTI().primitiveOf("Dataset"),
            testDTI().primitiveOf("Primitive"));
  }

  @Test
  public void types_default_containsImpl() {
    Array types =
        Core.types(
            new TestContext(),
            DefaultClosure.create(new TestContext(), FunctionCall.getDefaultInstance()));
    List<Data> typesList = types.stream().collect(Collectors.toList());
    assertThat(typesList)
        .containsExactly(
            testDTI().primitiveOf("DefaultClosure"), testDTI().primitiveOf("Container"));
  }

  @Test
  public void deepCopyOnDefaultDTI_returnCopy() {
    Data data =
        DefaultDataTypeImplementation.instance.containerOf(
            ImmutableMap.of(
                "field1",
                DefaultDataTypeImplementation.instance.arrayOf(
                    ImmutableList.of(DefaultDataTypeImplementation.instance.primitiveOf(1.0)))));
    Data got = Core.deepCopy(data);
    Assert.assertNotSame(got, data);
    Path arrayPath = Path.parse(".field1");
    Path primPath = Path.parse(".field1[0]");
    Assert.assertNotSame(arrayPath.get(data), arrayPath.get(got));
    Assert.assertNotSame(primPath.get(data), primPath.get(got));
    AssertUtil.assertDCAPEquals(data, got);
  }

  @Test
  public void is_caseInsensitive() {
    Data input = DefaultClosure.create(new TestContext(), FunctionCall.getDefaultInstance());
    assertThat(Core.is(new TestContext(), input, "container").bool()).isTrue();
    assertThat(Core.is(new TestContext(), input, "Container").bool()).isTrue();
    assertThat(Core.is(new TestContext(), input, "defaultclosure").bool()).isTrue();
    assertThat(Core.is(new TestContext(), input, "DefaultClosure").bool()).isTrue();
  }

  @Test
  public void is_null() {
    Data input = testDTI().emptyArray();
    assertThat(Core.is(new TestContext(), input, "container").bool()).isTrue();
    assertThat(Core.is(new TestContext(), input, "array").bool()).isTrue();
    assertThat(Core.is(new TestContext(), input, "null").bool()).isTrue();
  }

  @Test
  public void fail_throws() {
    StackFrame top = mock(StackFrame.class);
    StackFrame bottom = mock(StackFrame.class);
    when(top.getName()).thenReturn("top");
    when(top.getParent()).thenReturn(bottom);
    when(bottom.getName()).thenReturn("top");

    PackageContext packageContext = new PackageContext(ImmutableSet.of("test"));

    Exception thrown =
        assertThrows(
            WhistleRuntimeException.class,
            () -> Core.fail(new TestRuntimeContext(top, bottom, packageContext), "Oops"));
    assertThat(thrown).hasMessageThat().contains("UserException");
    assertThat(thrown).hasMessageThat().contains("Oops");
  }
}
