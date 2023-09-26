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
package com.google.cloud.verticals.foundations.dataharmonization.function;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.Objects;
import java.util.SortedSet;

/**
 * Closure is an interface for a (maybe) ready-to-execute function call. It includes the arguments,
 * along with any unbound free parameters (which should be bound before the Closure is executed).
 */
public interface Closure extends Data {

  /** Returns the bound and unbound ({@link FreeParameter}) arguments in the closure. */
  Data[] getArgs();

  /**
   * Binds (i.e. copies the closure and replaces) the next {@link FreeParameter} with the given
   * value, according to the first element of {@link #getFreeArgIndices()}.
   *
   * <p>Note that this will return a copy of the closure with the parameter bound and must not
   * change this closure.
   */
  Closure bindNextFreeParameter(Data value);

  /**
   * Executes this closure and returns the result. Closure must have no free parameters.
   *
   * @param context RuntimeContext to execute with.
   * @return The function result.
   */
  Data execute(RuntimeContext context);

  /**
   * Returns the number of unbound parameters. {@link #bindNextFreeParameter(Data)} must be called
   * this many times (but note that it copies, see {@link #bindNextFreeParameter(Data)}.
   */
  int getNumFreeParams();

  /**
   * Returns the indices in the arguments array ({@link #getArgs()}) that are free parameters
   * ({@link FreeParameter}).
   */
  SortedSet<Integer> getFreeArgIndices();

  /**
   * Returns a descriptive name for the closure. This can be the function name or an internal name.
   */
  String getName();

  /** Represents a free (unbound) argument in a closure. */
  final class FreeParameter implements Data {
    private final String name;

    public FreeParameter(String name) {
      this.name = name;
    }

    @Override
    public boolean isNullOrEmpty() {
      return false;
    }

    @Override
    public Data deepCopy() {
      return new FreeParameter(name);
    }

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FreeParameter)) {
        return false;
      }
      FreeParameter that = (FreeParameter) o;
      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name);
    }
  }
}
