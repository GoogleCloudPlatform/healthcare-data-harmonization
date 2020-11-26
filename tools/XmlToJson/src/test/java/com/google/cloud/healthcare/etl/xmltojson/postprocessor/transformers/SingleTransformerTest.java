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

/** Unit Tests for Single Transformer * */
public class SingleTransformerTest {

  /**
   * This test verifies, that no arrays are transformed, if an empty set of keys was sent to the
   * SingleTransformer constructor.
   */
  @Test
  public void singleTransformerEmptySet() throws PostProcessorException {
    Set<String> arraysToTransform = new HashSet<>();
    SingleTransformer singleTransformer = new SingleTransformer(arraysToTransform);

    String inputJSON =
        "{"
            + "   \"addr\":["
            + "      {"
            + "         \"city\":["
            + "            \"PLUGERVILLE\""
            + "         ]"
            + "      }"
            + "   ],"
            + "   \"id\":["
            + "      {"
            + "         \"nullFlavor\":\"NA\""
            + "      }"
            + "   ]"
            + "}";

    String expectedJSON = inputJSON;

    JSONObject jsonEl = new JSONObject(inputJSON);
    singleTransformer.transform(jsonEl, "addr");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies, that the transformer correctly transforms the specified array to a single
   * object, if the array is in the set of keys supplied to the constructor.
   */
  @Test
  public void singleTransformerCorrectTransformation() throws PostProcessorException {
    Set<String> arraysToTransform = new HashSet<>();
    arraysToTransform.add("addr");
    SingleTransformer singleTransformer = new SingleTransformer(arraysToTransform);

    String inputJSON =
        "{"
            + "   \"addr\":["
            + "      {"
            + "         \"city\":["
            + "            \"PLUGERVILLE\""
            + "         ]"
            + "      }"
            + "   ],"
            + "   \"id\":["
            + "      {"
            + "         \"nullFlavor\":\"NA\""
            + "      }"
            + "   ]"
            + "}";

    String expectedJSON =
        "{"
            + "   \"addr\":{"
            + "         \"city\":["
            + "            \"PLUGERVILLE\""
            + "         ]"
            + "   },"
            + "   \"id\":["
            + "      {"
            + "         \"nullFlavor\":\"NA\""
            + "      }"
            + "   ]"
            + "}";

    JSONObject jsonEl = new JSONObject(inputJSON);
    singleTransformer.transform(jsonEl, "addr");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies, that the transformer does not change the object if already a single object.
   */
  @Test
  public void singleTransformerNoTransformationNeeded() throws PostProcessorException {
    Set<String> arraysToTransform = new HashSet<>();
    arraysToTransform.add("addr");
    SingleTransformer singleTransformer = new SingleTransformer(arraysToTransform);

    String inputJSON =
        "{"
            + "   \"addr\":{"
            + "         \"city\":["
            + "            \"PLUGERVILLE\""
            + "         ]"
            + "   }"
            + "}";

    String expectedJSON = inputJSON;

    JSONObject jsonEl = new JSONObject(inputJSON);
    singleTransformer.transform(jsonEl, "addr");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies, that the transformer does not change transform the array if it contains
   * more than one element.
   */
  @Test
  public void singleTransformerNoTransformationPossible() throws PostProcessorException {
    Set<String> arraysToTransform = new HashSet<>();
    arraysToTransform.add("addr");
    SingleTransformer singleTransformer = new SingleTransformer(arraysToTransform);

    String inputJSON =
        "{"
            + "   \"addr\":["
            + "      {"
            + "         \"city\":["
            + "            \"PLUGERVILLE\""
            + "         ]"
            + "      },"
            + "      {"
            + "         \"city\":["
            + "            \"WATERLOO\""
            + "         ]"
            + "      }"
            + "   ],"
            + "   \"id\":["
            + "      {"
            + "         \"nullFlavor\":\"NA\""
            + "      }"
            + "   ]"
            + "}";

    String expectedJSON = inputJSON;

    JSONObject jsonEl = new JSONObject(inputJSON);
    singleTransformer.transform(jsonEl, "addr");
    String actualJSON = jsonEl.toString();

    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }
}
