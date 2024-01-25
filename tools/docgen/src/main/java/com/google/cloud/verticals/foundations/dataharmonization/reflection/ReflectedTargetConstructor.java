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
public class ReflectedTargetConstructor extends ReflectedInstance {
  public static final String TARGET_CONSTRUCTOR_BASECLASS_NAME =
      "com.google.cloud.verticals.foundations.dataharmonization.target.Target$Constructor";
  public static final String TARGET_BASECLASS_NAME =
      "com.google.cloud.verticals.foundations.dataharmonization.target.Target";
  public static final Class<?> TARGET_CONSTRUCTOR_BASECLASS;
  public static final Class<?> TARGET_BASECLASS;

  static {
    try {
      TARGET_CONSTRUCTOR_BASECLASS = Class.forName(TARGET_CONSTRUCTOR_BASECLASS_NAME);
      TARGET_BASECLASS = Class.forName(TARGET_BASECLASS_NAME);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private final String subclassName;

  public ReflectedTargetConstructor(Object instance) {
    super(instance);
    this.subclassName =
        instance != null ? instance.getClass().getName() : TARGET_CONSTRUCTOR_BASECLASS_NAME;
  }

  @Override
  protected String getReflectedClassName() {
    return subclassName;
  }

  public String getName() {
    return invoke(String.class, "getName");
  }
}
