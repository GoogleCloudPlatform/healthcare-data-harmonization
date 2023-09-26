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
package com.google.cloud.verticals.foundations.dataharmonization.data.registry;

import com.google.cloud.verticals.foundations.dataharmonization.data.function.reference.FcnRefProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.error.UndeclaredVariableException;
import com.google.cloud.verticals.foundations.dataharmonization.error.UnrecognizedOperatorException;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import java.util.Map;

/**
 * Keeps track of a map from function lexical token name to its FunctionReference Proto generator.
 *
 * @param <T> The type of function Signature to register
 */
public class FunctionRegistry<T extends FcnRefProtoGenerator> {
  protected final Map<String, T> registry;

  public FunctionRegistry(Map<String, T> registry) {
    this.registry = registry;
  }

  public T getSymbolSignature(String symbol) throws UndeclaredVariableException {
    if (!registry.containsKey(symbol)) {
      throw new UnrecognizedOperatorException("Unrecognized symbol " + symbol);
    }
    return registry.get(symbol);
  }

  public FunctionReference getSymbolReference(String symbol) {
    return this.getSymbolSignature(symbol).getFunctionReferenceProto();
  }
}
