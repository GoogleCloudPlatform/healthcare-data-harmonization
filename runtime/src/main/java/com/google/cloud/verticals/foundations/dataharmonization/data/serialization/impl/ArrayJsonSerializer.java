/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * ArrayJsonSerializer provides a {@link com.google.gson.JsonSerializer} TypeAdapter allowing Gson
 * to convert a {@link Array} to a JsonObject and ultimately to a byte array.
 */
public class ArrayJsonSerializer implements JsonSerializer<Array> {

  public static JsonArray processArray(Array src) {
    JsonArray array = new JsonArray();
    for (int x = 0; x < src.size(); x++) {
      array.add(JsonSerializerDeserializer.gson.toJsonTree(src.asArray().getElement(x), Data.class));
    }
    return array;
  }

  @Override
  public JsonElement serialize(Array src, Type typeOfSrc, JsonSerializationContext context) {
    return processArray(src);
  }
}
