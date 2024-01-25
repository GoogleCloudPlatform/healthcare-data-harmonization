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
package com.google.cloud.verticals.foundations.dataharmonization.reflection;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests for reflection utility. */
public class ReflectedPluginTest {
  /** Placeholder test class for a plugin. */
  public static class MyPlugin implements Plugin {
    @Override
    public String getPackageName() {
      return "my_plugin";
    }
  }

  @BeforeClass
  public static void setUp() {
    ReflectedInstance.registerFactory(ReflectedPlugin::new);
  }

  @Test
  public void isPlugin_onPluginClass_returnsTrue() {
    assertThat(ReflectedPlugin.isPlugin(MyPlugin.class.getName())).isTrue();
  }

  @Test
  public void isPlugin_onNonPluginClass_returnsFalse() {
    assertThat(ReflectedPlugin.isPlugin(ReflectedPluginTest.class.getName())).isFalse();
  }

  @Test
  public void getPackageName_returnsIt() {
    assertThat(new ReflectedPlugin(new MyPlugin()).getPackageName()).isEqualTo("my_plugin");
  }
}
