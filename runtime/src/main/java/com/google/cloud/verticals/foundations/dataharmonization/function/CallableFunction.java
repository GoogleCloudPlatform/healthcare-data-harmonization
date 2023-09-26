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

package com.google.cloud.verticals.foundations.dataharmonization.function;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.function.BiFunction;

/** Represents the implementation of a function that can be invoked to produce a result. */
public abstract class CallableFunction implements Registrable {

  public static CallableFunction identity() {
    return new CallableFunction() {
      @Override
      protected Data callInternal(RuntimeContext context, Data... args) {
        return context.getDataTypeImplementation().arrayOf(ImmutableList.copyOf(args));
      }

      @Override
      public Signature getSignature() {
        return new Signature(Builtins.PACKAGE_NAME, "identity", ImmutableList.of(Data.class), true);
      }

      @Override
      public DebugInfo getDebugInfo() {
        return DebugInfo.simpleFunction("", FunctionType.NATIVE);
      }
    };
  }

  /**
   * Executes this CallableFunction and returns its result.
   *
   * <p>This method will call {@link RuntimeContext#wrap(Signature, Data[], BiFunction)} to perform
   * any wrapping tasks before/after the actual logic of this CallableFunction ({@link
   * CallableFunction#callInternal(RuntimeContext, Data...)}) is executed.
   *
   * @param context The RuntimeContext to use.
   * @param args The arguments to the function.
   * @return The resut of the function call.
   */
  public final Data call(RuntimeContext context, Data... args) {
    return context.wrap(this, args, this::callInternal);
  }

  /**
   * Invokes this function to produce a result, derived from the given arguments. This method should
   * only perform the operation of this function. "Preparations" (such as pushing a stack frame to
   * the stack, wrapping with any try/catch/finally, checking argument types against the {@link
   * Signature} returned by {@link #getSignature()}, etc) should be performed before this method is
   * called.
   *
   * <p>This method's implementation must be thread-safe.
   *
   * @param context the RuntimeContext calling this method.
   * @param args the arguments for this function.
   * @return the result of the invocation.
   */
  protected abstract Data callInternal(RuntimeContext context, Data... args);

  /**
   * Returns the {@link Signature} of this method. That is, the description of the concrete
   * implementations/superinterfaces of {@link Data} that are expected by the args parameter {@link
   * #call(RuntimeContext, Data...)}.
   */
  public abstract Signature getSignature();

  @Override
  public String getName() {
    return getSignature().getName();
  }

  /**
   * Returns the package context to switch to while executing this function. By default just returns
   * the current PackageContext given.
   *
   * @param current the current package context.
   */
  public PackageContext getLocalPackageContext(PackageContext current) {
    return current;
  }

  public abstract DebugInfo getDebugInfo();

  @Override
  public int hashCode() {
    return Objects.hashCode(getSignature());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof CallableFunction
        && getSignature().equals(((CallableFunction) other).getSignature());
  }
}
