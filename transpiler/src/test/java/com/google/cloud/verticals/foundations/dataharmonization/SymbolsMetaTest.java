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
package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.mergeMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.sourceMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.symbolMeta;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SymbolReference;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Symbols;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for Metadata presence in various messages. */
@RunWith(Parameterized.class)
public class SymbolsMetaTest {

  private final String whistle;
  private final List<Meta> expectedSymbolMeta;
  private final Function<WhistleParser, RuleContext> rule;
  private final Function<Message, List<Meta>> getSymbolMetas;

  public SymbolsMetaTest(
      String name,
      String whistle,
      List<Meta> expectedSymbolMeta,
      Function<WhistleParser, RuleContext> rule,
      Function<Message, List<Meta>> getSymbolMetas) {
    this.whistle = whistle;
    this.expectedSymbolMeta = expectedSymbolMeta;
    this.rule = rule;
    this.getSymbolMetas = getSymbolMetas;
  }

  @Parameters(name = "meta - {0}")
  public static ImmutableList<Object[]> data() {
    return ImmutableList.copyOf(
        new Object[][] {
          {
            "nested blocks",
            "nested_blocks.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(4, 2, 4, 9),
                    symbolMeta(4, 6, 4, 6, "x", "1", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(2, 0, 2, 7),
                    symbolMeta(2, 4, 2, 4, "x", "2", true, false, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "nested blocks with reference to variables",
            "nested_blocks_ref_vars.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(4, 2, 4, 9),
                    symbolMeta(4, 6, 4, 6, "x", "1", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(5, 5, 5, 5, "x", "1", false, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(2, 0, 2, 7),
                    symbolMeta(2, 4, 2, 4, "x", "2", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(7, 5, 7, 5, "x", "2", false, false, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "writing vars",
            "write_var.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(1, 0, 1, 7),
                    symbolMeta(1, 4, 1, 4, "x", "0", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(2, 0, 2, 7),
                    symbolMeta(2, 4, 2, 4, "x", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(3, 0, 3, 10),
                    symbolMeta(3, 4, 3, 6, "x", "0", false, true, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "nested writing vars",
            "nested_write_vars.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(1, 0, 1, 7),
                    symbolMeta(1, 4, 1, 4, "x", "0", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(7, 0, 7, 7),
                    symbolMeta(7, 4, 7, 4, "x", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(8, 3, 8, 3, "x", "0", false, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(3, 2, 3, 9),
                    symbolMeta(3, 6, 3, 6, "x", "1", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(4, 2, 4, 9),
                    symbolMeta(4, 6, 4, 6, "x", "1", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(5, 6, 5, 6, "x", "1", false, false, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "writing vars with paths",
            "write_var_with_paths.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(1, 0, 1, 9),
                    symbolMeta(1, 4, 1, 4, "x", "0", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(2, 0, 2, 9),
                    symbolMeta(2, 4, 2, 4, "x", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(3, 4, 3, 4, "x", "0", false, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(4, 0, 4, 9),
                    symbolMeta(4, 4, 4, 4, "y", "0", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(5, 0, 5, 9),
                    symbolMeta(5, 4, 5, 4, "y", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(6, 0, 6, 9),
                    symbolMeta(6, 4, 6, 4, "y", "0", false, true, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "nested writing vars with paths",
            "nested_write_var_with_paths.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(1, 0, 1, 9),
                    symbolMeta(1, 4, 1, 4, "x", "0", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(7, 0, 7, 9),
                    symbolMeta(7, 4, 7, 4, "x", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(8, 3, 8, 3, "x", "0", false, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(3, 2, 3, 11),
                    symbolMeta(3, 6, 3, 6, "x", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(4, 2, 4, 11),
                    symbolMeta(4, 6, 4, 6, "x", "0", false, true, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(5, 6, 5, 6, "x", "0", false, false, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "function overload variables",
            "function_overloads.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(2, 2, 2, 9),
                    symbolMeta(2, 6, 2, 6, "x", "1", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(3, 5, 3, 5, "x", "1", false, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(6, 2, 6, 9),
                    symbolMeta(6, 6, 6, 6, "x", "2", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    symbolMeta(7, 5, 7, 5, "x", "2", false, false, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>) p -> getAllSymbolMetas((PipelineConfig) p)
          },
          {
            "function overload argument symbols",
            "function_overloads.wstl",
            Arrays.asList(
                mergeMeta(
                    sourceMeta(1, 9, 1, 9),
                    symbolMeta(
                        1, 9, 1, 9, "a", "<init>", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(5, 9, 5, 9),
                    symbolMeta(
                        5, 9, 5, 9, "a", "<init>", true, false, SymbolReference.Type.VARIABLE)),
                mergeMeta(
                    sourceMeta(5, 11, 5, 11),
                    symbolMeta(
                        5, 11, 5, 11, "b", "<init>", true, false, SymbolReference.Type.VARIABLE))),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, List<Meta>>)
                p ->
                    ((PipelineConfig) p)
                        .getFunctionsList().stream()
                            .map(FunctionDefinition::getArgsList)
                            .flatMap(Collection::stream)
                            .collect(toImmutableList())
                            .stream()
                            .map(Argument::getMeta)
                            .collect(toImmutableList())
          }
        });
  }

  @Test
  public void test() throws IOException {
    Transpiler t = new Transpiler();
    String wstlInput = Resources.toString(Resources.getResource("metatest/" + whistle), UTF_8);
    Message got = t.transpile(wstlInput, rule);
    List<Meta> gotMetas = getSymbolMetas.apply(got);
    List<String> environments = getEnvironmentsFromPipelineConfig((PipelineConfig) got);

    // Update the expected metas environment placeholders.
    List<Meta> expectedSymbolMetaWithEnvironment = new ArrayList<>();
    for (Meta expectedMeta : expectedSymbolMeta) {
      expectedSymbolMetaWithEnvironment.add(
          updateEnvironmentPlaceHolders(expectedMeta, environments));
    }
    assertEquals(expectedSymbolMetaWithEnvironment, gotMetas);
  }

  private List<String> getEnvironmentsFromPipelineConfig(PipelineConfig p) {
    List<String> environments = new ArrayList<>();
    environments.add(p.getRootBlock().getName());

    ImmutableList<String> nestedEnvironments =
        p.getFunctionsList().stream().map(FunctionDefinition::getName).collect(toImmutableList());
    environments.addAll(nestedEnvironments);

    return environments;
  }

  /**
   * Replaces environment placeholders with the actual environment names which are generated during
   * creation of the Pipeline proto object. Environment IDs for blocks and function calls make use
   * of a random UUID and can not be hardcoded until after the pipeline has been created. This
   * method will take in a list of environments that are present in the pipeline proto, ordered by
   * appearance in the pipeline proto, and replaces the placeholders in expectedMeta with the
   * corresponding environment value stored at that list index.
   *
   * @param expectedMeta The expectedMeta object with placeholders for environment ids.
   * @param environments List of environments in the main pipeline proto in order of appearance.
   * @return The updated meta with environment placeholders replaced with the actual environment id.
   */
  private static Meta updateEnvironmentPlaceHolders(Meta expectedMeta, List<String> environments)
      throws InvalidProtocolBufferException {
    String environment =
        expectedMeta
            .getEntriesOrThrow(Symbols.getDescriptor().getFullName())
            .unpack(Symbols.class)
            .getSymbols(0)
            .getEnvironment();
    try {
      int envIndex = Integer.parseInt(environment);
      SymbolReference symbolRef =
          expectedMeta
              .getEntriesOrThrow(Symbols.getDescriptor().getFullName())
              .unpack(Symbols.class)
              .getSymbols(0)
              .toBuilder()
              .setEnvironment(environments.get(envIndex))
              .build();

      Symbols symbol =
          expectedMeta
              .getEntriesOrThrow(Symbols.getDescriptor().getFullName())
              .unpack(Symbols.class)
              .toBuilder()
              .setSymbols(0, symbolRef)
              .build();

      return expectedMeta.toBuilder()
          .putEntries(Symbols.getDescriptor().getFullName(), Any.pack(symbol))
          .build();
    } catch (NumberFormatException e) {
      // Return the original meta, with no replacements.
      return expectedMeta;
    }
  }

  private static List<Meta> getAllSymbolMetas(PipelineConfig pipelineConfig) {
    List<Meta> allMetas = new ArrayList<>();

    ImmutableList<Meta> rootMetas =
        pipelineConfig.getRootBlock().getMappingList().stream()
            .filter(fm -> fm.hasVar() || fm.getValue().hasFromLocal())
            .map(fm -> fm.hasVar() ? fm.getMeta() : fm.getValue().getMeta())
            .collect(toImmutableList());
    allMetas.addAll(rootMetas);

    ImmutableList<Meta> innerMetas =
        pipelineConfig.getFunctionsList().stream()
            .map(FunctionDefinition::getMappingList)
            .flatMap(Collection::stream)
            .collect(toImmutableList())
            .stream()
            .filter(fm -> fm.hasVar() || fm.getValue().hasFromLocal())
            .map(fm -> fm.hasVar() ? fm.getMeta() : fm.getValue().getMeta())
            .collect(toImmutableList());
    allMetas.addAll(innerMetas);

    return allMetas;
  }
}
