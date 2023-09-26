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

package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.buildSource;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.functionMeta;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExpressionContext;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/** Helper functions for generating lambdas. */
public final class LambdaHelper {
  private static Random random = null;

  static String getUUIDString() {
    UUID uuid;
    if (random == null) {
      uuid = UUID.randomUUID();
    } else {
      uuid = new UUID(random.nextInt(), random.nextInt());
    }
    return uuid.toString();
  }

  /** Sets seed for uuid generator, only used for testing purpose */
  public static void setSeed(int seed) {
    random = new Random(seed);
  }
  /**
   * Wraps the given expression in a function call as a body of the given signature. That is, given
   * a signature like (fb, fc) where fb and fc are free params, and an expression like (x + fb + fc)
   * where x is a variable from some outer scope, this method generates a function definition like:
   *
   * <p>
   *
   * <pre>
   * def lambda_...(fb, fc, x) x + fb + fc</pre>
   *
   * <p>and a call like:
   *
   * <p>
   *
   * <pre>lambda_...(fb, fc, x)</pre>
   *
   * where fb and fc are ValueSources with freeParameter set.
   *
   * @param transpiler the Transpiler instance to use.
   * @param signature the Signature that the lambda should build on. Should only contain free
   *     params.
   * @param body The expression body of the lambda.
   * @param prefix The prefix that will be assigned to the lambda's name.
   * @param type The type of lambda; This is stored in the meta and determines whether it can write
   *     back to variables in the parent scope.
   * @return A closure (i.e. a function call proto with build_closure set to true that calls this
   *     lambda with all the free and bound parameters in place).
   */
  public static FunctionCall lambda(
      Transpiler transpiler,
      Signature signature,
      ExpressionContext body,
      String prefix,
      FunctionType type) {

    // First, the arguments are just the free parameters.
    List<FunctionDefinition.Argument> args =
        signature.getFreeParamNames().stream()
            .map(name -> FunctionDefinition.Argument.newBuilder().setName(name).build())
            .collect(Collectors.toList());

    transpiler.pushEnv(prefix + getUUIDString(), args);

    // When we transpile the body we may pull in more arguments from the parent scope.
    ValueSource bodyValue = (ValueSource) body.accept(transpiler);
    Environment lambdaEnv = transpiler.popEnv();

    FunctionDefinition lambdaDef =
        lambdaEnv
            .generateDefinition(
                type == FunctionType.BLOCK || type == FunctionType.IMPLICIT,
                Collections.singletonList(FieldMapping.newBuilder().setValue(bodyValue).build()))
            .setMeta(functionMeta(body, type))
            .build();

    transpiler.addFunction(lambdaDef);
    FunctionCall lambdaCall =
        lambdaEnv.generateInvocation(
            true,
            buildSource(body),
            signature.getFreeParamNames().stream()
                .map(ValueSource.newBuilder()::setFreeParameter)
                .map(ValueSource.Builder::build)
                .toArray(ValueSource[]::new));
    return lambdaCall;
  }
}
