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

package com.google.cloud.verticals.foundations.dataharmonization.function.signature;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Signature contains information about a function's expected argument types. This class also
 * contains utilities for handling variadic parameters.
 *
 * <p>See {@link CallableFunction#getSignature()}
 */
public final class Signature implements Serializable {
  private final List<Class<? extends Data>> args;
  private final boolean isVariadic;
  private final boolean inheritsParentVars;
  private final String name;
  private final String packageName;
  /**
   * Creates a new Signature.
   *
   * @param packageName The package name of the signatory function.
   * @param name The name of the signatory function.
   * @param args The argument types expected by the signatory function represented by this instance.
   * @param isVariadic Whether the (last argument of the) signature is variadic.
   */
  public Signature(
      String packageName,
      @Nonnull String name,
      @Nonnull List<Class<? extends Data>> args,
      boolean isVariadic) {
    this(packageName, name, args, isVariadic, /* inheritsParentArgs */ false);
  }

  /**
   * Creates a new Signature.
   *
   * @param packageName The package name of the signatory function
   * @param name The name of the signatory function.
   * @param args The argument types expected by the signatory function represented by this instance.
   * @param isVariadic Whether the (last argument of the) signature is variadic.
   * @param inheritsParentVars Whether this function's stack frame should be able to access
   */
  public Signature(
      @Nonnull String packageName,
      @Nonnull String name,
      @Nonnull List<Class<? extends Data>> args,
      boolean isVariadic,
      boolean inheritsParentVars) {
    this.packageName = packageName;
    this.name = name;
    this.args = args;
    this.isVariadic = isVariadic;
    this.inheritsParentVars = inheritsParentVars;
  }

  /**
   * Returns true iff this function's stack frame should be able to access variables in the parent
   * stack frame. Useful for anonymous/inline functions and blocks.
   */
  public boolean getInheritsParentVars() {
    return inheritsParentVars;
  }

  /** Returns the name of the signatory function. */
  public String getName() {
    return name;
  }

  public String getPackageName() {
    return packageName;
  }

  /** Returns the argument types expected by the signatory function represented by this instance. */
  public List<Class<? extends Data>> getArgs() {
    return Collections.unmodifiableList(args);
  }

  /**
   * Gets the type of the last argument in the signature. Useful for extracting variadic parameters.
   */
  public Class<? extends Data> getLastArgType() {
    return Iterables.getLast(args);
  }

  /** Returns whether the (last argument of the) signature is variadic. */
  public boolean isVariadic() {
    return isVariadic;
  }

  @Override
  public String toString() {
    return String.format(
        "%s(%s%s)",
        name,
        args.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")),
        isVariadic ? "..." : "");
  }

  // Auto-generated equality members:
  @Override
  public int hashCode() {
    return Objects.hash(args, isVariadic, name, packageName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Signature signature = (Signature) o;
    return isVariadic == signature.isVariadic
        && args.equals(signature.args)
        && name.equals(signature.name)
        && packageName.equals(signature.packageName);
  }
}
