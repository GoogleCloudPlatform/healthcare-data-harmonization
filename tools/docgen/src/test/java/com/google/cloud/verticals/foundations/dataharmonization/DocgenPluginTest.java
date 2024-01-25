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
package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.common.truth.Truth.assertThat;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the DocgenGradle plugin (specifically how it integrates with gradle projects). */
@RunWith(JUnit4.class)
public class DocgenPluginTest {

  @Test
  public void addsGenerateWhistleDocTask() {
    Project project = ProjectBuilder.builder().build();
    project
        .getPluginManager()
        .apply("com.google.cloud.verticals.foundations.dataharmonization.docgen");

    assertThat(project.getTasks().getByName("generateWhistleDoc")).isInstanceOf(Javadoc.class);
  }

  @Test
  public void addsVerifyWhistleDocTask() {
    Project project = ProjectBuilder.builder().build();
    project
        .getPluginManager()
        .apply("com.google.cloud.verticals.foundations.dataharmonization.docgen");

    assertThat(project.getTasks().getByName("verifyWhistleDoc")).isInstanceOf(Javadoc.class);
  }

  @Test
  public void addsJavaPluginTask() {
    Project project = ProjectBuilder.builder().build();
    project
        .getPluginManager()
        .apply("com.google.cloud.verticals.foundations.dataharmonization.docgen");

    assertThat(project.getPlugins().getPlugin(JavaPlugin.class)).isNotNull();
  }
}
