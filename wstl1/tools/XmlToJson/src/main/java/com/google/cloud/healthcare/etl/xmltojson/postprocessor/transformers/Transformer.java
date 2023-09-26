// Copyright 2020 Google LLC.
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

package com.google.cloud.healthcare.etl.xmltojson.postprocessor.transformers;

import com.google.cloud.healthcare.etl.xmltojson.postprocessor.PostProcessorException;
import org.json.JSONObject;

/** Interface for a JSON transformation */
public interface Transformer {
  /**
   * Method in charge of transforming an specific key of a JSON object
   *
   * @param jsonEl the JSON object to be transformed
   * @param key the specific key to be transformed
   * @throws PostProcessorException
   */
  public void transform(JSONObject jsonEl, String key) throws PostProcessorException;
}
