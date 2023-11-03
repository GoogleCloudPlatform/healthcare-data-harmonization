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

import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableContainerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.impl.DefaultErrorConverter;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for reading TestReports as containers. */
@RunWith(JUnit4.class)
public class TestReportTest {

  private final RuntimeContext context = RuntimeContextUtil.testContext();
  private final DataTypeImplementation dti =
      RuntimeContextUtil.testContext().getDataTypeImplementation();

  @Test
  public void getNumRunViaGetField_returnsIt() {
    int want = 123;

    TestReport report = TestReport.builder().setNumRun(want).setNumFailed(0).build(dti);

    assertThat(report.getField("numRun")).isEqualTo(dti.primitiveOf((double) want));
  }

  @Test
  public void getNumFailedViaGetField_returnsIt() {
    int want = 123;

    TestReport report = TestReport.builder().setNumRun(200).setNumFailed(want).build(dti);

    assertThat(report.getField("numFailed")).isEqualTo(dti.primitiveOf((double) want));
  }

  @Test
  public void getNumPassedViaGetField_computesIt() {
    TestReport report = TestReport.builder().setNumRun(300).setNumFailed(100).build(dti);

    assertThat(report.getField("numPassed")).isEqualTo(dti.primitiveOf(200.));
  }

  @Test
  public void getRunsViaGetField_returnsIt() {
    WhistleRuntimeException wantEx =
        WhistleRuntimeException.fromCurrentContext(context, new IllegalArgumentException("oops!"));
    Data wantExData = new DefaultErrorConverter().convert(context, wantEx);

    TestRunReport report1 =
        TestRunReport.builder().setName("hello").setError(wantEx).build(context);
    TestRunReport report2 =
        TestRunReport.builder().setName("hello2").setFailure(wantEx).build(context);
    TestRunReport report3 = TestRunReport.builder().setName("hello3").build(context);
    TestReport report =
        TestReport.builder()
            .setNumRun(3)
            .setNumFailed(2)
            .addTest(report1)
            .addTest(report2)
            .addTest(report3)
            .build(dti);

    Data wantData =
        arrayOf(
            mutableContainerOf(
                i -> {
                  i.set("name", dti.primitiveOf("hello"));
                  i.set("error", wantExData);
                }),
            mutableContainerOf(
                i -> {
                  i.set("name", dti.primitiveOf("hello2"));
                  i.set("failure", wantExData);
                }),
            mutableContainerOf(i -> i.set("name", dti.primitiveOf("hello3"))));

    assertDCAPEquals(wantData, report.getField("tests"));
  }
}
