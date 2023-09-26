/*
 * Copyright 2020 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization;

import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FunctionNames;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetCustomSinkContext;
import com.google.protobuf.AbstractMessage;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link Transpiler#visitTargetCustomSink(TargetCustomSinkContext)} )}. */
@RunWith(Parameterized.class)
public class CustomTargetTest {
  private final String whistle;
  private final AbstractMessage expectedVS;

  public CustomTargetTest(String whistle, AbstractMessage expectedVS) {
    this.whistle = whistle;
    this.expectedVS = expectedVS;
  }

  @Parameters(name = "custom sink target - {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "pkg::GCS(\"gs://hello\")",
            FunctionCall.newBuilder()
                .setReference(
                    FunctionReference.newBuilder().setName("GCS").setPackage("pkg").build())
                .addArgs(ValueSource.newBuilder().setConstString("gs://hello").build())
                .build()
          },
          {
            "pkg::GCS()",
            FunctionCall.newBuilder()
                .setReference(
                    FunctionReference.newBuilder().setName("GCS").setPackage("pkg").build())
                .build()
          },
          {
            "pkg::GCS(testvar)",
            FunctionCall.newBuilder()
                .setReference(
                    FunctionReference.newBuilder().setName("GCS").setPackage("pkg").build())
                .addArgs(ValueSource.newBuilder().setFromLocal("testvar").build())
                .build()
          },
          {
            "pkg::GCS(\"hello {testvar}\")",
            FunctionCall.newBuilder()
                .setReference(
                    FunctionReference.newBuilder().setName("GCS").setPackage("pkg").build())
                .addArgs(
                    ValueSource.newBuilder()
                        .setFunctionCall(
                            FunctionCall.newBuilder()
                                .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
                                .addArgs(ValueSource.newBuilder().setConstString("hello %s"))
                                .addArgs(ValueSource.newBuilder().setFromLocal("testvar"))
                                .build()))
                .build()
          },
        });
  }

  @Test
  public void test() {
    Environment env = new Environment("testRoot");
    env.declareOrInheritVariable("testvar");
    Transpiler t = new Transpiler(env);

    AbstractMessage got = t.transpile(whistle, WhistleParser::target);

    // Strip metas for this test.
    got = TestHelper.clearMeta(got);

    assertEquals(expectedVS, got);
  }
}
