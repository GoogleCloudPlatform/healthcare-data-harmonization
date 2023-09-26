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

package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame.DefaultBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.InitializationContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import java.io.Serializable;

/**
 * A RuntimeContextImplementation contains factory methods for a specific implementation of {@link
 * RuntimeContext}.
 */
public interface RuntimeContextImplementation extends Serializable {

  /** Constructs the main {@link RuntimeContext} used during the mapping execution. */
  RuntimeContext constructMainContext(
      PackageContext packageContext,
      StackFrame stackTop,
      StackFrame stackBottom,
      Registries registries,
      ImportProcessor importProcessor,
      MetaData metaData);

  /**
   * Constructs the initial RuntimeContext. This defaults to an instance of {@link
   * InitializationContext} since it doesn't associate with a stack frame but instead generates the
   * first stack frame.
   */
  default RuntimeContext constructInitialContext(
      PackageContext context,
      Registries registries,
      ImportProcessor importProcessor,
      MetaData metaData) {
    return new InitializationContext(
        context, registries, importProcessor, this, new DefaultBuilder(), metaData);
  }
}
