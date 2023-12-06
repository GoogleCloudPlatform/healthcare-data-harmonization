/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.match;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;

/** Test util for matching criteria */
public final class MatchingTestUtils {
  public static final String MATCHING_DIR = "/matching/";

  public static Container readJsonFile(String filename) throws IOException {
    return readJsonFile(MATCHING_DIR, filename);
  }

  public static Container readJsonFile(String dir, String filename) throws IOException {
    InputStream stream = MatchingTestUtils.class.getResourceAsStream(dir + filename);
    Data data = JsonSerializerDeserializer.jsonToData(ByteStreams.toByteArray(stream));
    return data.asContainer();
  }

  private MatchingTestUtils() {}
}
