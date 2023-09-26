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

package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import java.io.Serializable;
import java.util.Set;

/**
 * Represent a single entry in a (call) stack of functions. Used for storing variables within the
 * scope of a function.
 */
public interface StackFrame extends Serializable {
  /**
   * Returns the variable with the given name, or {@link null} if it does not exist. If this stack
   * frame {@link #inheritsParentVars()} it should first check the parent for the var, and return
   * that if it exists. If two ancestors have the same variable, the first (i.e. closest) in the
   * stack is used.
   *
   * <p>Note that {@link WhistleFunction#OUTPUT_VAR} is a special case and should never be
   * inherited.
   *
   * <p>Note that there are no restrictions on the value of name, however it will be matched
   * exactly.
   */
  Data getVar(String name);

  /**
   * Overwrites the value of the variable with the given name with the given value. If this stack
   * frame {@link #inheritsParentVars()} it should first check the parent for the var, and if it
   * exists delegate the assignment to the parent instead. If two ancestors have the same variable,
   * the first (i.e. closest) in the stack is used.
   *
   * <p>Note that {@link WhistleFunction#OUTPUT_VAR} is a special case and should never be
   * inherited.
   */
  void setVar(String name, Data value);

  /**
   * Overwrites the value of the variable with the given name with the given value. Only writes to
   * this stack frame, no matter the value of {@link #inheritsParentVars()}.
   */
  void setLocalVar(String name, Data value);

  /** Returns this stack frame's parent (i.e. the stack frame just below it in the stack). */
  StackFrame getParent();

  /**
   * Returns a new {@link StackFrame.Builder} capable of building this implementation of {@link
   * StackFrame}.
   */
  Builder newBuilder();

  /** Returns a human readable name for this stack frame. */
  String getName();

  /**
   * Returns all variable names stored in this stack frame. Should not include inherited variables.
   */
  Set<String> getVars();

  /**
   * Returns true iff this stack frame inherits variables from the parent stack frame. See {@link
   * StackFrame#setVar(String, Data)} and {@link StackFrame#getVar(String)} for how this affects
   * behaviour.
   */
  boolean inheritsParentVars();

  /** Copies the entire stack and all variables (including those in parent frames). */
  StackFrame stackCopy();

  /** Returns the debugging info for the function that this stack frame was created for. */
  DebugInfo getDebugInfo();

  /** Returns the number of stack frames below it in the stack. **/
  int getCount();

  /** Builder for {@link StackFrame}. */
  interface Builder extends Serializable {

    /** Set the parent of the stack frame to be built. See {@link StackFrame#getParent()} */
    Builder setParent(StackFrame parent);

    /** Set the name of the stack frame to be built. See {@link StackFrame#getName()} */
    Builder setName(String name);

    /**
     * Set whether this stack frame shall inherit the parent stack frame's variables. See {@link
     * StackFrame#inheritsParentVars()}, as well as {@link StackFrame#setVar(String, Data)} and
     * {@link StackFrame#getVar(String)}.
     */
    Builder setInheritParentVars(boolean inheritParentVars);

    Builder setDebugInfo(DebugInfo debugInfo);

    /** build the stack frame with the properties set on the builder. */
    StackFrame build();
  }
}
