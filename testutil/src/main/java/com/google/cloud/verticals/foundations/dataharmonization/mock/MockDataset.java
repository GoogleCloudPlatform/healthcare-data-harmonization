/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.mock;

import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.TransparentCollection;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.Objects;
import java.util.stream.Stream;

/** Mock implementation of a Dataset, using an Array as the data source. */
public class MockDataset implements Dataset, TransparentCollection<MockDataset> {
  private final Array backing;

  public MockDataset(Array backing) {
    this.backing = backing;
  }

  @Override
  public Dataset map(RuntimeContext ctx, Closure closure, boolean flatten) {

    Stream<Data> projection =
        backing.stream().map(elem -> closure.bindNextFreeParameter(elem).execute(ctx));
    if (flatten) {
      projection = projection.flatMap(d -> d.isArray() ? d.asArray().stream() : Stream.of(d));
    }
    return new MockDataset(arrayOf(projection.toArray(Data[]::new)));
  }

  @Override
  public MockDataset getThrough(Path path) {
    return new MockDataset(backing.getThrough(path));
  }

  @Override
  public MockDataset flatten() {
    return new MockDataset(backing.flatten());
  }

  @Override
  public Data getDataToSerialize() {
    return backing;
  }

  @Override
  public boolean isNullOrEmpty() {
    return backing.isNullOrEmpty();
  }

  @Override
  public Data deepCopy() {
    return new MockDataset(backing.deepCopy().asArray());
  }

  public Array getBackingArray() {
    return backing;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof MockDataset)) {
      return false;
    }
    return backing.equals(((MockDataset) obj).backing);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(backing);
  }

  @Override
  public String toString() {
    return "MockDataset{backing=" + backing + '}';
  }
}
