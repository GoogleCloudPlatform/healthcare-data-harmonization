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
package com.google.cloud.verticals.foundations.dataharmonization.doclet;

import com.google.cloud.verticals.foundations.dataharmonization.DocgenPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.FilePrinter;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.Markdown;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.PackageDoc;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedCallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedInstance;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedJavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedTargetConstructor;
import com.google.common.base.Charsets;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;

/** Entrypoint class for the Whistle Doclet. */
public class WhistleDoclet extends StandardDoclet {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private Path destPath = Path.of("./");
  private Boolean verify = false;
  private String gradleProjectPath = "The gradle project this file pertains to.";

  /** Option indicating where to output the files. */
  public class DestinationOption implements Option {

    @Override
    public int getArgumentCount() {
      return 1;
    }

    @Override
    public String getDescription() {
      return "g3doc base folder to output into.";
    }

    @Override
    public Kind getKind() {
      return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
      return List.of("-d");
    }

    @Override
    public String getParameters() {
      return "<destination>";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
      destPath = Path.of(arguments.get(0));
      return true;
    }
  }

  /**
   * Option indicating whether to just verify the file integrity rather than actually output docs.
   */
  public class VerifyOption implements Option {
    public static final String NAME = "verify";

    @Override
    public int getArgumentCount() {
      return 0;
    }

    @Override
    public String getDescription() {
      return "Flag indicating to just verify content of existing docs rather than output new ones.";
    }

    @Override
    public Kind getKind() {
      return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
      return List.of("-" + NAME);
    }

    @Override
    public String getParameters() {
      return "";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
      verify = true;
      return true;
    }
  }

  /**
   * Option indicating whether to just verify the file integrity rather than actually output docs.
   */
  public class GradleProjectPathOption implements Option {
    public static final String NAME = "gradleProjectPath";

    @Override
    public int getArgumentCount() {
      return 1;
    }

    @Override
    public String getDescription() {
      return "Option indicating which gradle project called this doclet (if any).";
    }

    @Override
    public Kind getKind() {
      return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
      return List.of("-" + NAME);
    }

    @Override
    public String getParameters() {
      return "";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
      gradleProjectPath = arguments.size() > 0 ? arguments.get(0) : gradleProjectPath;
      return true;
    }
  }

  @Override
  public Set<Option> getSupportedOptions() {
    return Sets.union(
        ImmutableSet.of(new DestinationOption(), new VerifyOption(), new GradleProjectPathOption()),
        super.getSupportedOptions());
  }

  @Override
  public boolean run(DocletEnvironment environment) {
    ReflectedInstance.registerFactory(ReflectedCallableFunction::new);
    ReflectedInstance.registerFactory(ReflectedJavaFunction::new);
    ReflectedInstance.registerFactory(ReflectedPlugin::new);
    ReflectedInstance.registerFactory(
        ReflectedTargetConstructor.TARGET_CONSTRUCTOR_BASECLASS_NAME,
        ReflectedTargetConstructor::new);

    PluginScanner pluginFinder = new PluginScanner();
    Set<ReflectedPlugin> plugins = new HashSet<>();
    pluginFinder.scan(environment.getIncludedElements(), plugins);

    ReversePackageSearch reversePackageSearch = new ReversePackageSearch(plugins);
    try {
      reversePackageSearch.generateReverseMappings();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to initialize engine", e);
    }

    // TODO(rpolyano): Make MarkupFormat configurable from CLI
    FunctionScanner docBuilder =
        new FunctionScanner(
            new Markdown(),
            environment.getDocTrees(),
            environment.getTypeUtils(),
            environment.getElementUtils(),
            reversePackageSearch);

    Map<String, PackageDoc.Builder> pkgToDocs = new HashMap<>();
    docBuilder.scan(environment.getIncludedElements(), pkgToDocs);

    FilePrinter mdPrinter = new FilePrinter(new Markdown());
    Map<Path, String> filesToWrite =
        mdPrinter.format(
            pkgToDocs.values().stream()
                .map(PackageDoc.Builder::build)
                .collect(Collectors.toList()));

    if (verify) {
      filesToWrite.forEach(this::verifyFile);
    } else {
      filesToWrite.forEach(this::writeFile);
    }

    return true;
  }

  private void writeFile(Path path, String s) {
    File file = destPath.resolve(path).toFile();
    if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
      throw new UncheckedIOException(
          new IOException(String.format("Failed to mkdirs %s", file.getParentFile())));
    }
    try (FileWriter fw = new FileWriter(file, Charsets.UTF_8)) {
      logger.atInfo().log("Writing %s to %s", path, file);
      fw.write(s);
      fw.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void verifyFile(Path path, String newDoc) {
    File file = destPath.resolve(path).toFile();
    if (!file.exists()) {
      throw new VerifyException(
          String.format(
              "Verification of doc file %s failed.\nMake sure you update the file by running the"
                  + " following commands:\n\n%s",
              file.getAbsolutePath(), fixCmd()),
          new FileNotFoundException(
              String.format("File %s did not exist but should.", file.getAbsolutePath())));
    }
    try {
      String existing = Files.readString(file.toPath(), Charsets.UTF_8);
      // Compare ignoring spaces since mdformat may have messed with them.
      // Also ignore non-word chars (b/232813309).
      // TODO(): Fix non-word chars and do not ignore them.
      String existingNormalized = existing.replaceAll("\\s+", " ").replaceAll("\\W+", " ").trim();
      String newDocNormalized = newDoc.replaceAll("\\s+", " ").replaceAll("\\W+", " ").trim();
      if (!existingNormalized.equals(newDocNormalized)) {
        throw new VerifyException(
            String.format(
                "Verification of doc file %s failed.\nMake sure you update the file by running the"
                    + " following commands:\n\n%s",
                file.getAbsolutePath(), fixCmd()),
            new VerifyException(
                "File content differed from expected.\n"
                    + diff(existingNormalized, newDocNormalized)));
      }
      logger.atInfo().log("Successfully verified %s at %s", path, file.getAbsolutePath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String fixCmd() {
    String google3 =
        gradleProjectPath.contains("google3")
            ? gradleProjectPath.substring(gradleProjectPath.lastIndexOf("google3"))
            : gradleProjectPath;
    String cd = String.format("cd %s", google3);
    String gradle = String.format("gradle %s", DocgenPlugin.GENERATE_TASK_NAME);
    String gradleHint =
        "If you do not have gradle installed, you can use gradlew in"
            + " //third_party/gradle/wrapper_files/gradlew";
    return String.join("\n", cd, gradle, gradleHint);
  }

  private String diff(String existing, String incoming) {
    int contextSize = 100;
    int minSnippetLen = Math.min(existing.length(), incoming.length());
    for (int i = 0; i < minSnippetLen; i++) {
      if (existing.charAt(i) == incoming.charAt(i)) {
        continue;
      }

      return String.format(
          "Was:       ...%s...\nShould be: ...%s...\n",
          existing.substring(
              Math.max(i - contextSize, 0), Math.min(i + contextSize, existing.length())),
          incoming.substring(
              Math.max(i - contextSize, 0), Math.min(i + contextSize, incoming.length())));
    }
    int extra = minSnippetLen;
    return String.format(
        "Was:       %s\nShould be: %s\n",
        extra >= existing.length() ? "<Past EOF>" : existing.substring(extra),
        extra >= incoming.length() ? "<Past EOF>" : incoming.substring(extra));
  }
}
