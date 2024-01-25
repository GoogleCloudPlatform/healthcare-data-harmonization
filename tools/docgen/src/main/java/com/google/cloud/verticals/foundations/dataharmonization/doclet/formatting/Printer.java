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
package com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.PackageDoc;
import java.util.Collection;

/**
 * A Printer outputs package documents in a specific consumable format.
 *
 * @param <TResult> The result of this printer, for example, a proto or a collection of text files.
 */
public interface Printer<TResult> {
  TResult format(Collection<PackageDoc> docs);
}
