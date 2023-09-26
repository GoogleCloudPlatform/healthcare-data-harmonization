/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Builtins interpretation of BuiltinsConfig. */
@RunWith(JUnit4.class)
public class BuiltinsConfigTest {

  @Test
  public void noFsFuncs_doesNotRegisterThem() {
    BuiltinsConfig config = BuiltinsConfig.builder().setAllowFsFuncs(false).build();
    Builtins builtins = new Builtins(config);

    List<JavaFunction> doNotWant =
        JavaFunction.ofPluginFunctionsInClass(FileFns.class, Builtins.PACKAGE_NAME);
    assertThat(doNotWant).isNotEmpty();

    List<CallableFunction> got = builtins.getFunctions();
    assertThat(got).containsNoneIn(doNotWant);
  }

  @Test
  public void allowFsFuncs_doesRegisterThem() {
    BuiltinsConfig config = BuiltinsConfig.builder().setAllowFsFuncs(true).build();
    Builtins builtins = new Builtins(config);

    List<JavaFunction> want =
        JavaFunction.ofPluginFunctionsInClass(FileFns.class, Builtins.PACKAGE_NAME);
    assertThat(want).isNotEmpty();

    List<CallableFunction> got = builtins.getFunctions();
    assertThat(got).containsAtLeastElementsIn(want);
  }
}
