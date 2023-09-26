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

package com.google.cloud.verticals.foundations.dataharmonization.integration.mocking;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A dummy plugin to test specifying mock function using non-Whistle function. */
public class MockingTestPlugin implements Plugin {
  public static final String PACKAGE_NAME = "test";

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    return ImmutableList.of(
        new CallableFunction() {
          @Override
          protected Data callInternal(RuntimeContext context, Data... args) {
            for (Data d : args) {
              if (d.isClass(Closure.class)) {
                d.asClass(Closure.class).execute(context);
              }
            }
            return new DefaultPrimitive("foo");
          }

          @Override
          public Signature getSignature() {
            return new Signature(PACKAGE_NAME, "mockFunc", ImmutableList.of(Data.class), true);
          }

          @Override
          public DebugInfo getDebugInfo() {
            return null;
          }
        });
  }
}
