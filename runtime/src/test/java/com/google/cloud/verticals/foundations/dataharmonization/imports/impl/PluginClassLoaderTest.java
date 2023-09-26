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

package com.google.cloud.verticals.foundations.dataharmonization.imports.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ClassLoader. */
@RunWith(JUnit4.class)
public class PluginClassLoaderTest {

  @Test
  public void load_validUrl_encodesIt() throws IOException {
    String url = TestPlugin.class.getName();
    byte[] encoded = new PluginClassLoader().load(ImportPathUtil.projectFile(url));
    assertEquals(url, new String(encoded, UTF_8));
  }

  @Test
  public void load_invalidUrl_throws() {
    String url = "foo";
    assertThrows(
        IOException.class, () -> new PluginClassLoader().load(ImportPathUtil.projectFile(url)));
  }

  @Test
  public void load_nonPluginClassUrl_throws() {
    String url = PluginClassLoaderTest.class.getName();
    assertThrows(
        IOException.class, () -> new PluginClassLoader().load(ImportPathUtil.projectFile(url)));
  }

  @Test
  public void getName() {
    assertEquals(PluginClassLoader.NAME, new PluginClassLoader().getName());
  }

  private abstract static class TestPlugin implements Plugin {}
}
