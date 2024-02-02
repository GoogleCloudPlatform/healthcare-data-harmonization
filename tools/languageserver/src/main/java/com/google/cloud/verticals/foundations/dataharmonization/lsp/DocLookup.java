/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.lsp;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.lsp.SymbolLookup.SymbolWithPosition;
import com.google.common.flogger.GoogleLogger;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

/** Class which deals with returning documentation related requests */
public final class DocLookup {
  private DocLookup() {}

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  //TODO(): Replace this with the constant in the processor once it can be builds with blaze.
  private static final String VAR_ARGS_FUNCTIONS = "VAR_ARGS";

  /**
   * Search the classpath and return any documentation for a given SymbolWithPosition object.
   *
   * @param symbol The SymbolWithPosition object whose documentation is being search
   * @return Associated documentation if it exists, null otherwise.
   */
  @Nullable
  public static String getMarkdownFromSymbol(SymbolWithPosition symbol) {
    String functionName = symbol.getSymbolReference().getName();
    int numArgs = symbol.getNumArgs();
    return searchForFunctionDoc(functionName, numArgs);
  }

  /**
   * Search the classpath and return any documentation for a given CallableFunction.
   *
   * @param callableFunction The callable function whose documentation is being search
   * @return Associated documentation if it exists, null otherwise.
   */
  @Nullable
  public static String getMarkdownFromCallableFunction(CallableFunction callableFunction) {
    String functionName = callableFunction.getName();
    int numArgs = callableFunction.getSignature().getArgs().size();
    return searchForFunctionDoc(functionName, numArgs);
  }

  /**
   * Searches the classpath for documentation given a function name and the number of arguments the
   * function takes. If nothing is found, it searches for a VAR_ARGS based function, if nothing is
   * still found, returns null.
   *
   * @param functionName The function for which documentation is being searched.
   * @param numArgs The number of args this function takes.
   */
  @Nullable
  private static String searchForFunctionDoc(String functionName, int numArgs) {
    try {
      ClassLoader loader = DocLookup.class.getClassLoader();
      InputStream res = loader.getResourceAsStream(functionName + "_" + numArgs);
      if (res != null) {
        return new String(res.readAllBytes(), UTF_8);
      } else {
        // Check for a varArg type function/target
        res = loader.getResourceAsStream(functionName + "_" + VAR_ARGS_FUNCTIONS);
        if (res != null) {
          return new String(res.readAllBytes(), UTF_8);
        }
      }
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Error while reading markdown definition for function during hover operation");
      return null;
    }
    return null;
  }
}
