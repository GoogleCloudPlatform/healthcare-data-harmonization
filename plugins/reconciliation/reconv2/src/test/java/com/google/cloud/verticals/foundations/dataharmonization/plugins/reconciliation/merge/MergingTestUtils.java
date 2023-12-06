/*
 * Copyright 2023 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.ChoiceField.choiceField;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.preferInbound;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.union;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_FIELD_RULES_METHOD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.ID_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.MERGE_RULES_PACKAGE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.META_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RESOURCE_TYPE_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.IDENTIFIER_FIELD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.function.BiFunction;
import org.mockito.invocation.InvocationOnMock;

/**
 * Test utilities for merge unit tests, e.g. creating and modifying mock context which contains
 * merge configuration.
 */
public class MergingTestUtils {
  /** Enables mocking of CallableFunction class. */
  private static final JsonSerializerDeserializer JSON = new JsonSerializerDeserializer();

  private static final String PATIENT_MERGE_FN_NAME = "PatientMerge";
  private abstract static class TestCallableFunc extends CallableFunction {

    @Override
    public Data callInternal(RuntimeContext context, Data... args) {
      throw new UnsupportedOperationException(
          "This method is not really implemented. It's overridden only to expose it for mocking.");
    }
  }

  @SuppressWarnings("unchecked")
  public static RuntimeContext buildMockContextMergeFunction() {
    // TODO (): Add method to RuntimeContextUtil for generating a rtx with specified
    //  registered function(s)
    RuntimeContext ctx = mock(RuntimeContext.class);

    // Define mocked CallableFunctions to execute the desired behavior (as if wstl fns).
    TestCallableFunc patientMergeFunction = mock(TestCallableFunc.class);
    TestCallableFunc defaultFieldRulesFunction = mock(TestCallableFunc.class);
    when(patientMergeFunction.callInternal(any(), any()))
        .then(args -> mockPatientMergeWstlFn(ctx, args));
    when(defaultFieldRulesFunction.callInternal(any(), any()))
        .thenReturn(mockDefaultFieldRulesWstlFn());

    // Build mock OverloadSelector
    OverloadSelector selector = mock(OverloadSelector.class);
    when(selector.select(eq(ImmutableList.of(patientMergeFunction)), any()))
        .then(args -> patientMergeFunction);
    when(selector.select(eq(ImmutableList.of(defaultFieldRulesFunction)), any()))
        .then(args -> defaultFieldRulesFunction);

    // Build mock FunctionPackageRegistry to return the mocked functions when a call is made to get
    // the desired function name.
    TestFunctionPackageRegistry registry = mock(TestFunctionPackageRegistry.class);
    when(registry.getOverloads(anySet(), eq(PATIENT_MERGE_FN_NAME)))
        .thenReturn(ImmutableSet.of(patientMergeFunction));
    when(registry.getOverloads(anySet(), eq(DEFAULT_FIELD_RULES_METHOD)))
        .thenReturn(ImmutableSet.of(defaultFieldRulesFunction));

    // Set up the mock RuntimeContext object to use the OverloadSelector and FunctionPackageRegistry
    // mocked above (in addition to other basic RuntimeContext mocking).
    // ...
    // Standard RuntimeContext mocking:
    when(ctx.getDataTypeImplementation()).thenReturn(DefaultDataTypeImplementation.instance);
    when(ctx.getRegistries()).thenReturn(mock(Registries.class));
    when(ctx.top()).thenReturn(mock(StackFrame.class));
    when(ctx.wrap(any(), any(), any()))
        .then(
            inv ->
                ((BiFunction<RuntimeContext, Data[], Data>) inv.getArgument(2))
                    .apply(ctx, inv.getArgument(1)));
    // Specific RuntimeContext behavior mocking for function references:
    when(ctx.getRegistries().getFunctionRegistry(MERGE_RULES_PACKAGE)).thenReturn(registry);
    when(ctx.getOverloadSelector()).thenReturn(selector);
    when(ctx.getMetaData()).thenReturn(new DefaultMetaData());

    return ctx;
  }

  private static Data mockPatientMergeWstlFn(RuntimeContext context, InvocationOnMock invocation) {
    Data existing = invocation.getArgument(1);
    Data inbound = invocation.getArgument(2);
    Data choiceField =
        choiceField(
            context, existing, inbound, "preferInbound", "deceasedBoolean", "deceasedDateTime");
    Data mergeResult =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "fieldOne",
                    preferInbound(
                        context,
                        existing.asContainer().getField("fieldOne"),
                        inbound.asContainer().getField("fieldOne")),
                    "fieldTwo",
                    preferInbound(
                        context,
                        existing.asContainer().getField("fieldTwo"),
                        inbound.asContainer().getField("fieldTwo")),
                    RESOURCE_TYPE_FIELD,
                    preferInbound(
                        context,
                        existing.asContainer().getField(RESOURCE_TYPE_FIELD),
                        inbound.asContainer().getField(RESOURCE_TYPE_FIELD)),
                    ID_FIELD,
                    preferInbound(
                        context,
                        existing.asContainer().getField(ID_FIELD),
                        inbound.asContainer().getField(ID_FIELD)),
                    META_FIELD,
                    preferInbound(
                        context,
                        existing.asContainer().getField(META_FIELD),
                        inbound.asContainer().getField(META_FIELD)),
                    IDENTIFIER_FIELD,
                    union(
                        context,
                        existing.asContainer().getField(IDENTIFIER_FIELD),
                        inbound.asContainer().getField(IDENTIFIER_FIELD))));
    if (!choiceField.isNullOrEmpty()) {
      for (String field : choiceField.asContainer().fields()) {
        mergeResult =
            mergeResult.asContainer().setField(field, choiceField.asContainer().getField(field));
      }
    }
    return mergeResult;
  }

  private static Container mockDefaultFieldRulesWstlFn() {
    return toData("{  \"fieldThree\": { \"rule\": \"preferInbound\" }  }").asContainer();
  }

  public static void setWstlFunctionInContext(
      RuntimeContext context, String functionName, Data returnValue) {
    TestCallableFunc function = mock(TestCallableFunc.class);
    when(function.callInternal(any(), any())).then(i -> returnValue);

    when(context.getOverloadSelector().select(eq(ImmutableList.of(function)), any()))
        .then(args -> function);
    when(context
            .getRegistries()
            .getFunctionRegistry(MERGE_RULES_PACKAGE)
            .getOverloads(anySet(), eq(functionName)))
        .thenReturn(ImmutableSet.of(function));
  }

  public static Data toData(String s) {
    return JSON.deserialize(s.getBytes());
  }

  public static Container addStableIdToNewResource(Data resourceSnapshot, String id) {
    Data resourceWithId = resourceSnapshot.deepCopy();
    resourceWithId = resourceWithId.asContainer().setField("id", testDTI().primitiveOf(id));
    return resourceWithId.asContainer();
  }
}
