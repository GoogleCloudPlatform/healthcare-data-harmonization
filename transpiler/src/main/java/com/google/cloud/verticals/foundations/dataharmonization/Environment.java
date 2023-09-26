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

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SOURCE_META_KEY;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.error.ErrorStrategy;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationIssue;
import com.google.cloud.verticals.foundations.dataharmonization.error.UndeclaredVariableException;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.InputSourceContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An Envrionment represents the lexical variable binding environment of a closure. It keeps track
 * of all variables declared so far in the current lexical scope and variables declared in parent
 * scopes that are required for any child scope.
 */
public class Environment {

  private final String name;
  private final boolean isRoot;
  private final Environment parent;
  // Variables declared in the current environment.
  private final LinkedHashSet<String> localVars;
  // Variables inherited from parent environment.
  private final LinkedHashSet<String> varFromParents;
  private final LinkedHashSet<Argument> args;

  protected Environment(
      String name,
      boolean isGlobal,
      Environment parent,
      Iterable<String> localVars,
      Iterable<String> fromParents,
      Iterable<Argument> args) {
    this.name = name;
    this.isRoot = isGlobal;
    this.parent = parent;
    this.localVars = Sets.newLinkedHashSet(localVars);
    this.varFromParents = Sets.newLinkedHashSet(fromParents);
    this.args = Sets.newLinkedHashSet(args);
    this.localVars.addAll(
        Sets.newHashSet(args).stream().map(Argument::getName).collect(Collectors.toList()));
  }

  /**
   * Generates a new Environment at root level.
   *
   * @param name: name for the environment.
   * @param args: Arguments declared in the environment.
   */
  public Environment(String name, Iterable<Argument> args) {
    this(name, true, null, new LinkedHashSet<>(), new LinkedHashSet<>(), args);
  }

  /**
   * Generates a new Environment at root level.
   *
   * @param name: name for the environment.
   */
  public Environment(String name) {
    this(name, true, null, new LinkedHashSet<>(), new LinkedHashSet<>(), ImmutableList.of());
  }

  /**
   * Returns an empty Environment that's a child of the current Environment.
   *
   * @param name Name of the child environment.
   * @param args: Arguments declared in the environment.
   */
  protected Environment createChild(String name, Iterable<Argument> args) {
    return new Environment(name, false, this, ImmutableSet.of("$this"), new HashSet<>(), args);
  }

  /**
   * Returns an empty Environment that's a child of the current Environment.
   *
   * @param name Name of the child environment.
   */
  protected Environment createChild(String name) {
    return createChild(name, ImmutableList.of());
  }

  /** Returns the parent environment of the current environment. */
  public Environment getParent() {
    return this.parent;
  }

  /**
   * Declare a variable at current environment, if it is not already present in a parent
   * environment.
   *
   * @param varName variable name to be declared.
   */
  public void declareOrInheritVariable(String varName) {
    requireNotNullOrBlank(varName);
    if (readVar(varName, false, null, null, null) == null) {
      this.localVars.add(varName);
    }
  }

  private String requireNotNullOrBlank(String varName) {
    if (varName == null) {
      throw new IllegalArgumentException("Variable name cannot be null.");
    }
    if (varName.trim().isEmpty()) {
      throw new IllegalArgumentException("Variable name cannot be empty.");
    }
    return varName;
  }

  /**
   * Declare all variables in the collection in the current environment. Duplicated elements in the
   * collection will only be added once.
   *
   * @param varNames collection of variable to be declared.
   */
  protected void declareLocalVariables(Collection<String> varNames) {
    this.localVars.addAll(
        varNames.stream().map(this::requireNotNullOrBlank).collect(Collectors.toList()));
  }

  /**
   * Returns true if the variable is already tracked in the current scope.
   *
   * @param varName Variable name to check.
   */
  public boolean hasVarInScope(String varName) {
    return this.localVars.contains(varName) || this.varFromParents.contains(varName);
  }

  /**
   * Returns a ValueSource proto of the variable if it's present in local environment. A helper
   * function for readVar.
   *
   * @param varName variable name to read.
   * @return ValueSource proto of the variable if it exists in the local environment and null if
   *     not.
   */
  private ValueSource readLocalVar(String varName) {
    return hasVarInScope(varName) ? ValueSource.newBuilder().setFromLocal(varName).build() : null;
  }

  /**
   * Returns a ValueSource proto of the variable if it's declared in the current environment or any
   * parent of the current environment. Variable is added to the fromParent fields of all parent
   * environments until the one that defines it.
   *
   * @param varName the name of the variable.
   * @return ValueSource of the variable.
   * @throws UndeclaredVariableException if it's not declared in any parents.
   */
  protected ValueSource readVar(String varName) {
    return readVar(varName, true, null, null, null);
  }

  /**
   * Used by the transpiler to read in an {@link InputSourceContext} and returns a ValueSource proto
   * of the variable if it's declared in the current environment or any parent of the current
   * environment. Variable is added to the fromParent fields of all parent environments until the
   * one that defines it. The InputSourceContext is used to generate meaningful error's should the
   * variable not be declared correctly. This error gets added to the {@link ErrorStrategy} object.
   *
   * @param inputCtx InputSourceContext passed in from the transpiler
   * @param fileInfo Used when generating a {@link UndeclaredVariableException}
   * @param strategy Object which holds a list of {@link TranspilationIssue}
   * @return ValueSource of the variable.
   * @throws UndeclaredVariableException if it's not declared in any parents.
   */
  protected ValueSource readVar(
      InputSourceContext inputCtx, FileInfo fileInfo, ErrorStrategy strategy) {
    return readVar(inputCtx.getText(), true, inputCtx, fileInfo, strategy);
  }

  /**
   * Returns a ValueSource proto of the variable if it's declared in the current environment or any
   * parent of the current environment. Variable is added to the fromParent fields of all parent
   * environments until the one that defines it.
   *
   * @param varName the name of the variable.
   * @param throwIfNotDeclared set true to throw an UndeclaredVariableException if the variable is
   *     not declared and inputCtx is null, instead of returning null.
   * @param inputCtx Used to generate error details should an UndeclaredVariableException occur.
   *     This exception will only be thrown in the case inputCtx is null. Otherwise, a {@link
   *     TranspilationIssue} gets added to the {@link ErrorStrategy} object and transpilation
   *     continues.
   * @param fileInfo Used when generating a {@link UndeclaredVariableException}
   * @param strategy Object which holds a list of {@link TranspilationIssue}
   * @return ValueSource of the variable, or null if it does not exist (and throwIfNotDelcared is
   *     false).
   * @throws UndeclaredVariableException if it's not declared in any parents and throwIfNotDeclared
   *     is true.
   */
  protected ValueSource readVar(
      String varName,
      boolean throwIfNotDeclared,
      InputSourceContext inputCtx,
      FileInfo fileInfo,
      ErrorStrategy strategy) {
    ValueSource vs = readLocalVar(varName);
    if (vs == null && this.parent != null) {
      vs = this.parent.readVar(varName, throwIfNotDeclared, inputCtx, fileInfo, strategy);
      if (vs != null) {
        this.varFromParents.add(varName);
      }
    }
    if (vs == null && throwIfNotDeclared && parent == null) {
      // Declare the variable, and add it to the list of transpilation issues
      declareOrInheritVariable(varName);
      vs = readVar(varName);
      UndeclaredVariableException exception =
          new UndeclaredVariableException(
              String.format("Variable %s is undeclared.", varName), inputCtx, fileInfo);

      if (inputCtx != null) {
        strategy.addIssue(exception.getTranspilationIssue());
      } else {
        // Throwing exception for unit tests.
        throw exception;
      }
    }

    return vs;
  }

  public boolean isRoot() {
    return isRoot;
  }

  public Set<String> getLocalVars() {
    return new HashSet<>(this.localVars);
  }

  public Set<String> getVarFromParents() {
    return new HashSet<>(varFromParents);
  }

  public String getName() {
    return this.name;
  }

  public FunctionCall generateInvocation(boolean closure, Source source, ValueSource... args) {
    if (args.length != this.args.size()) {
      throw new IllegalArgumentException(
          String.format(
              "Wrong number of arguments for %s: want %d, got %d",
              this.name, this.args.size(), args.length));
    }

    return FunctionCall.newBuilder()
        .setReference(FunctionReference.newBuilder().setName(this.name).build())
        .addAllArgs(Arrays.asList(args))
        .addAllArgs(
            this.varFromParents.stream().map(this.parent::readVar).collect(Collectors.toList()))
        .setMeta(Meta.newBuilder().putEntries(SOURCE_META_KEY, Any.pack(source)))
        .setBuildClosure(closure)
        .build();
  }

  public FunctionDefinition.Builder generateDefinition(
      boolean inheritParentVars, List<FieldMapping> body) {
    return FunctionDefinition.newBuilder()
        .setName(this.name)
        .setInheritParentVars(inheritParentVars)
        .addAllArgs(this.args)
        .addAllArgs(
            this.varFromParents.stream()
                .map(v -> Argument.newBuilder().setName(v).build())
                .collect(Collectors.toList()))
        .addAllMapping(body);
  }

  @Override
  public String toString() {
    return "Environment{"
        + "name='"
        + name
        + '\''
        + ", isRoot="
        + isRoot
        + ", localVars="
        + localVars
        + ", varFromParents="
        + varFromParents
        + ", parent="
        + parent.getName()
        + '}';
  }
}
