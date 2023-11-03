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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.targets;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for InstanceFns (i.e. the functions within). */
@RunWith(JUnit4.class)
public class ExampleTargetTest {
  @Test
  public void exampleTargetConstructor_wrongNumArgs_returnsError() {
    // Note the use of testDTI to create realistic data - we are testing the number of args check,
    // we don't want it to fail for another reason here (like wrong data type).
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ExampleTarget.Constructor()
                .construct(
                    mock(RuntimeContext.class),
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ExampleTarget.Constructor()
                .construct(
                    mock(RuntimeContext.class),
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(4.)));
  }

  @Test
  public void exampleTargetConstructor_wrongArgTypes_returnsError() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ExampleTarget.Constructor()
                .construct(
                    mock(RuntimeContext.class),
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().emptyContainer()));
  }

  @Test
  public void exampleTargetConstructor_wrongPrimitiveType_returnsError() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new ExampleTarget.Constructor()
                    .construct(
                        mock(RuntimeContext.class),
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf("oops")));
    assertThat(ex).hasMessageThat().contains("not numeric");
  }
}
