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

package com.google.cloud.verticals.foundations.dataharmonization.integration.mocking;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.integration.mocking.BufferTargetPlugin.BufferTarget;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for mock target functionality. */
@RunWith(Enclosed.class)
public class MockTargetTest {
  private static final MockingTest TESTER = new MockingTest("mock_target_valid/");

  /**
   * Tests for {@link BufferTarget} functionality and reproducibility in parameterized test setting.
   */
  @RunWith(Parameterized.class)
  public static class BufferTargetTest {
    @Parameter public String name;

    @Parameter(1)
    public String wstlName;

    @Parameters(name = "{0}")
    public static ImmutableCollection<Object[]> tests() {
      return ImmutableList.of(
          new Object[] {"basic function", "mock_target_basic.wstl"},
          new Object[] {"basic function reproducible", "mock_target_basic.wstl"});
    }

    @Test
    public void test() throws IOException {
      Engine engine = TESTER.initializeTestFile(wstlName);
      Data actualResult = engine.transform(NullData.instance);
      List<Data> actualBufferContent = BufferTarget.instance.getBufferContent();

      List<Data> expectedBufferContent =
          ImmutableList.of(new DefaultPrimitive("foo"), new DefaultPrimitive("Hello World"));
      Data expectedResult = NullData.instance;

      Assert.assertEquals(expectedBufferContent, actualBufferContent);
      AssertUtil.assertDCAPEquals(expectedResult, actualResult);
      // clean up
      BufferTarget.instance.clearBuffer();
    }
  }

  /** Integration test for mock target functionality. */
  @RunWith(Parameterized.class)
  public static class MockTargetValidTest {
    @Parameter public String name;

    @Parameter(1)
    public String testFileBaseName;

    @Parameter(2)
    public List<Data> expectedBufferContent;

    @Parameters(name = "{0}")
    public static ImmutableCollection<Object[]> tests() {
      return ImmutableList.of(
          new Object[] {"basic test", "mock_target_basic", ImmutableList.of()},
          new Object[] {
            "mock target with selector",
            "mock_target_with_selector",
            ImmutableList.of(new DefaultPrimitive("bar"), new DefaultPrimitive("barVal"))
          },
          new Object[] {
            "mock target calls itself",
            "mock_target_call_itself",
            ImmutableList.of(
                new DefaultPrimitive("foo"),
                new DefaultPrimitive("fooVal from mock"),
                new DefaultPrimitive("differentArg"),
                new DefaultPrimitive("fooVal"),
                new DefaultPrimitive("bar"),
                new DefaultPrimitive("barVal from mock"),
                new DefaultPrimitive("differentArg"),
                new DefaultPrimitive("barVal"))
          },
          new Object[] {"override builtin target", "mock_builtin_target", ImmutableList.of()},
          new Object[] {
            "mock target with multiple arguments",
            "mock_target_multiple_args",
            ImmutableList.of(
                new DefaultPrimitive("arg1"),
                new DefaultPrimitive("false"),
                new DefaultPrimitive("irrelevant"),
                new DefaultPrimitive("Hi"))
          },
          new Object[] {
            "mock target that swallows the input", "mock_target_no_op", ImmutableList.of()
          },
          new Object[] {"mock target with no argument", "mock_target_no_arg", ImmutableList.of()});
    }

    @After
    public void clean() {
      // clean up
      BufferTarget.instance.clearBuffer();
    }

    @Test
    public void test() throws IOException {
      Engine engine = TESTER.initTestFileWithMock(testFileBaseName);
      Data expectedResult = TESTER.loadJson(testFileBaseName + ".json");

      Data actualResult = engine.transform(NullData.instance);
      List<Data> actualBufferContent = BufferTarget.instance.getBufferContent();

      Assert.assertEquals(expectedBufferContent, actualBufferContent);
      AssertUtil.assertDCAPEquals(expectedResult, actualResult);
    }
  }
}
