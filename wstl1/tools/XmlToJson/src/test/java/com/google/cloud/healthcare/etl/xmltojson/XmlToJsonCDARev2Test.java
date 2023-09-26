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

package com.google.cloud.healthcare.etl.xmltojson;

import static com.google.cloud.healthcare.etl.xmltojson.XmlToJsonCDARev2Utils.createCDARev2XmlToJsonParser;
import static com.google.cloud.healthcare.etl.xmltojson.XmlToJsonCDARev2Utils.parseXml;
import static com.google.cloud.healthcare.etl.xmltojson.XmlToJsonCDARev2Utils.readFile;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.skyscreamer.jsonassert.JSONAssert;

/** Unit Tests for XML To JSON parser * */
@RunWith(Parameterized.class)
public class XmlToJsonCDARev2Test {

  @Parameter(0)
  public String fileName;

  @Parameters(name = "key={0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"valid_ccda_rev2_sample1"},
          {"valid_ccda_rev2_sample2"},
          {"valid_ccda_rev2_sample3"},
          {"valid_ccda_rev2_sample4"},
          {"valid_ccda_rev2_sample5"},
          {"valid_ccda_rev2_sample6"},
          {"valid_ccda_rev2_sample7"},
          {"valid_ccda_rev2_sample8"},
          {"valid_ccda_rev2_sample9"},
          {"valid_ccda_rev2_sample10"},
          {"valid_ccda_rev2_sample11"},
          {"valid_ccda_rev2_sample12"},
          {"valid_ccda_rev2_sample13"},
        });
  }

  /**
   * This test verifies, by using valid Synthea samples, that the parser correctly converts a valid
   * CCDA Rev 2 XML to the expected JSON.
   */
  @Test
  public void xmlToJSONCDARev2ValidCCDARev2() {
    String inputXml = readFile("src/test/resources/synthea/inputs/" + fileName + ".xml");
    XmlToJson parser = createCDARev2XmlToJsonParser();
    String actualJSON = parseXml(parser, inputXml);
    String expectedJSON = readFile("src/test/resources/synthea/outputs/" + fileName + ".json");
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ true);
  }

  @Test
  public void xmlToJSONCDARev2ValidCCDARev2WithAdditionalFields() throws XmlToJsonException {
    String fieldKey = "__data_source__";
    String fieldValue = "source";
    Map<String, String> fields = ImmutableMap.of(fieldKey, fieldValue);
    String inputXml = readFile("src/test/resources/synthea/inputs/" + fileName + ".xml");
    XmlToJson parser = new XmlToJsonCDARev2(fields);
    String actualJSON = parseXml(parser, inputXml);
    JsonObject expectedJSON =
        JsonParser.parseString(readFile("src/test/resources/synthea/outputs/" + fileName + ".json"))
            .getAsJsonObject();
    expectedJSON.addProperty(fieldKey, fieldValue);
    JSONAssert.assertEquals(new Gson().toJson(expectedJSON), actualJSON, /* strict */ true);
  }
}
