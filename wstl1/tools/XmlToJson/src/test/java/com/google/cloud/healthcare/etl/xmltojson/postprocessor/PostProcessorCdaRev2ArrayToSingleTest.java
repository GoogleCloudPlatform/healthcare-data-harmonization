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

import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.skyscreamer.jsonassert.JSONAssert;

/** Unit Tests for Post Processor for CDA Rev 2 Array to Single Conversion. */
@RunWith(Parameterized.class)
public class PostProcessorCdaRev2ArrayToSingleTest {
  private PostProcessor ppCDARev2;

  @Parameter(0)
  public String key;

  @Parameters(name = "key={0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"reference"}, {"thumbnail"}, {"high"}, {"low"}, {"width"}, {"center"},
        });
  }

  @Before
  public void setUp() throws Exception {
    ppCDARev2 = new PostProcessorCdaRev2();
  }

  /** This test verifies, that the arrays with specified keys are correctly converted to single. */
  @Test
  public void postProcessorCDARev2ArrayOfObjectsToSingle() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\":["
                + "               {"
                + "                  \"value\":\"#medications-desc-1\""
                + "               }"
                + "            ]"
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String expectedJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\":{"
                + "               \"value\":\"#medications-desc-1\""
                + "            }"
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /** This test verifies, that the arrays with specified keys are correctly converted to single. */
  @Test
  public void postProcessorCDARev2ArrayOfArraysToSingle() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\":["
                + "               [\"primitiveString\"]"
                + "            ]"
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String expectedJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\": [\"primitiveString\"]"
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /** This test verifies, that the arrays with specified keys are correctly converted to single. */
  @Test
  public void postProcessorCDARev2ArrayOfPrimitivesToSingle() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\":["
                + "               \"primitiveString\""
                + "            ]"
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String expectedJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\": \"primitiveString\""
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /**
   * This test verifies, that in case an array to be converted to single contains more than one
   * element, then the conversion is not forced.
   */
  @Test
  public void postProcessorCDARev2ArrayToSingleNoConversion() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"manufacturedMaterial\":{"
                + "      \"code\":{"
                + "         \"originalText\":{"
                + "            \"%s\":["
                + "               {"
                + "                  \"value\":\"#medications-desc-1\""
                + "               },"
                + "               {"
                + "                  \"value\":\"#medications-desc-2\""
                + "               }"
                + "            ]"
                + "         }"
                + "      }"
                + "   }"
                + "}",
            key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(inputJSON, actualJSON, /* strict */ false);
  }

  /** This test verifies, that if JSON does not contain array keys there is no transformation. */
  @Test
  public void noArrayKeys() throws PostProcessorException {
    String inputJSON =
        String.format("{"
            + "   \"manufacturedMaterial\":{"
            + "      \"code\":{"
            + "         \"originalText\":{"
            + "            \"%s\":\"primitiveString\""
            + "         }"
            + "      }"
            + "   }"
            + "}", key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(inputJSON, actualJSON, /* strict */ false);
  }
}
