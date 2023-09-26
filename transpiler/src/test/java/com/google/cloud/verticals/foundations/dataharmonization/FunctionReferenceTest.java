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

import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for the FunctionName rules. */
@RunWith(Parameterized.class)
public class FunctionReferenceTest {
  private final String whistle;
  private final FunctionReference expectedVS;

  public FunctionReferenceTest(String whistle, FunctionReference expectedVS) {
    this.whistle = whistle;
    this.expectedVS = expectedVS;
  }

  @Test
  public void test() {
    Transpiler t = new Transpiler(new Environment("testRoot"));
    FunctionReference got = (FunctionReference) t.transpile(whistle, WhistleParser::functionName);
    assertEquals(expectedVS, got);
  }

  @Parameters(name = "functionName - {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"func", FunctionReference.newBuilder().setName("func").build()},
          {"replace", FunctionReference.newBuilder().setName("replace").build()},
          {"pkg::func", FunctionReference.newBuilder().setPackage("pkg").setName("func").build()},
          {
            "pkg1::pkg2::func",
            FunctionReference.newBuilder().setPackage("pkg1::pkg2").setName("func").build()
          },
          {"*::func", FunctionReference.newBuilder().setPackage("*").setName("func").build()},
          {"*::func", FunctionReference.newBuilder().setPackage("*").setName("func").build()},
        });
  }
}
