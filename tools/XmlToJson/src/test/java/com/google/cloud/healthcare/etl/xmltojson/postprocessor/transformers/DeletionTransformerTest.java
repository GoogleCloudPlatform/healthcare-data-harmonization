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
import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/** Unit Tests for Deletion Transformer * */
public class DeletionTransformerTest {

  /**
   * This test verifies, that no keys are deleted if an empty set of keys is sent to the
   * DeletionTransformer constructor.
   */
  @Test
  public void deletionTransformerEmptySet() throws PostProcessorException {
    Set<String> keysToDelete = new HashSet<>();
    DeletionTransformer deletionTransformer = new DeletionTransformer(keysToDelete);

    String inputJSON =
        "{"
            + "   \"manufacturedMaterial\":{"
            + "      \"code\":{"
            + "         \"originalText\":{"
            + "            \"text\":["
            + "               {"
            + "                  \"value\":\"#medications-desc-1\""
            + "               }"
            + "            ]"
            + "         }"
            + "      }"
            + "   }"
            + "}";

    JSONObject jsonEl = new JSONObject(inputJSON);
    deletionTransformer.transform(jsonEl, "manufacturedMaterial");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(inputJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies, that the transformer correctly deletes the specified key if in the set of
   * keys supplied to the constructor.
   */
  @Test
  public void deletionTransformerCorrectDeletion() throws PostProcessorException {
    Set<String> keysToDelete = new HashSet<>();
    keysToDelete.add("manufacturedMaterial");
    DeletionTransformer deletionTransformer = new DeletionTransformer(keysToDelete);

    String inputJSON =
        "{"
            + "   \"manufacturedMaterial\":{"
            + "      \"code\":{"
            + "         \"originalText\":{"
            + "            \"text\":["
            + "               {"
            + "                  \"value\":\"#medications-desc-1\""
            + "               }"
            + "            ]"
            + "         }"
            + "      }"
            + "   },"
            + "   \"city\":[\"TX\"]"
            + "}";

    String expectedJSON = "{ \"city\":[\"TX\"]}";

    JSONObject jsonEl = new JSONObject(inputJSON);
    deletionTransformer.transform(jsonEl, "manufacturedMaterial");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies, that the transformer does not delete any key if the key intended to be
   * deleted is not present in the JSON.
   */
  @Test
  public void deletionTransformerNoDeletion() throws PostProcessorException {
    Set<String> keysToDelete = new HashSet<>();
    keysToDelete.add("manufacturedMaterial");
    DeletionTransformer deletionTransformer = new DeletionTransformer(keysToDelete);

    String inputJSON =
        "{"
            + "   \"manufacturedMaterial\":{"
            + "      \"code\":{"
            + "         \"originalText\":{"
            + "            \"text\":["
            + "               {"
            + "                  \"value\":\"#medications-desc-1\""
            + "               }"
            + "            ]"
            + "         }"
            + "      }"
            + "   }"
            + "}";

    String expectedJSON = inputJSON;

    JSONObject jsonEl = new JSONObject(inputJSON);
    deletionTransformer.transform(jsonEl, "city");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies, that the transformer does not delete any key if the key intended to be
   * deleted is not present in the JSON as a top level element.
   */
  @Test
  public void deletionTransformerNoDeletionInSecondLevel() throws PostProcessorException {
    Set<String> keysToDelete = new HashSet<>();
    keysToDelete.add("manufacturedMaterial");
    DeletionTransformer deletionTransformer = new DeletionTransformer(keysToDelete);

    String inputJSON =
        "{"
            + "   \"manufacturedMaterial\":{"
            + "      \"code\":{"
            + "         \"originalText\":{"
            + "            \"text\":["
            + "               {"
            + "                  \"value\":\"#medications-desc-1\""
            + "               }"
            + "            ]"
            + "         }"
            + "      }"
            + "   }"
            + "}";

    String expectedJSON = inputJSON;

    JSONObject jsonEl = new JSONObject(inputJSON);
    deletionTransformer.transform(jsonEl, "code");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }
}