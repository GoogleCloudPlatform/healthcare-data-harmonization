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
package com.google.cloud.verticals.foundations.dataharmonization.symbols;

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SYMBOLS_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerHelper.preprocessString;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.buildSource;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.Environment;
import com.google.cloud.verticals.foundations.dataharmonization.TranspilerHelper;
import com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SymbolReference;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Symbols;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ArgAliasContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionCallContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionCallSourceContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionDefContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionIdentifierContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionNameContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionNameSimpleContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionNameWildcardContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.IdentifierContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.InputSourceContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.PackageNameContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourcePathContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetCustomSinkContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetStaticContext;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Helps add symbol meta info to messages. */
public class SymbolHelper {

  private SymbolHelper() {}

  public static Meta.Builder withSymbol(Meta.Builder meta, FunctionCallContext call) {
    FunctionNameContext functionNameCtx = call.functionName();

    String funcName;
    Source funcLocation;
    String packageName = null;
    Source packageLocation = null;

    if (functionNameCtx instanceof FunctionNameSimpleContext) {
      FunctionNameSimpleContext simple = ((FunctionNameSimpleContext) functionNameCtx);
      FunctionIdentifierContext nameToken = simple.functionIdentifier();
      if (!simple.identifier().isEmpty()) {
        packageName =
            simple.identifier().stream()
                .map(IdentifierContext::getText)
                .map(TranspilerHelper::preprocessString)
                .collect(joining(WhistleHelper.getTokenLiteral(WhistleLexer.PKG_REF)));
        packageLocation = buildSource(simple.identifier().get(0));
      }
      funcName = preprocessString(nameToken.getText());
      funcLocation = buildSource(nameToken);
    } else if (functionNameCtx instanceof FunctionNameWildcardContext) {
      FunctionNameWildcardContext wildcard = (FunctionNameWildcardContext) functionNameCtx;
      packageName = wildcard.MULT().getText();
      funcName = preprocessString(wildcard.functionIdentifier().getText());
      funcLocation = buildSource(wildcard.functionIdentifier());
    } else {
      throw new IllegalArgumentException(
          String.format(
              "%s was not a known parser rule for function name", functionNameCtx.getClass()));
    }

    ImmutableList.Builder<SymbolReference> symbols = ImmutableList.builder();
    if (packageLocation != null && packageName != null) {
      symbols.add(
          SymbolReference.newBuilder()
              .setName(packageName)
              .setPosition(packageLocation)
              .setDefinition(false)
              .setType(SymbolReference.Type.PACKAGE)
              .build());
    }

    symbols.add(
        SymbolReference.newBuilder()
            .setName(funcName)
            .setPosition(funcLocation)
            .setDefinition(false)
            .setType(SymbolReference.Type.FUNCTION)
            .build());

    return withSymbol(meta, symbols.build());
  }

  public static Meta.Builder withSymbol(Meta.Builder meta, FunctionDefContext def) {
    return withSymbol(
        meta,
        ImmutableList.of(
            SymbolReference.newBuilder()
                .setName(preprocessString(def.functionIdentifier().getText()))
                .setPosition(buildSource(def.functionIdentifier()))
                .setDefinition(true)
                .setType(SymbolReference.Type.FUNCTION)
                .build()));
  }

  public static Meta.Builder withSymbol(
      Meta.Builder meta,
      ArgAliasContext argAliasContext,
      Environment environment,
      Map<String, Set<String>> variableDefinitionEnvironments) {
    String argIdentifier = argAliasContext.identifier().getText();
    variableDefinitionEnvironments
        .computeIfAbsent(argIdentifier, v -> new HashSet<>())
        .add(environment.getName());

    return withSymbol(
        meta,
        ImmutableList.of(
            SymbolReference.newBuilder()
                .setName(argIdentifier)
                .setPosition(buildSource(argAliasContext))
                .setDefinition(true)
                .setType(SymbolReference.Type.VARIABLE)
                .setEnvironment(
                    getDefinedEnvironment(
                        argIdentifier, environment, variableDefinitionEnvironments))
                .build()));
  }

  public static Meta.Builder withSymbol(
      Meta.Builder meta,
      TargetContext target,
      Environment environment,
      Map<String, Set<String>> variableDefinitionEnvironments) {
    if (target == null) {
      return meta;
    }
    if (target instanceof TargetCustomSinkContext) {
      return withSymbol(meta, ((TargetCustomSinkContext) target).functionCall());
    } else if (!(target instanceof TargetStaticContext)) {
      throw new IllegalArgumentException(
          String.format("%s was not a known parser rule for target", target.getClass()));
    }

    TargetStaticContext staticContext = (TargetStaticContext) target;
    if (staticContext.targetKeyword() == null
        || staticContext.targetKeyword().VAR() == null
        || staticContext.targetPath().targetPathHead().identifier() == null) {
      return meta;
    }

    String baseVar =
        preprocessString(staticContext.targetPath().targetPathHead().identifier().getText());

    // Check if this target has a path segment (ex x.pathSegment) on a variable which may have
    // already been declared. If not then this is a new declaration, otherwise it is operating
    // on an already declared variable.
    boolean mergeOnDeclaredVar =
        variableDefinitionEnvironments.containsKey(baseVar)
            && !staticContext.targetPath().targetPathSegment().isEmpty();
    boolean isDef = !mergeOnDeclaredVar && !environment.hasVarInScope(baseVar);
    // Writing a variable which is already in scope.
    boolean isWrite = mergeOnDeclaredVar || environment.hasVarInScope(baseVar);

    if ((isDef || isWrite) && !mergeOnDeclaredVar) {

      variableDefinitionEnvironments
          .computeIfAbsent(baseVar, v -> new HashSet<>())
          .add(environment.getName());
    }

    // Get the defined environment for this target symbol. If a target is a definition or write
    // and is not performing a merge on an already declared variable then it's environment is
    // the environment currently being visited. Otherwise, we are visiting a merge on an already
    // declared variable and the search below will fetch the environment where the variable being
    // merging on was declared.
    String definedEnvironment =
        getDefinedEnvironment(baseVar, environment, variableDefinitionEnvironments);

    return withSymbol(
        meta,
        ImmutableList.of(
            SymbolReference.newBuilder()
                .setName(baseVar)
                .setPosition(buildSource(staticContext.targetPath().targetPathHead().identifier()))
                .setDefinition(isDef)
                .setIsWrite(isWrite)
                .setType(SymbolReference.Type.VARIABLE)
                .setEnvironment(definedEnvironment)
                .build()));
  }

  public static Meta.Builder withSymbol(
      Meta.Builder meta,
      SourcePathContext sourceInput,
      Environment environment,
      Map<String, Set<String>> variableDefinitionEnvironments) {
    if (sourceInput.sourcePathHead() instanceof FunctionCallSourceContext) {
      return withSymbol(
          meta, ((FunctionCallSourceContext) sourceInput.sourcePathHead()).functionCall());
    }

    InputSourceContext inputSource = (InputSourceContext) sourceInput.sourcePathHead();
    String var = preprocessString(inputSource.identifier().getText());
    if (!environment.hasVarInScope(var)) {
      // TODO(rpolyano): Is this a valid situation?
      return meta;
    }

    return withSymbol(
        meta,
        ImmutableList.of(
            SymbolReference.newBuilder()
                .setName(var)
                .setPosition(buildSource(inputSource.identifier()))
                .setDefinition(false)
                .setType(SymbolReference.Type.VARIABLE)
                .setEnvironment(
                    getDefinedEnvironment(var, environment, variableDefinitionEnvironments))
                .build()));
  }

  public static Meta.Builder withSymbol(Meta.Builder meta, PackageNameContext def) {
    Source position;
    String name;
    if (def.identifier() != null) {
      name = preprocessString(def.identifier().getText());
      position = buildSource(def.identifier());
    } else {
      name = preprocessString(def.CONST_STRING().getText());
      position = buildSource(def.CONST_STRING().getSymbol(), def.CONST_STRING().getSymbol());
    }

    return withSymbol(
        meta,
        ImmutableList.of(
            SymbolReference.newBuilder()
                .setName(name)
                .setPosition(position)
                .setDefinition(true)
                .setType(SymbolReference.Type.PACKAGE)
                .build()));
  }

  public static Meta.Builder withSymbol(
      Meta.Builder meta, Iterable<SymbolReference> newSymbolRefs) {
    try {
      Symbols.Builder symbols = Symbols.newBuilder();
      if (meta.containsEntries(SYMBOLS_META_KEY)) {
        symbols =
            Symbols.newBuilder(meta.getEntriesOrThrow(SYMBOLS_META_KEY).unpack(Symbols.class));
      }

      symbols.addAllSymbols(newSymbolRefs);

      meta.putEntries(SYMBOLS_META_KEY, Any.pack(symbols.build()));
      return meta;
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Given a variable and an environment, this helper method will search through the
   * variableDefinitionEnvironments map to determine which environment block this variable was
   * declared in. If nothing is found, the search is moved up to the parent environment. Ultimately
   * if nothing is found, this signals a local scoped variable.
   *
   * @param var The variable whose defined environment is being looked up
   * @param environment The current environment
   * @param variableDefinitionEnvironments Map which keeps track of environments where variables are
   *     defined
   * @return The environment where the variable was defined.
   */
  private static String getDefinedEnvironment(
      String var,
      Environment environment,
      Map<String, Set<String>> variableDefinitionEnvironments) {
    if (environment.getParent() == null) {
      return environment.getName();
    }
    if (variableDefinitionEnvironments.containsKey(var)
        && variableDefinitionEnvironments.get(var).contains(environment.getName())) {
      return environment.getName();
    }
    return getDefinedEnvironment(var, environment.getParent(), variableDefinitionEnvironments);
  }

  public static Meta.Builder withImpliedPackageDefSymbol(Meta.Builder meta, String name) {
    return withSymbol(
        meta,
        ImmutableList.of(
            SymbolReference.newBuilder()
                .setName(name)
                .setPosition(
                    Source.newBuilder()
                        .setStart(SourcePosition.newBuilder().setLine(1).setColumn(0))
                        .setEnd(SourcePosition.newBuilder().setLine(1).setColumn(0)))
                .setDefinition(true)
                .setType(SymbolReference.Type.PACKAGE)
                .build()));
  }
}
