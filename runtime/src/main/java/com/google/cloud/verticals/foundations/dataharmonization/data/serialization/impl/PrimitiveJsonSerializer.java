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

import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * PrimitiveJsonSerializer provides a {@link com.google.gson.JsonSerializer} TypeAdapter allowing
 * Gson to convert a {@link Primitive} to a JsonPrimitive and ultimately to a byte array.
 */
public class PrimitiveJsonSerializer implements JsonSerializer<Primitive> {

  static JsonPrimitive processPrimitive(Primitive src) {
    if (src.bool() != null) {
      return new JsonPrimitive(src.bool());
    } else if (src.num() != null && src.isFractionNegligible()) {
      return new JsonPrimitive(src.rounded());
    } else if (src.num() != null) {
      return new JsonPrimitive(src.num());
    } else if (src.string() != null) {
      return new JsonPrimitive(src.string());
    }

    return null;
  }

  @Override
  public JsonElement serialize(Primitive src, Type typeOfSrc, JsonSerializationContext context) {
    return processPrimitive(src);
  }
}
