/*
 * Copyright 2023 Google LLC.
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
import com.google.common.flogger.GoogleLogger;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

/** Class which deals with returning signature related requests */
public final class SignatureLookup {

  private SignatureLookup() {}

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  /**
   * Search the classpath and return any signature for a given CallableFunction.
   *
   * @param callableFunction The callable function whose documentation is being search
   * @return Associated signature.
   */
  @Nullable
  public static String getSignatureFromCallableFunction(CallableFunction callableFunction) {
    return searchForFunctionSignature(callableFunction.getSignature().toString());
  }

  /**
   * Searches the classpath for signature given a function name and the number of arguments the
   * function takes. If nothing is found, it searches for a VAR_ARGS based function, if nothing is
   * still found, returns null.
   *
   * @param fxnSignature The language server signature for which original signature is being
   *     searched.
   */
  @Nullable
  private static String searchForFunctionSignature(String fxnSignature) {
    try {
      ClassLoader loader = SignatureLookup.class.getClassLoader();
      InputStream res =
          loader.getResourceAsStream("signature_" + sanitizeForFileName(fxnSignature));
      if (res != null) {
        return new String(res.readAllBytes(), UTF_8);
      }
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Error while reading signature for function during auto-completion operation.");
      return null;
    }
    return null;
  }

  private static String sanitizeForFileName(String input) {
    // Replace invalid characters with underscores
    return input.replaceAll("[^a-zA-Z0-9-\\.\\-]", "_");
  }
}
