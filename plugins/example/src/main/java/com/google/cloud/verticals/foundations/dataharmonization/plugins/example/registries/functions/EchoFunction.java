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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.registries.functions;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.ExamplePlugin;
import com.google.common.collect.ImmutableList;

/**
 * This class implements an example function which extends {@link CallableFunction}, and is used to
 * showcase how a custom function registry may be implemented to return instances of functions which
 * are not registered to it during function lookup. This function simply returns the name of itself
 * when callInternal is called.
 */
public class EchoFunction extends CallableFunction {

  private final String functionName;

  public EchoFunction(String functionName) {
    this.functionName = functionName;
  }

  @Override
  protected Data callInternal(RuntimeContext context, Data... args) {
    return context.getDataTypeImplementation().primitiveOf(this.functionName);
  }

  @Override
  public Signature getSignature() {
    return new Signature(ExamplePlugin.NAME, this.functionName, ImmutableList.of(), false);
  }

  @Override
  public DebugInfo getDebugInfo() {
    return DebugInfo.simpleFunction(this.functionName, FunctionType.NATIVE);
  }
}
