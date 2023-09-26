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

import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.mergeMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.sourceMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.symbolMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.symbolRef;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SymbolReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for Metadata presence in various messages. */
@RunWith(Parameterized.class)
public class MetaTest {

  private final String whistle;
  private final Meta expectedCallMeta;
  private final List<Meta> expectedFunctionMetas;
  private final Function<WhistleParser, RuleContext> rule;
  private final Function<Message, Meta> metaField;

  public MetaTest(
      String name,
      String whistle,
      Meta expectedCallMeta,
      List<Meta> expectedFunctionMetas,
      Function<WhistleParser, RuleContext> rule,
      Function<Message, Meta> metaField) {
    this.whistle = whistle;
    this.expectedCallMeta = expectedCallMeta;
    this.expectedFunctionMetas = expectedFunctionMetas;
    this.rule = rule;
    this.metaField = metaField;
  }

  @Parameters(name = "meta - {0}")
  public static Collection<Object[]> data() {
    return ImmutableList.copyOf(
        new Object[][] {
          {
            "simple block",
            "{\n\tfield: 1;\n\t// Comment\n\tfield2: \"two\";\n}",
            sourceMeta(1, 0, 5, 0),
            Arrays.asList(sourceMeta(1, 0, 5, 0, FunctionType.BLOCK)),
            (Function<WhistleParser, RuleContext>) WhistleParser::block,
            (Function<Message, Meta>) MetaTest::getValueSourceFunctionCallMeta
          },
          {
            "nested block",
            "{\n\tvar x: 1;\n\t{\n\t\tvar x: 2\n\t}\n}",
            sourceMeta(1, 0, 6, 0),
            Arrays.asList(
                sourceMeta(3, 1, 5, 1, FunctionType.BLOCK),
                sourceMeta(1, 0, 6, 0, FunctionType.BLOCK)),
            (Function<WhistleParser, RuleContext>) WhistleParser::block,
            (Function<Message, Meta>) MetaTest::getValueSourceFunctionCallMeta
          },
          {
            "path",
            "testVar.field[0]",
            mergeMeta(
                sourceMeta(1, 0, 1, 15),
                symbolMeta(
                    1,
                    0,
                    1,
                    6,
                    "testVar",
                    "testRoot",
                    false,
                    false,
                    SymbolReference.Type.VARIABLE)),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::expression,
            (Function<Message, Meta>)
                m -> mergeMeta(getValueSourceFunctionCallMeta(m), ((ValueSource) m).getMeta())
          },
          {
            "path with selector",
            "testVar.field[where $.field > 0]",
            mergeMeta(
                sourceMeta(1, 13, 1, 31),
                symbolMeta(
                    1,
                    0,
                    1,
                    6,
                    "testVar",
                    "testRoot",
                    false,
                    false,
                    SymbolReference.Type.VARIABLE)),
            ImmutableList.of(sourceMeta(1, 20, 1, 30, FunctionType.LAMBDA)),
            (Function<WhistleParser, RuleContext>) WhistleParser::expression,
            (Function<Message, Meta>)
                m -> mergeMeta(getValueSourceFunctionCallMeta(m), ((ValueSource) m).getMeta())
          },
          {
            "mapping statement - end with newline",
            "var target: testVar.foo.bar\n",
            mergeMeta(
                sourceMeta(1, 0, 1, 26),
                symbolMeta(
                    1, 4, 1, 9, "target", "testRoot", true, false, SymbolReference.Type.VARIABLE)),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::statement,
            (Function<Message, Meta>) s -> ((FieldMapping) s).getMeta()
          },
          {
            "mapping statement - end with multiple newline",
            "var target: testVar.foo.bar\n\n",
            mergeMeta(
                sourceMeta(1, 0, 1, 26),
                symbolMeta(
                    1, 4, 1, 9, "target", "testRoot", true, false, SymbolReference.Type.VARIABLE)),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::statement,
            (Function<Message, Meta>) s -> ((FieldMapping) s).getMeta()
          },
          {
            "mapping statement - side kw",
            "side target: testVar.foo.bar\n",
            sourceMeta(1, 0, 1, 27),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::statement,
            (Function<Message, Meta>) s -> ((FieldMapping) s).getMeta()
          },
          // TODO(rpolyano): This should not be grammatically valid.
          {
            "mapping statement - index target",
            "var [123]: testVar.foo.bar\n",
            sourceMeta(1, 0, 1, 25),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::statement,
            (Function<Message, Meta>) s -> ((FieldMapping) s).getMeta()
          },
          {
            "function definition",
            "def func(a, b, c) a + b + c",
            mergeMeta(
                sourceMeta(1, 0, 1, 26, FunctionType.DECLARED),
                symbolMeta(1, 4, 1, 7, "func", true, SymbolReference.Type.FUNCTION)),
            Arrays.asList(
                mergeMeta(
                    sourceMeta(1, 0, 1, 26, FunctionType.DECLARED),
                    symbolMeta(1, 4, 1, 7, "func", true, SymbolReference.Type.FUNCTION))),
            (Function<WhistleParser, RuleContext>) WhistleParser::functionDef,
            (Function<Message, Meta>) v -> ((FunctionDefinition) v).getMeta()
          },
          {
            "function call",
            "func(1, 2, 3)",
            mergeMeta(
                sourceMeta(1, 0, 1, 12),
                symbolMeta(1, 0, 1, 3, "func", false, SymbolReference.Type.FUNCTION)),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::expression,
            (Function<Message, Meta>) MetaTest::getValueSourceFunctionCallMeta
          },
          {
            "function call with package",
            "pack::func(1, 2, 3)",
            mergeMeta(
                sourceMeta(1, 0, 1, 18),
                symbolMeta(
                    symbolRef(1, 0, 1, 3, "pack", false, SymbolReference.Type.PACKAGE),
                    symbolRef(1, 6, 1, 9, "func", false, SymbolReference.Type.FUNCTION))),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::expression,
            (Function<Message, Meta>) MetaTest::getValueSourceFunctionCallMeta
          },
          {
            "function call with package as custom sink",
            "pack::func(1, 2, 3): testVar.foo.bar\n",
            mergeMeta(
                sourceMeta(1, 0, 1, 35),
                symbolMeta(
                    symbolRef(1, 0, 1, 3, "pack", false, SymbolReference.Type.PACKAGE),
                    symbolRef(1, 6, 1, 9, "func", false, SymbolReference.Type.FUNCTION))),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::statement,
            (Function<Message, Meta>) s -> ((FieldMapping) s).getMeta()
          },
          {
            "operator call",
            "10 + 10",
            sourceMeta(1, 0, 1, 6),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::expression,
            (Function<Message, Meta>) MetaTest::getValueSourceFunctionCallMeta
          },
          {
            "implied package",
            "\n",
            symbolMeta(1, 0, 1, 0, "$default", true, SymbolReference.Type.PACKAGE),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, Meta>) p -> ((PipelineConfig) p).getMeta()
          },
          {
            "explicit package",
            "package \"hello_world\"\n",
            symbolMeta(1, 8, 1, 20, "hello_world", true, SymbolReference.Type.PACKAGE),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, Meta>) p -> ((PipelineConfig) p).getMeta()
          },
          {
            "explicit identifier package",
            "package hello_world\n",
            symbolMeta(1, 8, 1, 18, "hello_world", true, SymbolReference.Type.PACKAGE),
            new ArrayList<Meta>(),
            (Function<WhistleParser, RuleContext>) WhistleParser::program,
            (Function<Message, Meta>) p -> ((PipelineConfig) p).getMeta()
          },
          {
              "string interpolation",
              "\"boop{10 + 10}\"",
              sourceMeta(1, 0, 1, 14),
              new ArrayList<Meta>(),
              (Function<WhistleParser, RuleContext>) WhistleParser::string,
              (Function<Message, Meta>) MetaTest::getValueSourceFunctionCallMeta
          },
        });
  }

  private static Meta getValueSourceFunctionCallMeta(Message v) {
    return ((ValueSource) v).getFunctionCall().getMeta();
  }

  @Test
  public void test() {
    Environment env = new Environment("testRoot");
    env.declareOrInheritVariable("testVar");
    Transpiler t = new Transpiler(env);

    Message got = t.transpile(whistle, rule);
    Meta gotMeta = metaField.apply(got);

    List<Meta> gotFunctionMetas =
        t.getAllFunctions().stream().map(FunctionDefinition::getMeta).collect(Collectors.toList());
    assertEquals(expectedCallMeta, gotMeta);
    assertEquals(expectedFunctionMetas, gotFunctionMetas);
  }
}
