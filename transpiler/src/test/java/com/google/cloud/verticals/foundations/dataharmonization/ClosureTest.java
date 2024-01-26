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
package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.clearMeta;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FunctionNames;
import com.google.cloud.verticals.foundations.dataharmonization.data.Functions;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Test for expression transpilation that requires constructing closure functions. */
@RunWith(Parameterized.class)
public class ClosureTest {
  private static final List<String> UUIDs =
      ImmutableList.of(
          "ffffffff-b8d5-9fd6-ffff-ffffbc12dbb4",
          "ffffffff-b8d5-9fd6-ffff-ffffbc12dbb4",
          "00000000-31e9-f345-ffff-ffffb73ee369",
          "ffffffff-aaca-f867-0000-0000070c5774",
          "ffffffff-c747-e698-0000-00004d95fa51",
          "ffffffff-9e5d-1176-ffff-ffffb3b88942",
          "ffffffff-9f83-48fd-0000-00003faffa1a",
          "00000000-3c9b-c140-0000-0000446b8453",
          "00000000-7cba-eb40-0000-00001bac6207",
          "ffffffff-ae30-24e9-ffff-ffffcf589c8d",
          "ffffffff-8648-9c4a-0000-0000497b11be",
          "00000000-7f3a-c27b-0000-00006a667a39",
          "ffffffff-fdb1-44ca-ffff-ffff811d6618",
          "00000000-5908-9911-0000-00004f1bcf38",
          "00000000-782f-65e9-0000-00000340a9f0",
          "ffffffff-91c3-853d-ffff-ffff9a7633de",
          "ffffffff-a50a-9a08-0000-000031c48152",
          "ffffffff-c902-b799-ffff-ffffdce6b5fd",
          "00000000-26c0-d25f-ffff-ffffeb6296f8");

  // expected result for foo[where $ > 0].bar.baz[0][sortBy $.field]
  private static ValueSource selectorTestResult(String whereId, String sortById) {
    // Get_field(foo[where $ > 0], ".bar.baz[0]")
    ValueSource getFieldResult =
        TranspilerHelper.constructFunctionCallVS(
            /* functionReference = */ FunctionNames.GET_FIELD.getFunctionReferenceProto(),
            /* source = */ null,
            /* buildClosure = */ false,
            /* args = */ Arrays.asList(
                // Where(foo, $ > 0)
                TranspilerHelper.constructFunctionCallVS(
                    /* functionReference = */ Functions.WHERE_REF,
                    /* source = */ null,
                    /* buildClosure = */ false,
                    /* args = */ Arrays.asList(
                        ValueSource.newBuilder().setFromLocal("foo").build(),
                        // call $ > 0 lambda
                        TranspilerHelper.constructFunctionCallVS(
                            FunctionReference.newBuilder()
                                .setName("selector-where_" + whereId)
                                .build(),
                            null,
                            true,
                            Arrays.asList(
                                ValueSource.newBuilder().setFreeParameter("$").build())))),
                ValueSource.newBuilder().setConstString(".bar.baz[0]").build()));
    ValueSource closure =
        TranspilerHelper.constructFunctionCallVS(
            /* functionReference = */ FunctionReference.newBuilder()
                .setName("selector-sortby_" + sortById)
                .build(),
            /* source = */ null,
            /* buildClosure = */ true,
            /* args = */ Arrays.asList(ValueSource.newBuilder().setFreeParameter("$").build()));
    // sortBy(foo[where $ > 0].bar.baz[0], $.field)
    return TranspilerHelper.constructFunctionCallVS(
        /* functionReference = */ Functions.SORTBY_REF,
        /* source = */ null,
        /* buildClosure = */ false,
        /* args = */ Arrays.asList(
            // Get_field(foo[where $ > 0], ".bar.baz[0]")
            getFieldResult,
            // call $.field closure
            closure));
  }

  @Parameter(0)
  public String testName;

  @Parameter(1)
  public String whistle;

  @Parameter(2)
  public Environment parentEnv;

  @Parameter(3)
  public List<FunctionDefinition> expectedLambdaDefs;

  @Parameter(4)
  public ValueSource expectedFuncCallVS;

  @Parameter(5)
  public Class<? extends Exception> expectedException;

  // TODO: (b/174692019) clean up the proto generation code.
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          // Infix operator - primitive
          {
            "Infix operator - primitive",
            "!false and 1?",
            new Environment("testEnv Empty"),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("infix-operator_" + UUIDs.get(1))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference("!"))
                                            .addArgs(
                                                ValueSource.newBuilder()
                                                    .setConstBool(false)
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("infix-operator_" + UUIDs.get(2))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference("?"))
                                            .addArgs(
                                                ValueSource.newBuilder().setConstInt(1).build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                FunctionNames.OPERATORS.getSymbolReference("and"),
                null,
                false,
                Arrays.asList(
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("infix-operator_" + UUIDs.get(1))
                            .build(),
                        null,
                        true,
                        ImmutableList.of()),
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("infix-operator_" + UUIDs.get(2))
                            .build(),
                        null,
                        true,
                        ImmutableList.of()))),
            null
          },
          // Infix operator - variadic defined
          {
            "Infix operator - variadic defined",
            "!foo.boolean\\ field and bar?",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo", "bar"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("infix-operator_" + UUIDs.get(3))
                    .setInheritParentVars(true)
                    .addArgs(Argument.newBuilder().setName("foo").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference("!"))
                                            .addArgs(
                                                ValueSource.newBuilder()
                                                    .setFunctionCall(
                                                        FunctionCall.newBuilder()
                                                            .setReference(
                                                                FunctionNames.GET_FIELD
                                                                    .getFunctionReferenceProto())
                                                            .addArgs(
                                                                ValueSource.newBuilder()
                                                                    .setFromLocal("foo")
                                                                    .build())
                                                            .addArgs(
                                                                ValueSource.newBuilder()
                                                                    .setConstString(
                                                                        ".boolean field")
                                                                    .build())
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("infix-operator_" + UUIDs.get(4))
                    .setInheritParentVars(true)
                    .addArgs(Argument.newBuilder().setName("bar").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference("?"))
                                            .addArgs(
                                                ValueSource.newBuilder()
                                                    .setFromLocal("bar")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                FunctionNames.OPERATORS.getSymbolReference("and"),
                null,
                false,
                Arrays.asList(
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("infix-operator_" + UUIDs.get(3))
                            .build(),
                        null,
                        true,
                        Arrays.asList(ValueSource.newBuilder().setFromLocal("foo").build())),
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("infix-operator_" + UUIDs.get(4))
                            .build(),
                        null,
                        true,
                        Arrays.asList(ValueSource.newBuilder().setFromLocal("bar").build())))),
            null
          },
          // Condition - primitive no else
          {
            "Condition - primitive no else",
            "if true then 1",
            new Environment("testEnv Empty"),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("ternary-then_" + UUIDs.get(5))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setConstInt(1).build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                Functions.TERNARY_REF,
                null,
                false,
                Arrays.asList(
                    ValueSource.newBuilder().setConstBool(true).build(),
                    ValueSource.newBuilder()
                        .setFunctionCall(
                            FunctionCall.newBuilder()
                                .setReference(
                                    FunctionReference.newBuilder()
                                        .setName("ternary-then_" + UUIDs.get(5))
                                        .build())
                                .setBuildClosure(true)
                                .build())
                        .build())),
            null
          },
          // Condition - primitive
          {
            "Condition - primitive",
            "if true then 1 else 2",
            new Environment("testEnv Empty"),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("ternary-then_" + UUIDs.get(6))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setConstInt(1).build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("ternary-else_" + UUIDs.get(7))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setConstInt(2).build())
                            .build())
                    .build()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(Functions.TERNARY_REF)
                        .addArgs(ValueSource.newBuilder().setConstBool(true).build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionReference.newBuilder()
                                                .setName("ternary-then_" + UUIDs.get(6))
                                                .build())
                                        .setBuildClosure(true)
                                        .build())
                                .build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionReference.newBuilder()
                                                .setName("ternary-else_" + UUIDs.get(7))
                                                .build())
                                        .setBuildClosure(true)
                                        .build())
                                .build())
                        .build())
                .build(),
            null
          },
          // Condition - variadic
          {
            "Condition - variadic",
            "if foo.boolean\\ field then bar else baz",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo", "bar", "baz"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("ternary-then_" + UUIDs.get(8))
                    .setInheritParentVars(true)
                    .addArgs(Argument.newBuilder().setName("bar").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setFromLocal("bar").build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("ternary-else_" + UUIDs.get(9))
                    .setInheritParentVars(true)
                    .addArgs(Argument.newBuilder().setName("baz").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setFromLocal("baz").build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                Functions.TERNARY_REF,
                null,
                false,
                Arrays.asList(
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionNames.GET_FIELD.getFunctionReferenceProto(),
                        null,
                        false,
                        Arrays.asList(
                            ValueSource.newBuilder().setFromLocal("foo").build(),
                            ValueSource.newBuilder().setConstString(".boolean field").build())),
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("ternary-then_" + UUIDs.get(8))
                            .build(),
                        null,
                        true,
                        Arrays.asList(ValueSource.newBuilder().setFromLocal("bar").build())),
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("ternary-else_" + UUIDs.get(9))
                            .build(),
                        null,
                        true,
                        Arrays.asList(ValueSource.newBuilder().setFromLocal("baz").build())))),
            null
          },
          // input source path single selector
          {
            "input source path single selector",
            "foo[where $ > 0]",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo", "bar"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("selector-where_" + UUIDs.get(10))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference(">"))
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder().setConstInt(0).build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                Functions.WHERE_REF,
                null,
                false,
                Arrays.asList(
                    ValueSource.newBuilder().setFromLocal("foo").build(),
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("selector-where_" + UUIDs.get(10))
                            .build(),
                        null,
                        true,
                        Arrays.asList(ValueSource.newBuilder().setFreeParameter("$").build())))),
            null
          },
          // input source path single selector iterate
          {
            "input source path single selector iterate",
            "foo[where $ > 0][]",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo", "bar"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("selector-where_" + UUIDs.get(11))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference(">"))
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder().setConstInt(0).build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                    Functions.WHERE_REF,
                    null,
                    false,
                    Arrays.asList(
                        ValueSource.newBuilder().setFromLocal("foo").build(),
                        TranspilerHelper.constructFunctionCallVS(
                            FunctionReference.newBuilder()
                                .setName("selector-where_" + UUIDs.get(11))
                                .build(),
                            null,
                            true,
                            Arrays.asList(ValueSource.newBuilder().setFreeParameter("$").build()))))
                .toBuilder()
                .setIterate(true)
                .build(),
            null
          },
          // input source path single selector with defined variable
          {
            "input source path single selector with defined variable",
            "foo[where $ > bar]",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo", "bar"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("selector-where_" + UUIDs.get(12))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addArgs(Argument.newBuilder().setName("bar").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference(">"))
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder()
                                                    .setFromLocal("bar")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                Functions.WHERE_REF,
                null,
                false,
                Arrays.asList(
                    ValueSource.newBuilder().setFromLocal("foo").build(),
                    TranspilerHelper.constructFunctionCallVS(
                        FunctionReference.newBuilder()
                            .setName("selector-where_" + UUIDs.get(12))
                            .build(),
                        null,
                        true,
                        Arrays.asList(
                            ValueSource.newBuilder().setFreeParameter("$").build(),
                            ValueSource.newBuilder().setFromLocal("bar").build())))),
            null
          },
          // input source path single selector with undefined variable
          {
            "input source path single selector with undefined variable",
            "foo[where $ > bar]",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ImmutableList.of(),
            ValueSource.getDefaultInstance(),
            TranspilationException.class
          },
          // input source path multiple selector - end by selector
          {
            "input source path multiple selector - end by selector",
            "foo[where $ > 0].bar.baz[0][sortBy $.field]",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("selector-where_" + UUIDs.get(13))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference(">"))
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder().setConstInt(0).build())
                                            .build())
                                    .build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("selector-sortby_" + UUIDs.get(14))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.GET_FIELD.getFunctionReferenceProto())
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder()
                                                    .setConstString(".field")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            selectorTestResult(UUIDs.get(13), UUIDs.get(14)),
            null
          },
          // input source path multiple selector - end by path
          {
            "input source path multiple selector - end by path",
            "foo[where $ > 0].bar.baz[0][sortBy $.field][0]",
            new Environment(
                "testEnv defined",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("selector-where_" + UUIDs.get(15))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.OPERATORS.getSymbolReference(">"))
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder().setConstInt(0).build())
                                            .build())
                                    .build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("selector-sortby_" + UUIDs.get(16))
                    .addArgs(Argument.newBuilder().setName("$").build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionNames.GET_FIELD.getFunctionReferenceProto())
                                            .addArgs(
                                                ValueSource.newBuilder().setFromLocal("$").build())
                                            .addArgs(
                                                ValueSource.newBuilder()
                                                    .setConstString(".field")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build()),
            TranspilerHelper.constructFunctionCallVS(
                FunctionNames.GET_FIELD.getFunctionReferenceProto(),
                null,
                false,
                Arrays.asList(
                    selectorTestResult(UUIDs.get(15), UUIDs.get(16)),
                    ValueSource.newBuilder().setConstString("[0]").build())),
            null
          },
          // Condition - primitive with multiple newlines
          {
            "Condition - primitive with multiple newlines",
            "if true then 1\n\n\n else 2",
            new Environment("testEnv Empty"),
            Arrays.asList(
                FunctionDefinition.newBuilder()
                    .setName("ternary-then_" + UUIDs.get(17))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setConstInt(1).build())
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setName("ternary-else_" + UUIDs.get(18))
                    .setInheritParentVars(true)
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(ValueSource.newBuilder().setConstInt(2).build())
                            .build())
                    .build()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(Functions.TERNARY_REF)
                        .addArgs(ValueSource.newBuilder().setConstBool(true).build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionReference.newBuilder()
                                                .setName("ternary-then_" + UUIDs.get(17))
                                                .build())
                                        .setBuildClosure(true)
                                        .build())
                                .build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionReference.newBuilder()
                                                .setName("ternary-else_" + UUIDs.get(18))
                                                .build())
                                        .setBuildClosure(true)
                                        .build())
                                .build())
                        .build())
                .build(),
            null
          },
        });
  }

  @BeforeClass
  public static void setUp() {
    LambdaHelper.setSeed(100);
  }

  @Test
  public void test() {
    Transpiler t = new Transpiler(this.parentEnv);
    FileInfo fileInfo = FileInfo.newBuilder().setUrl("unitTestURI").build();

    if (expectedException != null) {
      assertThrows(
          expectedException, () -> t.transpile(whistle, WhistleParser::expression, fileInfo));
    } else {
      ValueSource got = (ValueSource) t.transpile(this.whistle, WhistleParser::expression);

      // Strip metas for this test.
      got = clearMeta(got);
      List<FunctionDefinition> gotFuncs =
          t.getAllFunctions().stream().map(TestHelper::clearMeta).collect(Collectors.toList());

      assertEquals(expectedFuncCallVS, got);
      assertEquals(expectedLambdaDefs, gotFuncs);
    }
  }
}
