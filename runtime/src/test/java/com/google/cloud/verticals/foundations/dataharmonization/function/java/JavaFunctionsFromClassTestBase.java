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

package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.errorprone.annotations.Keep;
import java.io.Serializable;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/** Base class to test java functions from instance methods . */
public abstract class JavaFunctionsFromClassTestBase {
  abstract List<JavaFunction> functionsInClass(Class<?> clazz, String pkgName);

  abstract List<JavaFunction> functionsInInstance(Serializable instance, String pkgName);

  private static final String PLACEHOLDER_PKG_NAME = "testPkg";

  @Test
  public void staticMethods_found() {
    List<JavaFunction> got = functionsInClass(TestFunctionsContainer.class, PLACEHOLDER_PKG_NAME);
    assertThat(got).hasSize(1);
    Assert.assertEquals("staticFunc", got.get(0).getSignature().getName());
    Assert.assertEquals(PLACEHOLDER_PKG_NAME, got.get(0).getSignature().getPackageName());
  }

  @Test
  public void instanceMethods_found() {
    List<JavaFunction> got =
        functionsInInstance(new TestFunctionsContainer(), PLACEHOLDER_PKG_NAME);
    assertThat(got).hasSize(1);
    Assert.assertEquals("instanceFunc", got.get(0).getSignature().getName());
    Assert.assertEquals(PLACEHOLDER_PKG_NAME, got.get(0).getSignature().getPackageName());
  }

  @Test
  public void nonPublicMethods_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> functionsInClass(TestInvalidFunctionsContainer.class, PLACEHOLDER_PKG_NAME));

    assertThrows(
        IllegalArgumentException.class,
        () -> functionsInInstance(new TestInvalidFunctionsContainer(), PLACEHOLDER_PKG_NAME));
  }

  private static final class TestFunctionsContainer implements Serializable {
    // Since these methods are accessed via reflection, so it triggers java error prone warning
    // we use @Keep to suppress these warnings.
    @Keep
    @PluginFunction
    public static Data staticFunc() {
      return NullData.instance;
    }

    @Keep
    @PluginFunction
    public Data instanceFunc() {
      return NullData.instance;
    }

    @Keep
    public static Data irrelevantStaticFunc() {
      return NullData.instance;
    }

    @Keep
    public Data irrelevantInstanceFunc() {
      return NullData.instance;
    }
  }

  private static final class TestInvalidFunctionsContainer implements Serializable {
    @Keep
    @PluginFunction
    static Data staticNotPublic() {
      return NullData.instance;
    }

    @Keep
    @PluginFunction
    Data instanceNotPublic() {
      return NullData.instance;
    }
  }
}
