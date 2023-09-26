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

/** Converts XML documents compliant with supported schemas to JSON * */
public interface XmlToJson {
  /**
   * Method in charge of converting an XML CCDA into JSON.
   *
   * @param input xml string to be parsed
   * @return conversion of input xml as json string
   * @throws XmlToJsonException
   */
  public String parse(String input) throws XmlToJsonException;
}
