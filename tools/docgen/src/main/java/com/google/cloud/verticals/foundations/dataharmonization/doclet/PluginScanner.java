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
package com.google.cloud.verticals.foundations.dataharmonization.doclet;

import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedInstance;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedPlugin;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner8;

/** ElementScanner that finds Plugin classes. */
public class PluginScanner extends ElementScanner8<Void, Set<ReflectedPlugin>> {
  @Override
  public Void visitType(TypeElement e, Set<ReflectedPlugin> plugins) {
    String className = e.getQualifiedName().toString();
    if (canLoad(className) && ReflectedPlugin.isPlugin(className)) {
      plugins.add(
          new ReflectedPlugin(ReflectedInstance.instantiate(e.getQualifiedName().toString())));
    }

    return super.visitType(e, plugins);
  }

  private static boolean canLoad(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }
}
