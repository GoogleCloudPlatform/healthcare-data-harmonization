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

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyArray;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyContainer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.VariableTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Ternary. */
@RunWith(JUnit4.class)
public class TernaryTest {

  @Test
  public void ternary_varInheritanceIntegrationTest_assignsVarsInParentContexts() throws Exception {
    /*
      package "test"

      var x: "init"
      if $root then {
        var x: "true block"
      } else {
        var x: "false block"
      }

      x
    */
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName("test")
            .setRootBlock(
                FunctionDefinition.newBuilder()
                    .addArgs(Argument.newBuilder().setName("$root").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            // var x:
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            // "init"
                            .setValue(ValueSource.newBuilder().setConstString("init").build())
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        // if x then y else z -> $Ternary(x, y, z)
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionReference.newBuilder()
                                                    .setName("ternary")
                                                    .setPackage("builtins")
                                                    .build())
                                            .addArgs(
                                                // if $root
                                                ValueSource.newBuilder()
                                                    .setFromLocal("$root")
                                                    .build())
                                            .addArgs(
                                                // then call block_1
                                                ValueSource.newBuilder()
                                                    .setFunctionCall(
                                                        FunctionCall.newBuilder()
                                                            .setBuildClosure(true)
                                                            .setReference(
                                                                FunctionReference.newBuilder()
                                                                    .setName("block_1")
                                                                    .build())
                                                            .addArgs(
                                                                ValueSource.newBuilder()
                                                                    .setFromLocal("x"))
                                                            .build()))
                                            .addArgs(
                                                // else call block_2
                                                ValueSource.newBuilder()
                                                    .setFunctionCall(
                                                        FunctionCall.newBuilder()
                                                            .setBuildClosure(true)
                                                            .setReference(
                                                                FunctionReference.newBuilder()
                                                                    .setName("block_2")
                                                                    .build())
                                                            .addArgs(
                                                                ValueSource.newBuilder()
                                                                    .setFromLocal("x"))
                                                            .build()))
                                            .build())
                                    .build())
                            .build())
                    .addMapping(
                        // final mapping sets output to x
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setFromLocal("x").build())
                            .build())
                    .build())
            .addFunctions(
                FunctionDefinition.newBuilder()
                    // the block that came after "then"
                    .setName("block_1")
                    .addArgs(Argument.newBuilder().setName("x").build())
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            // var x:
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            // "true block"
                            .setValue(ValueSource.newBuilder().setConstString("true block").build())
                            .build())
                    .build())
            .addFunctions(
                FunctionDefinition.newBuilder()
                    // the block that came after "else"
                    .setName("block_2")
                    .addArgs(Argument.newBuilder().setName("x").build())
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            // var x:
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            // "false block"
                            .setValue(
                                ValueSource.newBuilder().setConstString("false block").build())
                            .build())
                    .build())
            .build();

    Data output =
        new Engine.Builder(TestConfigExtractor.of(config))
            .initialize()
            .build()
            .transform(testDTI().primitiveOf(true));
    assertTrue(output.isPrimitive());
    assertEquals("true block", output.asPrimitive().string());

    output =
        new Engine.Builder(TestConfigExtractor.of(config))
            .initialize()
            .build()
            .transform(testDTI().primitiveOf(false));
    assertTrue(output.isPrimitive());
    assertEquals("false block", output.asPrimitive().string());
  }

  @Test
  public void test_ternary_three_arg_true_cond() {
    RuntimeContext mockContext = mock(RuntimeContext.class);
    Closure mockTruePart = mock(Closure.class);
    Closure mockFalsePart = mock(Closure.class);
    Ternary.ternary(mockContext, testDTI().primitiveOf(true), mockTruePart, mockFalsePart);
    verify(mockTruePart, times(1)).execute(any());
    verify(mockFalsePart, times(0)).execute(any());
  }

  @Test
  public void test_ternary_three_arg_false_cond() {
    RuntimeContext mockContext = mock(RuntimeContext.class);
    Closure mockTruePart = mock(Closure.class);
    Closure mockFalsePart = mock(Closure.class);
    Ternary.ternary(mockContext, testDTI().primitiveOf(false), mockTruePart, mockFalsePart);
    verify(mockTruePart, times(0)).execute(mockContext);
    verify(mockFalsePart, times(1)).execute(mockContext);
  }

  @Test
  public void test_ternary_two_arg_true_cond() {
    RuntimeContext mockContext = mock(RuntimeContext.class);
    Closure mockTruePart = mock(Closure.class);
    when(mockTruePart.execute(any())).thenReturn(testDTI().primitiveOf("true"));
    assertEquals(
        testDTI().primitiveOf("true"),
        Ternary.ternary(mockContext, testDTI().primitiveOf(true), mockTruePart));
    verify(mockTruePart, times(1)).execute(any());
  }

  @Test
  public void test_ternary_two_arg_false_cond() {
    RuntimeContext mockContext = mock(RuntimeContext.class);
    Closure mockTruePart = mock(Closure.class);
    assertEquals(
        NullData.instance,
        Ternary.ternary(mockContext, testDTI().primitiveOf(false), mockTruePart));
    verify(mockTruePart, times(0)).execute(mockContext);
  }

  @Test
  public void isTruthy_null_false() {
    assertFalse(Ternary.isTruthy(null));
  }

  @Test
  public void isTruthy_nullData_false() {
    assertFalse(Ternary.isTruthy(NullData.instance));
  }

  @Test
  public void isTruthy_emptyContainer_false() {
    assertFalse(Ternary.isTruthy(emptyContainer()));
  }

  @Test
  public void isTruthy_emptyArray_false() {
    assertFalse(Ternary.isTruthy(emptyArray()));
  }

  @Test
  public void isTruthy_nullPrimitive_false() {
    assertFalse(Ternary.isTruthy(testDTI().primitiveOf((Boolean) null)));
    assertFalse(Ternary.isTruthy(testDTI().primitiveOf((String) null)));
    assertFalse(Ternary.isTruthy(testDTI().primitiveOf((Double) null)));
  }

  @Test
  public void isTruthy_num_true() {
    assertTrue(Ternary.isTruthy(testDTI().primitiveOf(3.14)));
  }

  @Test
  public void isTruthy_loadedString_true() {
    assertTrue(Ternary.isTruthy(testDTI().primitiveOf("hi")));
  }

  @Test
  public void isTruthy_emptyString_false() {
    assertFalse(Ternary.isTruthy(testDTI().primitiveOf("")));
  }

  @Test
  public void isTruthy_falseBool_false() {
    assertFalse(Ternary.isTruthy(testDTI().primitiveOf(false)));
  }

  @Test
  public void isTruthy_trueBool_true() {
    assertTrue(Ternary.isTruthy(testDTI().primitiveOf(true)));
  }

  @Test
  public void isTruthy_loadedContainer_true() {
    assertTrue(Ternary.isTruthy(containerOf("key", mock(Data.class))));
  }

  @Test
  public void isTruthy_loadedArray_true() {
    assertTrue(Ternary.isTruthy(arrayOf(mock(Data.class))));
  }
}
