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

/** Unit Tests for Post Processor for CDA Rev 2 Key Deletion. */
@RunWith(Parameterized.class)
public class PostProcessorCdaRev2KeyDeletionTest {
  private PostProcessor ppCDARev2;

  @Parameter(0)
  public String key;

  @Parameters(name = "key={0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"nullFlavor"},
        });
  }

  @Before
  public void setUp() throws Exception {
    ppCDARev2 = new PostProcessorCdaRev2();
  }

  /** This test verifies, that keys are correctly deleted when contained in an array. */
  @Test
  public void postProcessorCDARev2DeleteKeyFromArray() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":["
                + "                  {"
                + "                     \"%s\":\"NA\"" // This is the key to be deleted
                + "                  },"
                + "                  {"
                + "                     \"city\":["
                + "                        \"PLUGERVILLE\""
                + "                     ],"
                + "                  }"
                + "               ]"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

    String expectedJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":["
                + "                  {"
                + "                     \"city\":["
                + "                        \"PLUGERVILLE\""
                + "                     ]"
                + "                  }"
                + "               ]"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /**
   * This test verifies, that keys are correctly deleted and also the container array if as a result
   * of key deletion it ends up empty.
   */
  @Test
  public void postProcessorCDARev2DeleteKeyAndAContainerArray() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":["
                + "                  {"
                + "                     \"%s\":\"NA\"" // This is the key to be deleted
                + "                  }"
                + "               ]"
                + "            },"
                + "            \"assignedAuthoringDevice\":{"
                + "               \"manufacturerModelName\":\"synthea\""
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

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

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /**
   * This test verifies, that keys are correctly deleted and also the containers leaving a final
   * empty JSON.
   */
  @Test
  public void postProcessorCDARev2DeleteAllFromArray() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":["
                + "                  {"
                + "                     \"%s\":\"NA\"" // This is the key to be deleted
                + "                  }"
                + "               ]"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

    String expectedJSON = "{}";

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /**
   * This test verifies, that keys are correctly deleted and also the containers, leaving a final
   * empty JSON, when data is contained in an array of arrays.
   */
  @Test
  public void postProcessorCDARev2DeleteAllFromArrayOfArrays() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":["
                + "                  [{"
                + "                     \"%s\":\"NA\"" // This is a key to be deleted
                + "                  },"
                + "                  {"
                + "                     \"%s\":\"NA\"" // This is a key to be deleted
                + "                  }]"
                + "               ]"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key, key);

    String expectedJSON = "{}";

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /** This test verifies, that keys are correctly deleted when contained in an object. */
  @Test
  public void postProcessorCDARev2DeleteKeyFromObject() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":"
                + "                  {"
                + "                     \"%s\":\"NA\"," // This is the key to be deleted
                + "                     \"state\": [\"TX\"]"
                + "                  }"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

    String expectedJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":"
                + "                  {"
                + "                     \"state\": [\"TX\"]"
                + "                  }"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /**
   * This test verifies, that keys are correctly deleted and also the container object if as a
   * result of key deletion it ends up empty.
   */
  @Test
  public void postProcessorCDARev2DeleteKeyAndAContainerObject() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":"
                + "                  {"
                + "                     \"%s\":\"NA\"" // This is the key to be deleted
                + "                  }"
                + "            },"
                + "            \"assignedAuthoringDevice\":{"
                + "               \"manufacturerModelName\":\"synthea\""
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

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

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }

  /**
   * This test verifies, that keys are correctly deleted and also the containers leaving a final
   * empty JSON.
   */
  @Test
  public void postProcessorCDARev2DeleteAllFromObject() throws PostProcessorException {
    String inputJSON =
        String.format(
            "{"
                + "   \"ClinicalDocument\":{"
                + "      \"author\":["
                + "         {"
                + "            \"assignedAuthor\":{"
                + "               \"addr\":"
                + "                  {"
                + "                     \"%s\":\"NA\"" // This is the key to be deleted
                + "                  }"
                + "            }"
                + "         }"
                + "      ]"
                + "   }"
                + "}",
            key);

    String expectedJSON = "{}";

    String actualJSON = ppCDARev2.postProcess(inputJSON);
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }
}
