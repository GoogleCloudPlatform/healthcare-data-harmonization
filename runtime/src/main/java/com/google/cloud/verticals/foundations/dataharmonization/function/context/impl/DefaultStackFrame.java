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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleStackOverflowError;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Default implementation of a stack frame. */
public class DefaultStackFrame implements StackFrame {
  private final HashMap<String, Data> variables = new HashMap<>();
  private final StackFrame parent;
  private final String name;
  private final boolean inheritParentVars;
  private final DebugInfo debugInfo;
  private final int count;

  // varToParentCaches are built on demand, and are thus transient. They do not contain any data
  // that can't be reconstructed when needed.
  private transient Map<String, StackFrame> varToParentCacheReads;
  private transient Map<String, StackFrame> varToParentCacheWrites;

  private DefaultStackFrame(
      StackFrame parent, String name, boolean inheritParentVars, DebugInfo debugInfo) {
    this.parent = parent;
    this.name = name;
    this.inheritParentVars = inheritParentVars;
    this.debugInfo = debugInfo;
    this.count = parent == null ? 0 : parent.getCount() + 1;
  }

  @Override
  public Data getVar(String name) {
    if (variables.containsKey(name)) {
      return variables.get(name);
    }
    if (inheritParentVars
        && canInheritVar(name)
        && getVarToParentCache(/* reads */ true).containsKey(name)) {
      return getVarToParentCache(/* reads */ true).get(name).getVar(name);
    }

    return NullData.instance;
  }

  private boolean canInheritVar(String name) {
    // The output var is a special case. Each stack frame should have its own.
    return !WhistleFunction.OUTPUT_VAR.equals(name);
  }

  /**
   * Returns or generates a cache that maps variable names to the stack frames that contain them.
   *
   * @param reads true iff the cache should map the closest stack frame that contains a variable or
   *     the furthest down the stack. Otherwise the cache should map the furthest stack frame.
   */
  private Map<String, StackFrame> getVarToParentCache(boolean reads) {
    Map<String, StackFrame> varToParentCache =
        reads ? varToParentCacheReads : varToParentCacheWrites;
    if (varToParentCache == null) {
      varToParentCache = new HashMap<>();
      StackFrame ancestor = parent;
      // Traverse up the stack until we reach a stack frame that does not inherit parent vars.
      while (ancestor != null) {
        // Map all the vars in the ancestor stackframe (not inherited ones) to that ancestor.
        // Iff reads is true, then we skip any vars that are already mapped (since that means they
        // were shadowed at closer/lower ancestor frame), i.e. reads use closest first semantics,
        // writes use furthest first semantics.
        // This means we won't have to travel up the stack again to find this var in the future.
        for (String var : ancestor.getVars()) {
          if (reads && varToParentCache.containsKey(var)) {
            continue;
          }
          varToParentCache.put(var, ancestor);
        }

        ancestor = ancestor.inheritsParentVars() ? ancestor.getParent() : null;
      }

      if (reads) {
        varToParentCacheReads = varToParentCache;
      } else {
        varToParentCacheWrites = varToParentCache;
      }
    }

    return varToParentCache;
  }

  @Override
  public void setVar(String name, Data value) {
    if (inheritParentVars
        && canInheritVar(name)
        && getVarToParentCache(/* reads */ false).containsKey(name)) {
      getVarToParentCache(/* reads */ false).get(name).setLocalVar(name, value);
      return;
    }

    variables.put(name, value);
  }

  @Override
  public void setLocalVar(String name, Data value) {
    variables.put(name, value);
  }

  @Override
  public StackFrame getParent() {
    return parent;
  }

  @Override
  public Builder newBuilder() {
    return new DefaultBuilder().setParent(this);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<String> getVars() {
    return variables.keySet();
  }

  @Override
  public boolean inheritsParentVars() {
    return inheritParentVars;
  }

  @Override
  public StackFrame stackCopy() {
    StackFrame parentCopy = parent == null ? null : parent.stackCopy();
    StackFrame copy = new DefaultBuilder()
        .setName(name)
        .setParent(parentCopy)
        .setInheritParentVars(inheritParentVars)
        .setDebugInfo(debugInfo)
        .build();
    variables.forEach(copy::setLocalVar);
    return copy;
  }

  @Override
  public DebugInfo getDebugInfo() {
    return debugInfo;
  }

  @Override
  public int getCount() {
    return count;
  }

  /**
   * Non-generated override of equals method to provide a logical equivalence check for
   * DefaultStackFrame.
   *
   * @param object The object on which to execute the comparison
   * @return true if equal, otherwise false
   */
  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof DefaultStackFrame)) {
      return false;
    }
    DefaultStackFrame that = (DefaultStackFrame) object;
    return Objects.equals(this.getVars(), that.getVars())
        && Objects.equals(this.getParent(), that.getParent())
        && Objects.equals(this.getName(), that.getName())
        && this.inheritParentVars == that.inheritParentVars
        && this.count == that.count;
  }

  public static final StackFrame EMPTY_FRAME =
      new DefaultStackFrame.DefaultBuilder().setName("EmptyFrame").build();

  /**
   * Non-generated Override of hashCode on the class to provide the basis for testing for logical
   * equivalence.
   *
   * @return int hash value of the class.
   */
  @Override
  public int hashCode() {
    return Objects.hash(variables, parent, name, inheritParentVars);
  }

  /** Builder for {@link DefaultStackFrame}. */
  @VisibleForTesting
  public static class DefaultBuilder implements StackFrame.Builder {

    public static final int STACK_FRAMES_LIMIT = 480;

    private StackFrame parent;
    private String name;
    private boolean inheritParentVars;
    private DebugInfo debugInfo;

    @CanIgnoreReturnValue
    @Override
    public Builder setParent(StackFrame parent) {
      this.parent = parent;
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder setInheritParentVars(boolean inheritParentVars) {
      this.inheritParentVars = inheritParentVars;
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder setDebugInfo(DebugInfo debugInfo) {
      this.debugInfo = debugInfo;
      return this;
    }

    @Override
    public StackFrame build() {
      DefaultStackFrame stackFrame = new DefaultStackFrame(parent, name, inheritParentVars,
          this.debugInfo);
      if (stackFrame.getCount() >= STACK_FRAMES_LIMIT) {
        throw new WhistleStackOverflowError(stackFrame);
      }
      return stackFrame;
    }
  }
}
