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

import java.util.List;
import java.util.Objects;

/** Reflection wrapper for the Whistle Plugin class. */
public class ReflectedPlugin extends ReflectedInstance {
  public static final String PLUGIN_BASECLASS_NAME =
      "com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin";
  private static final Class<?> PLUGIN_BASECLASS;

  static {
    try {
      PLUGIN_BASECLASS = Class.forName(PLUGIN_BASECLASS_NAME);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private final String subclassName;

  public ReflectedPlugin(Object instance) {
    super(instance);
    this.subclassName = instance != null ? instance.getClass().getName() : PLUGIN_BASECLASS_NAME;
  }

  @Override
  protected String getReflectedClassName() {
    return subclassName;
  }

  /** Returns the package name of the plugin. */
  public String getPackageName() {
    return invoke(String.class, "getPackageName");
  }

  /** Returns true if the given plugin class is a subclass of the Whistle Plugin class. */
  public static boolean isPlugin(String className) {
    try {
      return !className.equals(PLUGIN_BASECLASS_NAME)
          && PLUGIN_BASECLASS.isAssignableFrom(Class.forName(className));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<ReflectedCallableFunction> getFunctions() {
    return invokeList(ReflectedCallableFunction.class, "getFunctions");
  }

  public List<ReflectedTargetConstructor> getTargets() {
    return invokeList(ReflectedTargetConstructor.class, "getTargets");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReflectedPlugin)) {
      return false;
    }
    ReflectedPlugin that = (ReflectedPlugin) o;
    return Objects.equals(subclassName, that.subclassName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subclassName);
  }
}
