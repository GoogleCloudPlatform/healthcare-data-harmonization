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

package com.google.cloud.healthcare.etl.xmltojson.postprocessor;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Unit Tests for Post Processor for CDA Rev 2 with Data Source. */
public class PostProcessorCdsRev2DataSourceTest {
  private PostProcessor ppCDARev2;

  private static final Map<String, String> additionalFields =
      ImmutableMap.of("__data_source__", "source");

  @Before
  public void setUp() throws Exception {
    ppCDARev2 = new PostProcessorCdaRev2();
  }

  /** This test verifies that the input datasource is contained in the output. */
  @Test
  public void postProcessorCDARev2WithDatasource() throws PostProcessorException {
    String inputJSON =
        "{"
            + "   \"ClinicalDocument\":{"
            + "      \"author\":["
            + "         {"
            + "            \"assignedAuthoringDevice\":{"
            + "               \"manufacturerModelName\":\"synthea\""
            + "            }"
            + "         }"
            + "      ]"
            + "   }"
            + "}";

    String expectedJSON =
        "{"
            + "   \"ClinicalDocument\":{"
            + "      \"author\":["
            + "         {"
            + "            \"assignedAuthoringDevice\":{"
            + "               \"manufacturerModelName\":\"synthea\""
            + "            }"
            + "         }"
            + "      ]"
            + "   },"
            + "   \"__data_source__\":\"source\""
            + "}";

    String actualJSON = ppCDARev2.postProcessWithAdditionalFields(inputJSON, additionalFields);
    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /** This test verifies that no additional fields are added when the map is empty. */
  @Test
  public void postProcessorCDARev2WithDatasource_emptyFieldMap() throws PostProcessorException {
    String inputJSON =
        "{"
            + "   \"ClinicalDocument\":{"
            + "      \"author\":["
            + "         {"
            + "            \"assignedAuthoringDevice\":{"
            + "               \"manufacturerModelName\":\"synthea\""
            + "            }"
            + "         }"
            + "      ]"
            + "   }"
            + "}";

    String expectedJSON =
        "{"
            + "   \"ClinicalDocument\":{"
            + "      \"author\":["
            + "         {"
            + "            \"assignedAuthoringDevice\":{"
            + "               \"manufacturerModelName\":\"synthea\""
            + "            }"
            + "         }"
            + "      ]"
            + "   }"
            + "}";

    String actualJSON = ppCDARev2.postProcessWithAdditionalFields(inputJSON, new HashMap<>());
    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }

  /**
   * This test verifies that the datasource is not in the output when the resulting resource is
   * empty.
   */
  @Test
  public void postProcessorCDARev2WithDatasource_emptyResource() throws PostProcessorException {
    String inputJSON = "{\"manufacturedMaterial\":{\"code\":{}}}";

    String expectedJSON = "{}";

    String actualJSON = ppCDARev2.postProcessWithAdditionalFields(inputJSON, additionalFields);
    Assert.assertEquals(JsonParser.parseString(expectedJSON), JsonParser.parseString(actualJSON));
  }
}
