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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.FileLoader;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Entry point for simple JSON to JSON transformation. */
public final class Main {
  private static final Options options = new Options();
  private static final String STDOUT = "stdout";

  static {
    options.addOption("help", "Prints this message.");
    options.addOption(
        list(new Option("i", "input_file_spec", true, "Absolute paths to input JSON files.")));
    options.addOption("o", "output_dir", true, "Absolute path to directory to output JSON files.");

    Option mapping =
        new Option(
            "m",
            "mapping_file_spec",
            true,
            "Absolute path to the mapping file to apply to each input JSON file.");
    mapping.setRequired(true);
    options.addOption(mapping);
  }

  private static Option list(Option opt) {
    opt.setArgs(Option.UNLIMITED_VALUES);
    opt.setValueSeparator(',');
    return opt;
  }

  public static void main(String[] args) throws Exception {
    try {
      CommandLineParser parser = new DefaultParser();
      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption("help")) {
        printHelp();
        return;
      }

      Path outputDir =
          FileSystems.getDefault().getPath(cmd.hasOption("o") ? cmd.getOptionValue("o") : STDOUT);
      if (cmd.hasOption("o")) {
        File outputDirFile = outputDir.toFile();
        if (!outputDirFile.exists()) {
          if (!outputDirFile.mkdirs()) {
            throw new IOException(String.format("Failed to create directory %s.", outputDir));
          }
        } else if (!outputDirFile.isDirectory()) {
          throw new IllegalArgumentException(
              String.format("Output directory %s exists but was not a directory.", outputDir));
        }
      }

      Map<Path, Data> inputData = new HashMap<>();
      if (cmd.hasOption("i")) {
        String[] filePaths = cmd.getOptionValues("i");
        inputData =
            stream(filePaths)
                .collect(Collectors.toMap(FileSystems.getDefault()::getPath, Main::readJson));
      } else {
        inputData.put(outputDir.resolve("default.json"), NullData.instance);
      }

      Path mappingPath = FileSystems.getDefault().getPath(cmd.getOptionValue("m"));
      ImportPath mappingImportPath =
          ImportPath.of(FileLoader.NAME, mappingPath, mappingPath.getParent());

      for (Path inputPath : inputData.keySet()) {
        Data input = inputData.get(inputPath);
        try (Engine engine =
            new Engine.Builder(ExternalConfigExtractor.of(mappingImportPath))
                .initialize()
                .build()) {
          Data output = engine.transform(input);
          writeJson(
              outputDir.resolve(
                  inputPath.getFileName().toString().replace(".json", ".output.json")),
              output);
        }
      }
    } catch (ParseException ex) {
      System.err.println(ex.getMessage());
      printHelp();
    }
  }

  private static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("runtime", options);
  }

  private static void writeJson(Path path, Data output) throws IOException {
    try (final OutputStream fos =
        path.getName(0).toString().equals(STDOUT)
            ? System.out
            : new FileOutputStream(path.toFile())) {
      fos.write(prettyPrintToJson(output).getBytes(UTF_8));
    }
  }

  private static String prettyPrintToJson(Data data) {
    byte[] json = new JsonSerializerDeserializer().serialize(data);
    Gson prettyPrinter = new GsonBuilder().setPrettyPrinting().create();
    return prettyPrinter.toJson(prettyPrinter.fromJson(new String(json, UTF_8), JsonElement.class));
  }

  private static Data readJson(String path) {
    File file = new File(path);
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] json = ByteStreams.toByteArray(fis);
      return new JsonSerializerDeserializer().deserialize(json);
    } catch (IOException e) {
      System.err.printf("Unable to read file %s%n", path);
      e.printStackTrace(System.err);
      return NullData.instance;
    }
  }

  private Main() {}
}
