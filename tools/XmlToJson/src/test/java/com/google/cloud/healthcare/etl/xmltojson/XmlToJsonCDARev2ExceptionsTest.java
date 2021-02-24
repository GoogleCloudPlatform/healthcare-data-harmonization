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

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/** Unit Tests for XmlToJson parser CDA Rev 2 Exceptions * */
public class XmlToJsonCDARev2ExceptionsTest {

  @Test(expected = XmlToJsonException.class)
  public void xmlToJSONCDARev2InvalidInputXML() throws XmlToJsonException {
    String inputXML = "<ClinicalDocument> No closing tag";

    XmlToJson parser = XmlToJsonCDARev2Utils.createCDARev2XmlToJsonParser();
    parser.parse(inputXML);
  }

  /**
   * This test verifies that the parser omits invalid fields from the output, in this case the field
   * realmCodes does not exist in the schema.
   */
  @Test
  public void xmlToJSONCDARev2InvalidCCDARev2() {
    String inputXml = readFile("src/test/resources/synthea/inputs/invalid_ccda_rev2.xml");
    XmlToJson parser = createCDARev2XmlToJsonParser();
    String actualJSON = parseXml(parser, inputXml);
    String expectedJSON =
        readFile("src/test/resources/synthea/outputs/invalid_ccda_rev2.json");
    JSONAssert.assertEquals(expectedJSON, actualJSON, /* strict */ false);
  }
}
