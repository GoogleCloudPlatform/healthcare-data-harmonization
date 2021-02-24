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

import org.junit.Before;
import org.junit.Test;

/** Unit Tests for Post Processor for CDA Rev 2 Exceptions. */
public class PostProcessorCdaRev2ExceptionsTest {
  private PostProcessor ppCDARev2;

  @Before
  public void setUp() throws Exception {
    ppCDARev2 = new PostProcessorCdaRev2();
  }

  @Test(expected = PostProcessorException.class)
  public void invalidJSON() throws PostProcessorException {
    String inputJSON =
        "{\n"
            + "   \"manufacturedMaterial\":{\n"
            + "      \"code\":{\n"
            + "         \"originalText\":{\n"
            + "            \"reference\":[\n"
            + "               [\"primitiveString\"" // No closing square bracket.
            + "            ]\n"
            + "         }\n"
            + "      }\n"
            + "   }\n"
            + "}";

    ppCDARev2.postProcess(inputJSON);
  }
}
