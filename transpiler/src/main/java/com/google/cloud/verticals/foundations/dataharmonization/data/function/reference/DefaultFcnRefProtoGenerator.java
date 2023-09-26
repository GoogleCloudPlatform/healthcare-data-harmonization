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
package com.google.cloud.verticals.foundations.dataharmonization.data.function.reference;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;

/** Generates FunctionReference proto for registered functions. */
public class DefaultFcnRefProtoGenerator implements FcnRefProtoGenerator {

  private final String packageName;
  private final String functionName;

  public DefaultFcnRefProtoGenerator(String packageName, String functionName) {
    this.packageName = packageName;
    this.functionName = functionName;
  }

  public DefaultFcnRefProtoGenerator(String functionName) {
    this(TranspilerData.BUILTIN_PKG, functionName);
  }

  @Override
  public FunctionReference getFunctionReferenceProto() {
    return FunctionReference.newBuilder().setPackage(packageName).setName(functionName).build();
  }
}
