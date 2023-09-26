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
package com.google.cloud.verticals.foundations.dataharmonization.builtins.error;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ErrorConverter. */
@RunWith(JUnit4.class)
public class ErrorConverterTest {

  @Test
  public void merged_callsMerge() {
    Data aRet = mock(Data.class);
    Data bRet = mock(Data.class);
    Data cRet = mock(Data.class);

    when(aRet.merge(eq(bRet), any())).thenReturn(cRet);

    ErrorConverter a = (context, ex) -> aRet;
    ErrorConverter b = (context, ex) -> bRet;

    ErrorConverter c = ErrorConverter.merged(a, b);

    Data result = c.convert(mock(RuntimeContext.class), null);
    assertThat(result).isSameInstanceAs(cRet);
  }

  @Test
  public void withField_mergesField() {
    Data baseErr = testDTI().containerOf(ImmutableMap.of("hello", testDTI().primitiveOf("world")));
    ErrorConverter base = (context, ex) -> baseErr;

    Data want =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "hello",
                    testDTI().primitiveOf("world"),
                    "hello2",
                    testDTI().primitiveOf("world2")));

    RuntimeContext ctx = mock(RuntimeContext.class);
    when(ctx.getDataTypeImplementation()).thenReturn(testDTI());
    Data got =
        ErrorConverter.withField(base, "hello2", testDTI().primitiveOf("world2"))
            .convert(ctx, null);

    assertDCAPEquals(want, got);
  }
}
