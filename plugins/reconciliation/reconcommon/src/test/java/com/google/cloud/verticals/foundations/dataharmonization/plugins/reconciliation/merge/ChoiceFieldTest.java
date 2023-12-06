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
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.ChoiceField.choiceField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ChoiceField. */
@RunWith(JUnit4.class)
public class ChoiceFieldTest {

  private final RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithRegistry();

  @Test
  public void choiceField_singleFieldOption_presentInBothResources() {
    String choiceField1 = "field1";
    String mergeRule = "preferInbound";
    Data expectedValue = testDTI().primitiveOf(1.);

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", expectedValue)
                    .put("field2", testDTI().primitiveOf(2.))
                    .build());
    Data expected = testDTI().containerOf(ImmutableMap.of(choiceField1, expectedValue));

    assertEquals(expected, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }

  @Test
  public void choiceField_multipleFieldOptions_differentFieldPresentInBothResources() {
    String choiceField1 = "field1";
    String choiceField2 = "field2";
    String mergeRule = "preferInbound";
    Data expectedValue = testDTI().primitiveOf(1.);

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field2", expectedValue)
                    .put("field3", testDTI().primitiveOf(2.))
                    .build());
    Data expected = testDTI().containerOf(ImmutableMap.of(choiceField2, expectedValue));

    assertEquals(
        expected, choiceField(context, existing, inbound, mergeRule, choiceField1, choiceField2));
  }

  @Test
  public void choiceField_invalidMergeRule_throws() {
    String choiceField1 = "field1";
    String mergeRule = "inbound";

    Data existing = testDTI().emptyContainer();
    Data inbound = testDTI().emptyContainer();

    IllegalArgumentException actualThrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> choiceField(context, existing, inbound, mergeRule, choiceField1));
    assertEquals(
        "Illegal merge method selected: 'inbound', valid options are {\"forceInbound\", "
            + "\"preferInbound\"}.",
        actualThrown.getMessage());
  }

  @Test
  public void choiceField_forceInbound_noChoiceFieldInInbound_nullDataReturned() {
    String choiceField1 = "field1";
    String mergeRule = "forceInbound";

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field2", testDTI().primitiveOf(2.))
                    .put("field3", testDTI().primitiveOf(3.))
                    .build());

    assertEquals(
        NullData.instance, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }

  @Test
  public void choiceField_forceInbound_choiceFieldInInbound() {
    String choiceField1 = "field1";
    String mergeRule = "forceInbound";
    Data expectedValue = testDTI().primitiveOf(1.);

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", expectedValue)
                    .put("field2", testDTI().primitiveOf(2.))
                    .build());
    Data expected = testDTI().containerOf(ImmutableMap.of(choiceField1, expectedValue));

    assertEquals(expected, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }

  @Test
  public void choiceField_forceInbound_noChoiceFieldInEitherResource() {
    String choiceField1 = "field1";
    String mergeRule = "forceInbound";

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field2", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field2", testDTI().primitiveOf(2.))
                    .put("field3", testDTI().primitiveOf(3.))
                    .build());

    assertEquals(
        NullData.instance, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }

  @Test
  public void choiceField_preferInbound_noChoiceFieldInInbound() {
    String choiceField1 = "field1";
    String mergeRule = "preferInbound";
    Data expectedValue = testDTI().primitiveOf(1.);

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field1", expectedValue)
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field2", testDTI().primitiveOf(2.))
                    .put("field3", testDTI().primitiveOf(3.))
                    .build());
    Data expected = testDTI().containerOf(ImmutableMap.of(choiceField1, expectedValue));

    assertEquals(expected, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }

  @Test
  public void choiceField_preferInbound_choiceFieldInInbound() {
    String choiceField1 = "field1";
    String mergeRule = "preferInbound";
    Data expectedValue = testDTI().primitiveOf(1.);

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", expectedValue)
                    .put("field2", testDTI().primitiveOf(2.))
                    .build());
    Data expected = testDTI().containerOf(ImmutableMap.of(choiceField1, expectedValue));

    assertEquals(expected, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }

  @Test
  public void choiceField_preferInbound_noChoiceFieldInEitherResource() {
    String choiceField1 = "field1";
    String mergeRule = "preferInbound";

    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field0", testDTI().primitiveOf("foo"))
                    .put("field2", testDTI().primitiveOf("bar"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field2", testDTI().primitiveOf(2.))
                    .put("field3", testDTI().primitiveOf(3.))
                    .build());

    assertEquals(
        NullData.instance, choiceField(context, existing, inbound, mergeRule, choiceField1));
  }
}
