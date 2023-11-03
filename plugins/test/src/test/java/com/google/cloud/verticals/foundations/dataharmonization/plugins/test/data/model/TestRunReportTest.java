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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.impl.DefaultErrorConverter;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for reading TestRunReports as containers. */
@RunWith(JUnit4.class)
public class TestRunReportTest {

  private final RuntimeContext context = RuntimeContextUtil.testContext();

  @Test
  public void getNameViaGetField_returnsIt() {
    String wantName = "hello";

    TestRunReport report = TestRunReport.builder().setName(wantName).build(context);

    assertThat(report.getField("name"))
        .isEqualTo(context.getDataTypeImplementation().primitiveOf(wantName));
  }

  @Test
  public void getErrorViaGetField_returnsIt() {
    WhistleRuntimeException wantEx =
        WhistleRuntimeException.fromCurrentContext(context, new IllegalArgumentException("oops!"));
    Data wantData = new DefaultErrorConverter().convert(context, wantEx);

    TestRunReport report = TestRunReport.builder().setName("hello").setError(wantEx).build(context);

    assertThat(report.getField("error")).isEqualTo(wantData);
  }

  @Test
  public void getFailureViaGetField_returnsIt() {
    WhistleRuntimeException wantEx =
        WhistleRuntimeException.fromCurrentContext(context, new IllegalArgumentException("oops!"));
    Data wantData = new DefaultErrorConverter().convert(context, wantEx);

    TestRunReport report =
        TestRunReport.builder().setName("hello").setFailure(wantEx).build(context);

    assertThat(report.getField("failure")).isEqualTo(wantData);
  }

  @Test
  public void getUnsetViaGetField_returnsNull() {
    TestRunReport report = TestRunReport.builder().setName("hello").build(context);

    assertThat(report.getField("failure")).isEqualTo(NullData.instance);
  }
}
