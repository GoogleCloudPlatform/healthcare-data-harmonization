// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.reflection;

import java.lang.reflect.Method;

/** Reflection wrapper for the Whistle Plugin class. */
public class ReflectedJavaFunction extends ReflectedCallableFunction {
  public ReflectedJavaFunction(Object instance) {
    super(instance);
  }

  @Override
  protected String getReflectedClassName() {
    return ReflectedCallableFunction.JAVAFUNCTION_CLASS_NAME;
  }

  public Method getMethod() {
    return invoke(Method.class, "getMethod");
  }
}
