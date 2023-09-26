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
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;

/**
 * ContainerJsonSerializer provides a {@link com.google.gson.JsonSerializer} TypeAdapter allowing
 * Gson to convert a {@link Container} to a JsonObject and ultimately to a byte array.
 */
public class DataJsonDeserializer implements JsonDeserializer<Data> {

  @Override
  public Data deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {
    if (element == null || element.isJsonNull()) {
      // This will never happen as GSON does not defer nulls to deserializers...for now.
      return NullData.instance;
    } else if (element.isJsonArray()) {
      // process Array
      return JsonSerializerDeserializer.gson.fromJson(element, Array.class);
    } else if (element.isJsonObject()) {
      // process Container
      return JsonSerializerDeserializer.gson.fromJson(element, Container.class);
    } else if (element.isJsonPrimitive()) {
      // process primitive
      return JsonSerializerDeserializer.gson.fromJson(element, Primitive.class);
    }
    throw new IllegalArgumentException(String.format("Cannot handle json element %s", element));
  }
}
