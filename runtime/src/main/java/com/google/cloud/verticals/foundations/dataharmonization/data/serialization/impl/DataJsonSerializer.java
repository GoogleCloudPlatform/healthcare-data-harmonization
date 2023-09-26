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
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.WithCustomSerialization;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * DataJsonSerializer provides a {@link com.google.gson.JsonSerializer} TypeAdapter allowing Gson to
 * convert a {@link Data} to a JsonPrimitive and ultimately to a byte array.
 */
public class DataJsonSerializer implements JsonSerializer<Data> {

  @Override
  public JsonElement serialize(Data src, Type typeOfSrc, JsonSerializationContext context) {
    if (src instanceof WithCustomSerialization) {
      Data dataToSerialize = ((WithCustomSerialization) src).getDataToSerialize();
      if (dataToSerialize.isClass(NullData.class)) {
        return JsonSerializerDeserializer.gson.toJsonTree(
            // since NullData implements Dataset thus inherits WithCustomSerialization interface
            // and NullData.getDataToSerialize = NullData.instance
            NullData.instance.asPrimitive(), Primitive.class);
      }
      // this just an easy case to detect. More generally, when X.getDataToSerialize() returns a
      // data that includes X in its subtree will result in StackOverFlowError.
      if (dataToSerialize == src) {
        throw new IllegalArgumentException(
            "Data:getDataToSerialize returns itself. Will result in StackOverflowError if"
                + " proceeded.");
      }
      return JsonSerializerDeserializer.gson.toJsonTree(dataToSerialize, Data.class);
    }
    if (src.isPrimitive()) {
      return JsonSerializerDeserializer.gson.toJsonTree(src.asPrimitive(), Primitive.class);
    } else if (src.isContainer()) {
      return JsonSerializerDeserializer.gson.toJsonTree(src.asContainer(), Container.class);
    } else if (src.isArray()) {
      return JsonSerializerDeserializer.gson.toJsonTree(src.asArray(), Array.class);
    } else if (src.isDataset()) {
      Data dataToSerialize = src.asDataset().getDataToSerialize();
      return JsonSerializerDeserializer.gson.toJsonTree(dataToSerialize, Data.class);
    } else {
      return null;
    }
  }
}
