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

package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/** DefaultPrimitive is a simple implementation of the {@link Primitive} interface. */
public class DefaultPrimitive implements Primitive {

  private Double num;
  private String str;
  private Boolean bool;

  public DefaultPrimitive(Double num) {
    this(num, null, null);
  }

  public DefaultPrimitive(String str) {
    this(null, str, null);
  }

  public DefaultPrimitive(Boolean bool) {
    this(null, null, bool);
  }

  private DefaultPrimitive(Double num, String str, Boolean bool) {
    this.num = num;
    this.str = str;
    this.bool = bool;
  }

  @Override
  public Double num() {
    return num;
  }

  @Override
  public String string() {
    return str;
  }

  @Override
  public Boolean bool() {
    return bool;
  }

  @Override
  public boolean isNullOrEmpty() {
    return (str == null || str.isEmpty()) && bool == null && num == null;
  }

  @Override
  public Data deepCopy() {
    return new DefaultPrimitive(num, str, bool);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Data) || !((Data) o).isPrimitive()) {
      return false;
    }
    Primitive other = ((Data) o).asPrimitive();
    return (Objects.equals(bool, other.bool())
            && Objects.equals(str, other.string())
            && Objects.equals(num, other.num()))
        || (isNullOrEmpty() && other.isNullOrEmpty());
  }

  @Override
  public int hashCode() {
    if (isNullOrEmpty()) {
      return NullData.instance.hashCode();
    }
    return Objects.hash(bool, num, str);
  }

  @Override
  public String toString() {
    if (null != num) {
      if (isFractionNegligible()) {
        return String.valueOf(rounded());
      }
      return String.valueOf(num);
    } else if (null != bool) {
      return String.valueOf(bool);
    } else {
      return str;
    }
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.writeObject(num);
    oos.writeObject(str);
    oos.writeObject(bool);
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    // default deserialization
    num = (Double) ois.readObject();
    str = (String) ois.readObject();
    bool = (Boolean) ois.readObject();
  }
}
