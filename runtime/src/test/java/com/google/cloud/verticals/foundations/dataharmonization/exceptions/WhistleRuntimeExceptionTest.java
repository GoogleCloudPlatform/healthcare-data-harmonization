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
package com.google.cloud.verticals.foundations.dataharmonization.exceptions;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for WhistleRuntimeException. */
@RunWith(JUnit4.class)
public class WhistleRuntimeExceptionTest {

  @Test
  public void message_isConcatenationOfCauses() {
    Throwable cause1 = new ArithmeticException("Arithmetic error of some kind");
    Throwable cause2 = new IllegalArgumentException("Some arg was illegal", cause1);
    Throwable cause3 = new IllegalStateException("Some state was illegal", cause2);

    WhistleRuntimeException whistleRuntimeException =
        WhistleRuntimeException.fromCurrentContext(RuntimeContextUtil.testContext(), cause3);

    assertThat(whistleRuntimeException)
        .hasMessageThat()
        .contains(
            "IllegalStateException: Some state was illegal\n"
                + "IllegalArgumentException: Some arg was illegal\n"
                + "ArithmeticException: Arithmetic error of some kind");
  }

  @Test
  public void message_onlyUniqueCauses() {
    Throwable cause1 = new IllegalArgumentException("Some arg was illegal");
    Throwable cause2 = new IllegalStateException("Some OTHER state was illegal", cause1);
    Throwable cause3 = new IllegalArgumentException("Some arg was illegal", cause2);
    Throwable cause4 = new IllegalStateException("Some state was illegal", cause3);

    WhistleRuntimeException whistleRuntimeException =
        WhistleRuntimeException.fromCurrentContext(RuntimeContextUtil.testContext(), cause4);

    assertThat(whistleRuntimeException)
        .hasMessageThat()
        .contains(
            "IllegalStateException: Some state was illegal\n"
                + "IllegalArgumentException: Some arg was illegal\n"
                + "IllegalStateException: Some OTHER state was illegal");
  }

  @Test
  public void message_isConcatenationOfCauses_hidden() {
    Throwable cause1 = new ArithmeticException("Arithmetic error of some kind");
    Throwable cause2 = new IllegalArgumentException("Some arg was illegal", cause1);
    Throwable cause3 = new IllegalStateException("Some state was illegal", cause2);

    RuntimeContext context = RuntimeContextUtil.testContext();
    context.getMetaData().setSerializableMeta("NO_DATA_IN_EX", true);

    WhistleRuntimeException whistleRuntimeException =
        WhistleRuntimeException.fromCurrentContext(context, cause3);

    assertThat(whistleRuntimeException)
        .hasMessageThat()
        .contains(
            "HiddenException: Hidden IllegalStateException\n"
                + "HiddenException: Hidden IllegalArgumentException\n"
                + "HiddenException: Hidden ArithmeticException");
  }

  @Test
  public void message_onlyUniqueCauses_hidden() {
    Throwable cause1 = new IllegalArgumentException("Some arg was illegal");
    Throwable cause2 = new IllegalStateException("Some OTHER state was illegal", cause1);
    Throwable cause3 = new IllegalArgumentException("Some arg was illegal", cause2);
    Throwable cause4 = new IllegalStateException("Some state was illegal", cause3);

    RuntimeContext context = RuntimeContextUtil.testContext();
    context.getMetaData().setSerializableMeta("NO_DATA_IN_EX", true);

    WhistleRuntimeException whistleRuntimeException =
        WhistleRuntimeException.fromCurrentContext(context, cause4);

    assertThat(whistleRuntimeException)
        .hasMessageThat()
        .contains(
            "HiddenException: Hidden IllegalStateException\n"
                + "HiddenException: Hidden IllegalArgumentException");
  }
}
