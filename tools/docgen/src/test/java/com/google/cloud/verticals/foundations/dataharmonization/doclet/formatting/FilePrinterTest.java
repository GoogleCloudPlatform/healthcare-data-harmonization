// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.TestMarkup;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ArgumentDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.FunctionDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.PackageDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ReturnDoc;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Correspondence;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test for FilePrinter. */
@RunWith(JUnit4.class)
public class FilePrinterTest {

  @Test
  public void format_targetsAndFunctions() throws IOException {
    PackageDoc doc =
        PackageDoc.builder()
            .setPackageName("testPkg")
            .setClassName("com.google.testpkg")
            .addFunction(function(1))
            .addFunction(function(2))
            .addFunction(function(3))
            .addTarget(target(1))
            .addTarget(target(2))
            .addTarget(target(3))
            .build();

    Map<Path, String> actual = new FilePrinter(new TestMarkup()).format(ImmutableList.of(doc));
    assertThat(actual)
        .comparingValuesUsing(
            Correspondence.<String, String>transforming(
                s -> s.replaceAll("\\s", ""), "with normalized spaces"))
        .containsExactly(Path.of("testPkg.test"), readExpected("testPkg1.test"));
  }

  private static String readExpected(String fileName) throws IOException {
    return new String(
            toByteArray(requireNonNull(FilePrinterTest.class.getResourceAsStream("/" + fileName))),
            UTF_8)
        .replaceAll("\\s", "");
  }

  private FunctionDoc function(int id) {
    FunctionDoc.Builder bldr =
        FunctionDoc.builder()
            .setName(String.format("testFn%d", id))
            .setReturns(
                ReturnDoc.builder()
                    .setType(String.format("RetType%d", id))
                    .setBody(String.format("This is the function return of function %d", id))
                    .build())
            .setBody(String.format("This is a test function number %d", id));
    for (int arg = 0; arg < 3; arg++) {
      bldr.addArgument(
          ArgumentDoc.builder()
              .setName(String.format("arg%d_%d", id, arg))
              .setType(String.format("Fn%dArg%dType", id, arg))
              .setBody(String.format("This is function %d's %d'th arg", id, arg))
              .build());
    }

    return bldr.build();
  }

  private FunctionDoc target(int id) {
    FunctionDoc.Builder bldr =
        FunctionDoc.builder()
            .setName(String.format("testTarget%d", id))
            .setReturns(
                ReturnDoc.builder()
                    .setType("THIS TYPE SHOULD NOT APPEAR")
                    .setBody("THIS BODY SHOULD NOT APPEAR")
                    .build())
            .setBody(String.format("This is a test target number %d", id));
    for (int arg = 0; arg < 3; arg++) {
      bldr.addArgument(
          ArgumentDoc.builder()
              .setName(String.format("targ%d_%d", id, arg))
              .setType(String.format("Targ%dArg%dType", id, arg))
              .setBody(String.format("This is target %d's %d'th arg", id, arg))
              .build());
    }

    return bldr.build();
  }
}
