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

/** Reflection wrapper for the Whistle Plugin class. */
public class ReflectedCallableFunction extends ReflectedInstance {
  public static final String BASECLASS_NAME =
      "com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction";
  public static final String JAVAFUNCTION_CLASS_NAME =
      "com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction";
  private static final Class<?> JAVAFUNCTION_CLASS;

  static {
    try {
      JAVAFUNCTION_CLASS = Class.forName(JAVAFUNCTION_CLASS_NAME);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private final String subclassName;

  public ReflectedCallableFunction(Object instance) {
    super(instance);
    this.subclassName = instance != null ? instance.getClass().getName() : BASECLASS_NAME;
  }

  @Override
  protected String getReflectedClassName() {
    return subclassName;
  }

  public ReflectedJavaFunction asJavaFunction() {
    return new ReflectedJavaFunction(reflectedInstance);
  }

  /** Returns true if the given plugin class is a subclass of the Whistle Plugin class. */
  public boolean isJavaFunction() {
    try {
      return JAVAFUNCTION_CLASS.isAssignableFrom(Class.forName(subclassName));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
