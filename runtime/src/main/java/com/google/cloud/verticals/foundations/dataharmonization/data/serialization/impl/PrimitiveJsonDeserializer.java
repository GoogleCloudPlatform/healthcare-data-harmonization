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

import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Type;

/**
 * PrimitiveJsonDeserializer provides a {@link com.google.gson.JsonDeserializer} TypeAdapter,
 * allowing Gson to convert a byte array to a {@link Primitive}.
 */
public class PrimitiveJsonDeserializer implements JsonDeserializer<Primitive> {

  @Override
  public Primitive deserialize(
      JsonElement element, Type typeOfT, JsonDeserializationContext context) {
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    // TODO(): Use DTI from RuntimeContext here.
    if (primitive.isBoolean()) {
      return DefaultDataTypeImplementation.instance.primitiveOf(primitive.getAsBoolean());
    } else if (primitive.isNumber()) {
      return DefaultDataTypeImplementation.instance.primitiveOf(primitive.getAsDouble());
    } else if (primitive.isString()) {
      return DefaultDataTypeImplementation.instance.primitiveOf(primitive.getAsString());
    }
    return NullData.instance;
  }
}
