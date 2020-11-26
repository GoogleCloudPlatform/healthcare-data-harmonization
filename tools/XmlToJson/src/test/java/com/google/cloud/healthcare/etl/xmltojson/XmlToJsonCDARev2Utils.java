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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/** Common utilities required by XmlToJson CDA Rev2 tests * */
public class XmlToJsonCDARev2Utils {
  public static XmlToJson createCDARev2XmlToJsonParser() {
    XmlToJson parser = null;
    try {
      parser = new XmlToJsonCDARev2();
    } catch (XmlToJsonException e) {
      e.printStackTrace();
    }
    return parser;
  }

  public static String readFile(String filePath) {
    File inputFile = new File(filePath);
    String fileStr = null;
    try {
      fileStr = new Scanner(inputFile).useDelimiter("\\Z").next();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return fileStr;
  }

  public static String parseXml(XmlToJson parser, String inputXMLStr) {
    String output = null;
    try {
      output = parser.parse(inputXMLStr);
    } catch (XmlToJsonException e) {
      e.printStackTrace();
    }
    return output;
  }
}
