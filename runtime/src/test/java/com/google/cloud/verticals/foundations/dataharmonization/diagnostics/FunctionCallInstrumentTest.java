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
package com.google.cloud.verticals.foundations.dataharmonization.diagnostics;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.diagnostics.FunctionCallInstrument.FunctionData;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for function call instrument and nested classes. */
@RunWith(JUnit4.class)
public class FunctionCallInstrumentTest {

  @Test
  public void add_incrementsNumCalls() {
    FunctionData data = new FunctionData(Duration.ofMillis(10), Duration.ofMillis(100));

    data.add(Duration.ofMillis(0), Duration.ofMillis(0));
    assertThat(data.numCalls).isEqualTo(2);
  }

  @Test
  public void add_incrementsTime() {
    FunctionData data = new FunctionData(Duration.ofMillis(10), Duration.ofMillis(100));

    data.add(Duration.ofSeconds(2), Duration.ofSeconds(1));
    assertThat(data.totalSelf).isEqualTo(Duration.ofMillis(2010));
    assertThat(data.total).isEqualTo(Duration.ofMillis(1100));
  }
}
