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
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/** Class to delete a key. */
public class DeletionTransformer implements Transformer {
  private Set<String> keysToTransform;

  /**
   * Method in charge of delete a key from a JSON object
   *
   * @param keysToTransform list of keys to be deleted
   */
  public DeletionTransformer(Set<String> keysToTransform) {
    this.keysToTransform = keysToTransform;
  }

  /**
   * Execute the transformation on an specific JSON object and key. Only if the key is in the list
   * provided to the constructor, it will be transformed
   *
   * @param jsonEl the JSON object to be transformed
   * @param key the specific key under consideration
   * @throws PostProcessorException
   */
  @Override
  public void transform(JSONObject jsonEl, String key) throws PostProcessorException {
    if (keysToTransform.contains(key) && jsonEl.has(key)) {
      forceDeletion(jsonEl, key);
    }
  }

  private void forceDeletion(JSONObject jsonEl, String key) throws PostProcessorException {
    try {
      jsonEl.remove(key);
    } catch (JSONException e) {
      throw new PostProcessorException("error while deleting key " + key, e);
    }
  }
}
