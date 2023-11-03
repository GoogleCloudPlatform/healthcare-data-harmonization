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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.functions;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.io.ExampleService;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for InstanceFns (i.e. the functions within). */
@RunWith(JUnit4.class)
public class InstanceFnsTest {
  @Test
  public void exampleInstanceFn_callsService() {
    // Set up the mocks
    ExampleService service = mock(ExampleService.class);
    when(service.getUserGreeting(anyString())).thenReturn("test");
    // Note the use of utility methods to mock runtime context.
    RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithRegistry();

    InstanceFns fns = new InstanceFns(service);

    // Get our function output and expected value.
    Primitive actual = fns.exampleInstanceFn(context, "user1");
    Primitive expected = testDTI().primitiveOf("test");

    // Use the utility equality method to assert the contents are equal.
    assertDCAPEquals(expected, actual);

    // Optional: Check that the mock service was called correctly.
    verify(service).getUserGreeting("user1");
    verifyNoMoreInteractions(service);
  }
}
