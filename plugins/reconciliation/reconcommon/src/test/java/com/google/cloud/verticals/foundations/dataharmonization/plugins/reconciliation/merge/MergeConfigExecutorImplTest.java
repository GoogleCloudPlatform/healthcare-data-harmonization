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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableMap;
import javax.naming.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for MergeConfigExecutorImpl */
@RunWith(JUnit4.class)
public class MergeConfigExecutorImplTest {

  private MergeConfigExecutorImpl executor;
  private RuntimeContext ctx;
  private TestMergeFunctionFactory mergeFuncFactory;

  @Before
  public void setup() {
    this.ctx = RuntimeContextUtil.testContext();
    this.mergeFuncFactory = new TestMergeFunctionFactory();
    this.executor = new MergeConfigExecutorImpl(mergeFuncFactory);
  }

  @Test
  public void mergeRule_noDefaultOrResourceFunc_throws() {
    ConfigurationException actualThrown =
        assertThrows(ConfigurationException.class, () -> executor.getMergeRule(ctx, "Patient"));

    assertEquals(
        "Must define a default merge rule function 'DefaultRule' with package 'merge_rules' in"
            + " the reconciliation configuration.",
        actualThrown.getMessage());
  }

  @Test
  public void mergeRule_noResourceFunc_usesDefault() throws ConfigurationException {
    mergeFuncFactory.addMergeRuleFunc("Default", ctx -> testDTI().primitiveOf("latest"));

    String mergeRule = executor.getMergeRule(ctx, "Patient");
    assertEquals("latest", mergeRule);
  }

  @Test
  public void mergeRule_withResourceFunc_getsMergeRule() throws ConfigurationException {
    mergeFuncFactory.addMergeRuleFunc("Default", ctx -> testDTI().primitiveOf("latest"));
    mergeFuncFactory.addMergeRuleFunc("Patient", ctx -> testDTI().primitiveOf("merge"));

    String mergeRule = executor.getMergeRule(ctx, "Patient");
    assertEquals("merge", mergeRule);
  }

  @Test
  public void mergeRule_withInvalidMergeRuleType_throws() {
    mergeFuncFactory.addMergeRuleFunc("Default", ctx -> testDTI().emptyContainer());

    ConfigurationException actualThrown =
        assertThrows(ConfigurationException.class, () -> executor.getMergeRule(ctx, "Patient"));
    assertEquals(
        "Configured resource-level merge rule functions must return string,"
            + " but instead 'merge_rules:DefaultRule' returned '{}'.",
        actualThrown.getMessage());
  }

  @Test
  public void mergeRule_withInvalidMergeRule_throws() {
    mergeFuncFactory.addMergeRuleFunc("Patient", ctx -> testDTI().primitiveOf("invalidRule"));

    ConfigurationException actualThrown =
        assertThrows(ConfigurationException.class, () -> executor.getMergeRule(ctx, "Patient"));

    assertEquals(
        "Resource-level merge rule for the Patient resource type must be a string with value of one"
            + " of {'latest', 'merge'}, but got 'invalidRule'",
        actualThrown.getMessage());
  }

  @Test
  public void mergeResources_withNoConfiguredFunc_throws() {
    assertThrows(
        NoMatchingOverloadsException.class,
        () ->
            executor.mergeResources(
                ctx, "Patient", testDTI().emptyContainer(), testDTI().emptyContainer()));
  }

  @Test
  public void mergeResources_mergeFuncMergesResources() {
    mergeFuncFactory.addMergeFunc(
        "Patient",
        (ctx, existing, inbound) ->
            testDTI()
                .containerOf(
                    ImmutableMap.of(
                        "field1", existing.asContainer().getField("field1"),
                        "field2", inbound.asContainer().getField("field2"))));

    Container existing =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "field1", testDTI().primitiveOf("existing_field1"),
                    "field2", testDTI().primitiveOf("existing_field2")));
    Container inbound =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "field1", testDTI().primitiveOf("inbound_field1"),
                    "field2", testDTI().primitiveOf("inbound_field2")));

    Data merged = executor.mergeResources(ctx, "Patient", existing, inbound);

    Container expected =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "field1", existing.getField("field1"),
                    "field2", inbound.getField("field2")));
    assertEquals(expected, merged);
  }
}
