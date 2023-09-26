/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.ExplicitEmptyString;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class VarAndFieldsTest {
  IntegrationTest tester = new IntegrationTest("var_and_fields/");

  @Parameter public String testName;

  @Parameter(1)
  public String wstlFile;

  @Parameter(2)
  public String expectedExceptionMessage;

  @Parameter(3)
  public Data expectedOutputRes;

  @Parameters(name = "{0}")
  public static ImmutableList<Object[]> tests() {
    return ImmutableList.of(
        new Object[] {
          "Error in root block",
          "var_and_field_root_context_exception.wstl",
          "Fields and variables cannot share the same name within the same context, cannot name"
              + " variable \"f1\" at 5:0-5:8, previously found field at 4:0-4:12",
          null
        },
        new Object[] {
          "Error in function def",
          "var_and_field_function_context_exception.wstl",
          "Fields and variables cannot share the same name within the same context, cannot name"
              + " variable \"f3\" at 8:4-8:12, previously found field at 7:4-7:8",
          null
        },
        new Object[] {
          "No exceptions with option enabled",
          "var_and_field_no_exception_option_enabled.wstl",
          null,
          testDTI()
              .containerOf(
                  ImmutableMap.of(
                      "f1",
                      testDTI().primitiveOf(1.0),
                      "f3",
                      testDTI().containerOf(ImmutableMap.of("res", testDTI().primitiveOf(9.0)))))
        },
        new Object[] {
          "No exceptions with option disabled",
          "var_and_field_no_exception_option_disabled.wstl",
          null,
          testDTI().containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(1.0)))
        },
        new Object[] {
          "Error from an imported file when the option is enabled.",
          "var_and_field_import_option_enabled_in_imported.wstl",
          "Error processing import"
              + " res:///tests/var_and_fields/var_and_field_root_context_exception.wstl\n"
              + "Fields and variables cannot share the same name within the same context, cannot"
              + " name variable \"f1\" at 5:0-5:8, previously found field at 4:0-4:12",
          null
        },
        new Object[] {
          "No exceptions from imported file when the option is disabled in it.",
          "var_and_field_import_option_disabled_in_imported.wstl",
          null,
          testDTI().containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(3.0)))
        },
        new Object[] {
          "Redeclared vars, error points to latest declaration of the var",
          "var_and_field_redeclared_vars.wstl",
          "Fields and variables cannot share the same name within the same context, cannot name"
              + " field \"f1\" at 8:0-8:4, previously found variable at 6:0-6:8",
          null
        },
        new Object[] {
          "Updated fields, error points to latest updated field location",
          "var_and_field_redeclared_field.wstl",
          "Fields and variables cannot share the same name within the same context, cannot name"
              + " variable \"f1\" at 8:0-8:8, previously found field at 6:0-6:4",
          null
        },
        new Object[] {
          "Multiple options enabled",
          "var_and_field_and_merge_options.wstl",
          null,
          testDTI()
              .containerOf(
                  ImmutableMap.of(
                      "f1", testDTI().primitiveOf(1.0),
                      "f3",
                          testDTI()
                              .containerOf(
                                  ImmutableMap.of(
                                      "toBeMerged",
                                      testDTI()
                                          .containerOf(
                                              ImmutableMap.of(
                                                  "m1", testDTI().primitiveOf("I am OG"),
                                                  "m2",
                                                      testDTI()
                                                          .primitiveOf(
                                                              "I was acquired from the"
                                                                  + " merger")))))))
        },
        new Object[] {
          "Nested blocks",
          "var_and_field_nested_blocks.wstl",
          "Fields and variables cannot share the same name within the same context, cannot name"
              + " variable \"x\" at 8:8-8:34, previously found field at 4:0-4:4",
          null
        },
        new Object[] {
          "Function calls",
          "var_and_field_function_calls.wstl",
          null,
          testDTI()
              .containerOf(
                  ImmutableMap.of(
                      "f1", testDTI().primitiveOf(1.0), "f2", testDTI().primitiveOf(3.0)))
        },
        new Object[] {
          "Explicit empty string",
          "var_and_field_explicit_empty_string.wstl",
          null,
          testDTI()
              .containerOf(
                  ImmutableMap.of(
                      "f1", new ExplicitEmptyString(), "f3", testDTI().primitiveOf(true)))
        });
  }

  @Test
  public void test() throws IOException {
    if (expectedExceptionMessage != null) {
      RuntimeException exception =
          Assert.assertThrows(RuntimeException.class, () -> tester.initializeTestFile(wstlFile));
      assertThat(exception).hasMessageThat().isEqualTo(expectedExceptionMessage);
    } else {
      Engine engine = tester.initializeTestFile(wstlFile);
      Data res = engine.transform(NullData.instance);
      Assert.assertEquals(expectedOutputRes, res);
    }
  }
}
