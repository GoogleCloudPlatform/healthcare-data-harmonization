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

import com.google.cloud.verticals.foundations.dataharmonization.doclet.WhistleDoclet;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.WhistleDoclet.GradleProjectPathOption;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.WhistleDoclet.VerifyOption;
import com.google.common.collect.ImmutableList;
import io.github.classgraph.ClassGraph;
import java.io.File;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.javadoc.Javadoc;

/** Entrypoint class for Whistle Doc Generation gradle plugin. */
public class DocgenPlugin implements Plugin<Project> {

  /** Extension for configuring docgen plugin. */
  public interface DocgenPluginExtension {

    DirectoryProperty getDestinationDirectory();
  }

  public static final String GENERATE_TASK_NAME = "generateWhistleDoc";
  private static final String VERIFY_TASK_NAME = "verifyWhistleDoc";

  @Override
  public void apply(Project project) {
    // Add the java plugin (if not already added since we depend on the extension below).
    project.getPluginManager().apply(JavaPlugin.class);
    DocgenPluginExtension docgenSettings =
        project.getExtensions().create("docgen", DocgenPluginExtension.class);
    project
        .getTasks()
        .register(GENERATE_TASK_NAME, Javadoc.class, task(docgenSettings, false, project));
    project
        .getTasks()
        .register(VERIFY_TASK_NAME, Javadoc.class, task(docgenSettings, true, project));
  }

  private static Action<? super Javadoc> task(
      DocgenPluginExtension docgenSettings, boolean verify, Project project) {

    return (javadoc) -> {
      JavaPluginExtension javaExtension =
          project.getExtensions().getByType(JavaPluginExtension.class);
      SourceSet sourceSet = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
      javadoc.setDescription("Generates Whistle Plugin Function documentation code.");
      javadoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);
      javadoc.setClasspath(sourceSet.getOutput().plus(sourceSet.getCompileClasspath()));
      javadoc.setSource(sourceSet.getAllJava());
      javadoc
          .getOptions()
          .setDestinationDirectory(
              docgenSettings
                  .getDestinationDirectory()
                  .orElse(javaExtension.getDocsDir().dir("whistle"))
                  .get()
                  .getAsFile());
      if (verify) {
        javadoc.getOptions().windowTitle(null).addBooleanOption(VerifyOption.NAME, true);
      }
      javadoc
          .getOptions()
          .windowTitle(null)
          .addStringOption(GradleProjectPathOption.NAME, project.getProjectDir().getAbsolutePath());
      javadoc
          .getModularity()
          .getInferModulePath()
          .convention(javaExtension.getModularity().getInferModulePath());

      // We need runtime access to two sets of classes:
      // 1) The project's classes, so we can load the plugin and determine packages
      // 2) This plugin's classes (i.e. the doclet and supporting code)
      // The order matters here because we don't want this plugin's deps
      // overriding the project's deps (e.x. gradle transitively imports an old version of guava).
      List<File> classes =
          ImmutableList.<File>builder()
              .addAll(sourceSet.getRuntimeClasspath().getFiles())
              .addAll(sourceSet.getCompileClasspath().getFiles())
              .addAll(currentClasspath())
              .build();

      javadoc.getOptions().setDocletpath(classes);
      javadoc.getOptions().setClasspath(classes);
      javadoc.getOptions().setDoclet(WhistleDoclet.class.getName());

      String debugPort = System.getProperty("docgenDebug");
      if (debugPort != null) {
        javadoc
            .getOptions()
            .jFlags("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
      }
    };
  }

  private static List<File> currentClasspath() {
    return new ClassGraph().getClasspathFiles();
  }
}
