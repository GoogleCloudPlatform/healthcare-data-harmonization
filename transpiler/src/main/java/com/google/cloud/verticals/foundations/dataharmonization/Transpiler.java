/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FILE_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SOURCE_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.buildSource;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.functionMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.sourceMeta;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleVocabulary.ARRAY_APPEND;
import static com.google.cloud.verticals.foundations.dataharmonization.symbols.SymbolHelper.withImpliedPackageDefSymbol;
import static com.google.cloud.verticals.foundations.dataharmonization.symbols.SymbolHelper.withSymbol;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FunctionNames;
import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.LambdaFuncNames;
import com.google.cloud.verticals.foundations.dataharmonization.data.Functions;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.error.ErrorStrategy;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationIssue;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.VariableTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.targets.TargetProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleBaseVisitor;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ArgAliasContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.BlockContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ConstStrContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprBlockContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprConditionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprInfixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprInfixOpLambdaContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprPostfixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprPrefixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprSourceContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExpressionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionCallContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionCallSourceContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionDefContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionNameSimpleContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionNameWildcardContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ImportFunctionCallContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ImportStrContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.InputSourceContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.InterpStrContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.OptionStatementContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.PathFieldNameContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.PathIndexContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.PathSelectorContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ProgramContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SelectorContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceConstBoolContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceConstNumContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceConstStrContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceExpressionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceInputContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceListInitContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourcePathContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourcePathSegmentContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.StatementContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetCustomSinkContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetStaticContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * A Transpiler traverses the AST generated by Whistle Parser, keeps track of the variable binding
 * in each lexical scope and produce whistle proto using the information from the AST.
 */
public class Transpiler extends WhistleBaseVisitor<AbstractMessage> {

  private static final ExpressionContext placeholder = new ExpressionContext();

  ArrayList<FunctionDefinition> allFunctions;
  Environment environment;
  FileInfo fileInfo;
  ErrorStrategy strategy;
  final Boolean throwTranspileException;
  // Map of variable names to a set of environments. Used to map variable definitions to the
  // environments they are declared in.
  private final Map<String, Set<String>> variableDefinitionEnvironments = new HashMap<>();

  public Transpiler() {
    this(new Environment(TranspilerData.INIT_ENV_NAME), true);
  }

  public Transpiler(Boolean throwTranspileException) {
    this(new Environment(TranspilerData.INIT_ENV_NAME), throwTranspileException);
  }

  public Transpiler(Environment env) {
    this(env, true);
  }

  public Transpiler(Environment env, Boolean throwTranspileException) {
    this.environment = env;
    this.allFunctions = new ArrayList<>();
    this.throwTranspileException = throwTranspileException;
  }

  List<FunctionDefinition> getAllFunctions() {
    return Collections.unmodifiableList(allFunctions);
  }

  /**
   * Transpiles whistle program in string representation to proto message. Serves as the main entry
   * point of the transpiler.
   *
   * @param whistle Whistle code in String representation.
   * @return Transpiled proto message.
   */
  public PipelineConfig transpile(String whistle, FileInfo info) {
    return (PipelineConfig) this.transpile(whistle, WhistleParser::program, info);
  }

  /**
   * Transpiles whistle code using a specific parser rule. Mainly used for unit testing.
   *
   * @param whistle the whistle code to transpile
   * @param rule the parser rule used at the top level
   * @param info location info for the source of the whistle code.
   * @return the transpiled proto
   */
  public AbstractMessage transpile(
      String whistle, Function<WhistleParser, RuleContext> rule, FileInfo info) {
    this.fileInfo = info;
    AbstractMessage visitParseTreeResult = null;

    CharStream stream = CharStreams.fromString(whistle);
    WhistleLexer wstlLexer = new WhistleLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(wstlLexer);

    WhistleParser wstlParser = new WhistleParser(tokens);
    this.strategy = new ErrorStrategy(info);
    wstlParser.setErrorHandler(strategy);

    RuleContext r = rule.apply(wstlParser);
    if (strategy.hasIssues() && throwTranspileException) {
      throw new TranspilationException(strategy.getIssues());
    }

    RuntimeException supressedEx = null;
    try {
      visitParseTreeResult = r.accept(this);
    } catch (RuntimeException ex) {
      // ANTLR will continue parsing even if there is a syntax error, and this might make the syntax
      // structure incorrect, causing NPE or some other exceptions down the line. This catch is
      // meant to catch those NPEs/others, then check for syntax errors that caused them.
      supressedEx = ex;
    }
    if (strategy.hasIssues() && throwTranspileException) {
      TranspilationException tex = new TranspilationException(strategy.getIssues());
      if (supressedEx != null) {
        tex.addSuppressed(supressedEx);
      }
      throw tex;
    } else if (supressedEx != null) {
      throw supressedEx;
    }
    return visitParseTreeResult;
  }

  /**
   * Transpiles whistle code using a specific parser rule. Mainly used for unit testing.
   *
   * @param whistle the whistle code to transpile
   * @param rule the parser rule used at the top level
   * @return the transpiled proto
   */
  AbstractMessage transpile(String whistle, Function<WhistleParser, RuleContext> rule) {
    return transpile(whistle, rule, null);
  }

  /**
   * Visit a parse tree produced by the {@code ConstStr} labeled alternative in {@link
   * WhistleParser#string}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitConstStr(ConstStrContext ctx) {
    String str = TranspilerHelper.preprocessString(ctx.CONST_STRING().getText());
    return ValueSource.newBuilder().setConstString(str).build();
  }

  /**
   * Visit a parse tree produced by the {@code InterpStr} labeled alternative in {@link
   * WhistleParser#string}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitInterpStr(InterpStrContext ctx) {
    StringBuilder ctxTemplate = new StringBuilder();
    // create template string as the first argument to string interpolation function
    // e.g. "hello {expr1} and {expr2}." will become "hello {} and {}."
    ctxTemplate.append(
        ctx.STRING_FRAGMENT_INTERP_START().getText().substring(1)); // trim string start quote
    for (TerminalNode seg : ctx.STRING_FRAGMENT_INTERP_MIDDLE()) {
      ctxTemplate.append(seg.getText());
    }
    ctxTemplate.append(
        ctx.STRING_FRAGMENT_INTERP_END().getText(),
        0,
        ctx.STRING_FRAGMENT_INTERP_END().getText().length() - 1); // trim tailing quote
    String template =
        TranspilerHelper.preprocessString(
            TranspilerHelper.generateTemplate(ctxTemplate.toString()));
    List<ValueSource> args =
        new ArrayList<>(
            ImmutableList.of(ValueSource.newBuilder().setConstString(template).build()));
    if (!args.addAll(
        ctx.expression().stream()
            .map(expr -> (ValueSource) expr.accept(this))
            .collect(toImmutableList()))) {
      throw new IllegalArgumentException(
          String.format(
                  "Some expressions are not successfully added as args to %s",
                  FunctionNames.STR_INTERP)
              + ctx.getText());
    }

    Meta.Builder meta = sourceMeta(buildSource(ctx));
    return rebuildFunctionCallWithIterationSupport(
        meta,
        FunctionCall.newBuilder()
            .setMeta(meta)
            .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
            .addAllArgs(args)
            .build());
  }

  /**
   * Visit a parse tree produced by {@code FunctionNameSimple} labeled alternative in {@link
   * WhistleParser#functionName}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitFunctionNameSimple(FunctionNameSimpleContext ctx) {
    String fcnName = TranspilerHelper.preprocessString(ctx.functionIdentifier().getText());
    String pkgName =
        ctx.identifier().stream()
            .map(ParseTree::getText)
            .map(TranspilerHelper::preprocessString)
            .collect(joining(TranspilerData.PKG_REF_DELIM));
    return FunctionReference.newBuilder().setPackage(pkgName).setName(fcnName).build();
  }

  /**
   * Visit a parse tree produced by {@code FunctionNameWildcard} labeled alternative in {@link
   * WhistleParser#functionName}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitFunctionNameWildcard(FunctionNameWildcardContext ctx) {
    String fcnName = TranspilerHelper.preprocessString(ctx.functionIdentifier().getText());
    String pkgName = ctx.MULT().getText();
    return FunctionReference.newBuilder().setPackage(pkgName).setName(fcnName).build();
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#program}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitProgram(ProgramContext ctx) {
    String packageName = defaultPackageName();
    if (ctx.packageName() != null) {
      packageName =
          ctx.packageName().identifier() != null
              ? ctx.packageName().identifier().getText()
              : ctx.packageName().CONST_STRING().getText();
      packageName = TranspilerHelper.preprocessString(packageName);
    }

    ImmutableList<Import> imports =
        ctx.importStatement().stream().map(i -> (Import) i.accept(this)).collect(toImmutableList());

    ImmutableList<Option> options =
        ctx.optionStatement().stream().map(o -> (Option) o.accept(this)).collect(toImmutableList());

    String rootEnvName = String.format(TranspilerData.ROOT_FUNCTION_NAME_FORMAT, packageName);
    pushEnv(
        rootEnvName,
        Collections.singletonList(Argument.newBuilder().setName(TranspilerData.ROOT_VAR).build()));

    ImmutableList<FieldMapping> rootMappings =
        ctx.statement().stream().map(s -> (FieldMapping) s.accept(this)).collect(toImmutableList());
    FunctionDefinition rootBlock =
        this.environment
            .generateDefinition(false, rootMappings)
            .setMeta(functionMeta(ctx.statement(), FunctionType.ROOT))
            .build();
    this.environment = this.environment.getParent();

    ctx.functionDef().forEach(f -> f.accept(this));

    PipelineConfig.Builder config =
        PipelineConfig.newBuilder()
            .setPackageName(packageName)
            .setMeta(
                ctx.packageName() != null
                    ? withSymbol(Meta.newBuilder(), ctx.packageName())
                    : withImpliedPackageDefSymbol(Meta.newBuilder(), packageName))
            .addAllImports(imports)
            .addAllOptions(options)
            .setRootBlock(rootBlock)
            .addAllFunctions(this.allFunctions);
    if (fileInfo != null) {
      config.setMeta(Meta.newBuilder().putEntries(FILE_META_KEY, Any.pack(fileInfo)));
    }

    return config.build();
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#argAlias}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitArgAlias(ArgAliasContext ctx) {
    Argument.Builder argBuilder = Argument.newBuilder();
    if (ctx.argMod() != null && !ctx.argMod().getText().trim().isEmpty()) {
      argBuilder.setModifier(ctx.argMod().getText());
    }

    String name = TranspilerHelper.preprocessString(ctx.identifier().getText());
    Meta meta =
        withSymbol(sourceMeta(buildSource(ctx)), ctx, environment, variableDefinitionEnvironments)
            .build();
    return argBuilder.setName(name).setMeta(meta).build();
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#functionDef}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitFunctionDef(FunctionDefContext ctx) {
    try {
      String name = TranspilerHelper.preprocessString(ctx.functionIdentifier().getText());
      ImmutableList<Argument> arguments =
          ctx.argAlias().stream().map(a -> (Argument) a.accept(this)).collect(toImmutableList());

      pushEnv(name, arguments);

      ImmutableList<FieldMapping> mappings;
      if (ctx.expression().getRuleContext() instanceof ExprBlockContext) {
        // Inline the block rather than generating a new lambda.
        mappings =
            getBlockFieldMappings(((ExprBlockContext) ctx.expression().getRuleContext()).block());
      } else {
        ValueSource body = (ValueSource) ctx.expression().accept(this);
        mappings = ImmutableList.of(FieldMapping.newBuilder().setValue(body).build());
      }

      FunctionDefinition def =
          environment
              .generateDefinition(false, mappings)
              .setMeta(withSymbol(functionMeta(ctx, FunctionType.DECLARED), ctx))
              .build();
      allFunctions.add(def);

      environment = environment.getParent();

      return def;
    } catch (NullPointerException npe) {
      // To handle the case where when we are ignoring Transpilation Exceptions, we return a null
      // functionDef for an invalid function so the file can continue to be parsed.
      // Valid functions still get loaded, invalid functions get ignored.
      if (throwTranspileException) {
        throw npe;
      }
      return FunctionDefinition.getDefaultInstance();
    }
  }

  /**
   * Visit a parse tree produced by the {@code ExprCondition} labeled alternative in link
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprCondition(ExprConditionContext ctx) {
    return ValueSource.newBuilder()
        .setFunctionCall(
            Functions.getSignature(Functions.TERNARY_REF, ctx.expression().size())
                .transpileFunctionCall(this, Functions.TERNARY_REF, ctx.expression(), ctx))
        .build();
  }

  /**
   * Visit a parse tree produced by the {@code ExprInfixOpLambda} labeled alternative in
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprInfixOpLambda(ExprInfixOpLambdaContext ctx) {
    String op = ctx.infixOperator4().getText();
    FunctionReference opRef = Functions.getOperatorRef(op);
    FunctionCall call =
        Functions.getSignature(opRef, ctx.expression().size())
            .transpileFunctionCall(this, opRef, ctx.expression(), ctx);

    return rebuildFunctionCallWithIterationSupport(sourceMeta(buildSource(ctx)), call);
  }

  /**
   * Visit a parse tree produced by the {@code ExprPrefixOp} labeled alternative in
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprPrefixOp(ExprPrefixOpContext ctx) {
    String op = ctx.prefixOperator().getText();
    FunctionReference opRef = Functions.getOperatorRef(op);
    FunctionCall call =
        Functions.getSignature(opRef, 1)
            .transpileFunctionCall(this, opRef, Collections.singletonList(ctx.expression()), ctx);

    return rebuildFunctionCallWithIterationSupport(sourceMeta(buildSource(ctx)), call);
  }

  /**
   * Visit a parse tree produced by the {@code ExprPostfixOp} labeled alternative in
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprPostfixOp(ExprPostfixOpContext ctx) {
    String op = ctx.postfixOperator().getText();
    FunctionReference opRef = Functions.getOperatorRef(op);
    FunctionCall call =
        Functions.getSignature(opRef, 1)
            .transpileFunctionCall(this, opRef, Collections.singletonList(ctx.expression()), ctx);

    return rebuildFunctionCallWithIterationSupport(sourceMeta(buildSource(ctx)), call);
  }

  /**
   * Visit a parse tree produced by the {@code ExprBlock} labeled alternative in
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprBlock(ExprBlockContext ctx) {
    return ctx.block().accept(this);
  }

  /**
   * Visit a parse tree produced by the {@code ExprSource} labeled alternative in
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprSource(ExprSourceContext ctx) {
    return ctx.source().accept(this);
  }

  /**
   * Visit a parse tree produced by the {@code ExprInfixOp} labeled alternative in link
   * WhistleParser#expression.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitExprInfixOp(ExprInfixOpContext ctx) {
    ParseTree opTree = ctx.infixOperator1();
    if (opTree == null) {
      opTree = ctx.infixOperator2();
    }
    if (opTree == null) {
      opTree = ctx.infixOperator3();
    }

    String op = opTree.getText();
    FunctionReference opRef = Functions.getOperatorRef(op);
    FunctionCall call =
        Functions.getSignature(opRef, ctx.expression().size())
            .transpileFunctionCall(this, opRef, ctx.expression(), ctx);

    return rebuildFunctionCallWithIterationSupport(sourceMeta(buildSource(ctx)), call);
  }

  /**
   * Visit a parse tree produced by {@code ImportStr} labeled alternative in {@link
   * WhistleParser#importStatement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitImportStr(ImportStrContext ctx) {
    return TranspilerHelper.constructImport(
        (ValueSource) ctx.string().accept(this), buildSource(ctx), ctx.string().getText());
  }

  /**
   * Visit a parse tree produced by {@code ImportFunctionCall} labeled alternative in {@link
   * WhistleParser#importStatement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitImportFunctionCall(ImportFunctionCallContext ctx) {
    return TranspilerHelper.constructImport(
        (ValueSource) ctx.functionCall().accept(this),
        buildSource(ctx),
        ctx.functionCall().getText());
  }

  @Override
  public AbstractMessage visitOptionStatement(OptionStatementContext ctx) {
    return Option.newBuilder()
        .setName(TranspilerHelper.preprocessString(ctx.CONST_STRING().getText()))
        .setMeta(sourceMeta(buildSource(ctx)))
        .build();
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#block}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitBlock(BlockContext ctx) {
    String blockName = LambdaFuncNames.BLOCK + LambdaHelper.getUUIDString();
    environment = environment.createChild(blockName);
    environment.declareLocalVariables(ImmutableList.of(TranspilerData.THIS));

    ImmutableList<FieldMapping> fieldMappings = getBlockFieldMappings(ctx);

    // Set inheritParentVars to true, as a block should inherit the parent's context, and any vars.
    FunctionDefinition body =
        environment
            .generateDefinition(true, fieldMappings)
            .setMeta(functionMeta(ctx, FunctionType.BLOCK))
            .build();
    allFunctions.add(body);

    FunctionCall call = environment.generateInvocation(/* closure= */ false, buildSource(ctx));

    environment = environment.getParent();
    return ValueSource.newBuilder().setFunctionCall(call).build();
  }

  /** Transpile the statements in the given block context within the current environment. */
  private ImmutableList<FieldMapping> getBlockFieldMappings(BlockContext ctx) {
    return ctx.statement().stream()
        .map(s -> (FieldMapping) s.accept(this))
        .collect(toImmutableList());
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#functionCall}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitFunctionCall(FunctionCallContext ctx) {
    FunctionReference ref = (FunctionReference) ctx.functionName().accept(this);
    FunctionCall call =
        Functions.getSignature(ref, ctx.expression().size())
            .transpileFunctionCall(this, ref, ctx.expression(), ctx);

    return rebuildFunctionCallWithIterationSupport(
        withSymbol(sourceMeta(buildSource(ctx)), ctx), call);
  }

  private ValueSource rebuildFunctionCallWithIterationSupport(
      Meta.Builder meta, FunctionCall call) {
    List<ValueSource> args = call.getArgsList();

    boolean iterated = args.stream().anyMatch(ValueSource::getIterate);

    if (iterated) {
      // Rewrite myFunc(arg1, arg2[], arg3, arg4[]) as
      // $Iterate(<myFunc closure>, arg2, arg4)
      ValueSource closure =
          TranspilerHelper.constructFunctionCallVS(
              call.getReference(),
              meta,
              true,
              Streams.mapWithIndex(
                      args.stream(),
                      (value, index) ->
                          value.getIterate()
                              ? ValueSource.newBuilder()
                                  .setFreeParameter(String.format("iteratedArg%d", index))
                                  .build()
                              : value)
                  .collect(toImmutableList()));

      List<ValueSource> iterateArgs =
          args.stream()
              .filter(ValueSource::getIterate)
              .map(a -> ValueSource.newBuilder(a).setIterate(false).build())
              .collect(toCollection(ArrayList::new));
      iterateArgs.add(0, closure);

      return TranspilerHelper.constructFunctionCallVS(
          FunctionReference.newBuilder()
              .setName(FunctionNames.ITERATE_FUNC)
              .setPackage(TranspilerData.BUILTIN_PKG)
              .build(),
          meta,
          false,
          iterateArgs);
    }

    return ValueSource.newBuilder().setFunctionCall(call).build();
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#statement}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitStatement(StatementContext ctx) {
    if (ctx.exception != null && !throwTranspileException) {
      // Return a default message if there is an exception and throwTranspileException is set
      // to false.
      return FieldMapping.getDefaultInstance();
    }
    ValueSource source = (ValueSource) ctx.expression().accept(this);

    // Build meta before processing target, so we can check if the var is going to be a declaration.
    Meta.Builder meta =
        withSymbol(
            Meta.newBuilder().putEntries(SOURCE_META_KEY, Any.pack(buildSource(ctx))),
            ctx.target(),
            environment,
            variableDefinitionEnvironments);

    AbstractMessage target = ctx.target() != null ? ctx.target().accept(this) : null;
    FieldMapping.Builder builder = FieldMapping.newBuilder().setValue(source);
    // add statement level debug info
    builder.setMeta(meta);
    if (target instanceof VariableTarget) {
      builder.setVar((VariableTarget) target);
    } else if (target instanceof FieldTarget) {
      builder.setField((FieldTarget) target);
    } else if (target instanceof FunctionCall) {
      builder.setCustomSink((FunctionCall) target);
    }

    if (source.getIterate()) {
      builder.setIterateSource(true);
      source = ValueSource.newBuilder(source).setIterate(false).build();
      builder.setValue(source);
    }
    // TODO(): Implement modifiers.

    return builder.build();
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#selector}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSelector(SelectorContext ctx) {
    throw new UnsupportedOperationException("selector should not be called directly");
  }

  /**
   * Visit a parse tree produced by the {@code SourceConstNum} labeled alternative in {@link
   * WhistleParser#source}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourceConstNum(SourceConstNumContext ctx) {
    ValueSource.Builder vsBuilder = ValueSource.newBuilder();
    if (ctx.num().INTEGER().size() == 1) {
      vsBuilder.setConstInt(Integer.parseInt(ctx.num().getText()));
    } else if (ctx.num().INTEGER().size() == 2) {
      vsBuilder.setConstFloat(Double.parseDouble(ctx.num().getText()));
    } else {
      throw new IllegalArgumentException(ctx.getText());
    }
    return vsBuilder.build();
  }

  /**
   * Visit a parse tree produced by the {@code SourceInput} labeled alternative in {@link
   * WhistleParser#source}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourceInput(SourceInputContext ctx) {
    return ctx.sourcePath().accept(this);
  }

  /**
   * Visit a parse tree produced by the {@code SourceConstStr} labeled alternative in {@link
   * WhistleParser#source}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourceConstStr(SourceConstStrContext ctx) {
    return ctx.string().accept(this);
  }

  /**
   * Visit a parse tree produced by the {@code SourceConstBool} labeled alternative in {@link
   * WhistleParser#source}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourceConstBool(SourceConstBoolContext ctx) {
    return ValueSource.newBuilder().setConstBool(ctx.BOOL().getText().equals("true")).build();
  }

  /**
   * Visit a parse tree produced by the {@code SourceExpression} labeled alternative in {@link
   * WhistleParser#source}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourceExpression(SourceExpressionContext ctx) {
    return ctx.expression().accept(this);
  }

  /**
   * Visit a parse tree produced by the {@code SourceListInit} labeled alternative in {@link
   * WhistleParser#source}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourceListInit(SourceListInitContext ctx) {
    return TranspilerHelper.constructFunctionCallVS(
        FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto(),
        sourceMeta(buildSource(ctx)),
        /* buildClosure= */ false,
        ctx.expression().stream()
            .map(expr -> (ValueSource) expr.accept(this))
            .collect(toImmutableList()));
  }

  /**
   * Visit a parse tree produced by {@link WhistleParser#sourcePath}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitSourcePath(SourcePathContext ctx) {
    // read path head
    ValueSource currentValue = (ValueSource) ctx.sourcePathHead().accept(this);
    Source currentSource = buildSource(ctx);

    boolean iterate = false;
    // start to process path segment
    // 1. trim tailing [] and set iterate field if it exists
    List<SourcePathSegmentContext> segs = new ArrayList<>(ctx.sourcePathSegment());
    if (!segs.isEmpty() && Iterables.getLast(segs).getText().equals("[]")) {
      iterate = true;
      segs = segs.subList(0, segs.size() - 1);
    }
    // 2. set up accumulator for segments
    StringBuilder path = new StringBuilder();
    // 3. loop through all path segment
    for (SourcePathSegmentContext seg : segs) {
      // 3.1 if the new segment is field or index -> append it to segment
      if (seg instanceof PathFieldNameContext) {
        String delim = ((PathFieldNameContext) seg).fieldName().PATH_DELIM().getText();
        String field = ((PathFieldNameContext) seg).fieldName().getText().substring(delim.length());
        path.append(delim);
        path.append(TranspilerHelper.preprocessString(field));
      } else if (seg instanceof PathIndexContext) {
        if (seg.getText().equals("[]")) {
          throw new IllegalArgumentException(
              "Having iteration in the middle of source path "
                  + ctx.getText()
                  + " is currently not supported. Please consider using equivalent wildcard"
                  + " expression.");
        }
        path.append(((PathIndexContext) seg).getText());
      } else if (seg instanceof PathSelectorContext) {
        // 3.2 if the new segment is an selector
        // 3.2.1 wrap the previous in get
        ValueSource selectee = TranspilerHelper.getPathOnValue(currentValue, currentSource, path);

        currentSource = buildSource(seg);

        PathSelectorContext pseg = (PathSelectorContext) seg;
        FunctionReference selectorRef =
            (FunctionReference) pseg.selector().functionName().accept(this);
        if (selectorRef.getPackage().isEmpty()) {
          selectorRef = FunctionReference.newBuilder(selectorRef).setPackage("*").build();
        }
        // Use a placeholder for the first argument since it comes from outside the selector:
        // selectee[selector <selector expr>] needs to become
        // selector(selectee, <selector expr closure>). We've already transpiled selectee, so we
        // sub in a placeholder expression and replace it below.
        // TODO(rpolyano): Consider using a oneof class instead here.
        List<ExpressionContext> args = Arrays.asList(placeholder, pseg.selector().expression());
        FunctionCall call =
            Functions.getSignature(selectorRef, args.size())
                .transpileFunctionCall(this, selectorRef, args, seg);
        call = FunctionCall.newBuilder(call).setArgs(0, selectee).build();
        currentValue = ValueSource.newBuilder().setFunctionCall(call).build();

        path = new StringBuilder();
      } else {
        throw new IllegalArgumentException("Unexpected path segment type " + seg);
      }
    }

    ValueSource result = TranspilerHelper.getPathOnValue(currentValue, currentSource, path);
    ValueSource.Builder resultBuilder = result.toBuilder();
    resultBuilder.setMeta(
        withSymbol(
            resultBuilder.getMetaBuilder(), ctx, environment, variableDefinitionEnvironments));
    return resultBuilder.setIterate(iterate).build();
  }

  /**
   * Visit a parse tree produced by the {@code InputSource} labeled alternative in {@link
   * WhistleParser#sourcePathHead}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitInputSource(InputSourceContext ctx) {
    // Pass in the inputSourceCtx, fileInfo and strategy. These will be used to generate a
    // transpilationIssue, which may get added to strategy.
    return this.environment.readVar(ctx, fileInfo, strategy);
  }

  /**
   * Visit a parse tree produced by the {@code FunctionCallSource} labeled alternative in {@link
   * WhistleParser#sourcePathHead}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitFunctionCallSource(FunctionCallSourceContext ctx) {
    return ctx.functionCall().accept(this);
  }

  /**
   * Visit a parse tree produced by the {@code TargetStatic} labeled alternative in {@link
   * WhistleParser#target}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitTargetStatic(TargetStaticContext ctx) {
    String targetKw = ctx.targetKeyword() != null ? ctx.targetKeyword().getText() : "";
    TargetProtoGenerator generator = TranspilerData.getTargetGenerator(targetKw);

    String name =
        ctx.targetPath() != null
            ? TranspilerHelper.preprocessString(ctx.targetPath().targetPathHead().getText())
            : "";
    String path = TranspilerHelper.extractPath(ctx.targetPath());

    if (ctx.targetMergeMode() != null) {
      if (path.contains(ARRAY_APPEND)) {
        // Add the error but return valid result.
        strategy.addIssue(
            new TranspilationIssue(
                fileInfo,
                ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine() + 1,
                ctx.getStop().getLine(),
                ctx.getStop().getCharPositionInLine(),
                String.format(
                    "Merge modes ('%s') cannot be used with array appends %s. Appends will create"
                        + " new elements at the end of the array, so there is nothing to be merged"
                        + " with at that point.",
                    ctx.targetMergeMode().getText(), ARRAY_APPEND)));
      }
      return generator.createWithMerge(name, path, environment, ctx.targetMergeMode().getText());
    }
    return generator.create(name, path, environment);
  }

  /**
   * Visit a parse tree produced by the {@code TargetCustomSink} labeled alternative in {@link
   * WhistleParser#target}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  @Override
  public AbstractMessage visitTargetCustomSink(TargetCustomSinkContext ctx) {
    ValueSource functionCallVs = (ValueSource) ctx.functionCall().accept(this);
    // TODO(floraliuyf): Make visitFunctionCall return a FunctionCall directly. Caller should put it
    //  in a ValueSource.
    return functionCallVs.getFunctionCall();
  }

  /**
   * Visit the children of a node, and return a user-defined result of the operation.
   *
   * @param node The {@link RuleNode} whose children should be visited.
   * @return The result of visiting the children of the node.
   */
  @Override
  public AbstractMessage visitChildren(RuleNode node) {
    if (node instanceof ExpressionContext
        && ((ExpressionContext) node).exception != null
        && !throwTranspileException) {
      // Return a default instance if there is an exception at this visitor.
      return ValueSource.getDefaultInstance();
    }
    if (node.equals(placeholder)) {
      return ValueSource.getDefaultInstance();
    }
    throw new UnsupportedOperationException("This method should not be called");
  }

  /**
   * Pushes a new environment to the stack and sets it as the current environment.
   *
   * @param name The name of the environment to push.
   * @param args Arguments to declare in the generated function.
   */
  public void pushEnv(String name, List<Argument> args) {
    environment = environment.createChild(name, args);
  }

  /**
   * Pops the current environment from the stack and returns it (setting the current environment to
   * the parent).
   */
  public Environment popEnv() {
    Environment popped = environment;
    environment = environment.getParent();
    return popped;
  }

  /** Adds the given function definition to the current list of functions. */
  public void addFunction(FunctionDefinition def) {
    allFunctions.add(def);
  }

  private String defaultPackageName() {
    if (this.fileInfo == null || fileInfo.getUrl().length() == 0) {
      return "$default";
    }

    URI uri = URI.create(fileInfo.getUrl());
    Path path = FileSystems.getDefault().getPath(uri.getPath());
    String name = path.getFileName().toString();
    return name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
  }
}
