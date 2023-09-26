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

import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;
import java.io.IOException;

/**
 * A Loader downloads an imported file from the given URL. The loader should be format independent.
 * Implementations of this interface are referred to by {@link Import#getLoader()}
 */
public interface Loader extends Registrable {
  /** Loads (or fetches) the data at the given url. */
  byte[] load(ImportPath path) throws IOException;
}
