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
package com.google.cloud.verticals.foundations.dataharmonization.plugin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests plugin (load). */
@RunWith(JUnit4.class)
public class PluginTest {

  @Test
  public void load_callsAllMethods() {
    Plugin plugin = mock(Plugin.class, Answers.CALLS_REAL_METHODS);
    Registries registries = mock(Registries.class, Answers.RETURNS_MOCKS);
    MetaData metaData = mock(MetaData.class);

    Plugin.load(plugin, registries, metaData);

    verify(plugin).getArgModifiers();
    verify(plugin).getFunctionRegistry();
    verify(plugin).getFunctions();
    verify(plugin).getLoaders();
    verify(plugin).getParsers();
    verify(plugin).getTargets();
    verify(plugin).getOptions();
    verify(plugin).onLoaded(eq(registries), eq(metaData));
  }
}
