/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.imports;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;
import java.io.IOException;

/**
 * A Parser parses the data loaded by a {@link Loader} and initializes the given {@link
 * RuntimeContext} with it. Implementations of this interface are referred to by {@link
 * Import#getParser()}
 */
public interface Parser extends Registrable {
  /**
   * Parse the given data and register it as appropriate into the given registries. If the given
   * data represents something that can import other things (like a Whistle file that can import
   * other Whistle files or plugins), then it should do so using the given ImportProcessor.
   */
  void parse(
      byte[] data,
      Registries registries,
      MetaData metaData,
      ImportProcessor processor,
      ImportPath iPath)
      throws IOException;

  /** Return true iff this parser can parse the file at the specified import path. */
  boolean canParse(ImportPath path);
}
