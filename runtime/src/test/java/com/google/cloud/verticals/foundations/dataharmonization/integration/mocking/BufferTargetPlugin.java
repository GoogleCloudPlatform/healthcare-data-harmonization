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

package com.google.cloud.verticals.foundations.dataharmonization.integration.mocking;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin that provides a custom target where you can access all that's been written to it. Used to
 * test mock target functionality.
 */
public class BufferTargetPlugin implements Plugin {
  public static final String PKG_NAME = "bufferTargetPlugin";

  @Override
  public String getPackageName() {
    return PKG_NAME;
  }

  @Override
  public List<Constructor> getTargets() {
    return ImmutableList.of(new BufferTarget.Constructor());
  }

  /**
   * A singleton target that records all {@link Data} that's used in its construction and all {@link
   * Data} written to it.
   */
  public static class BufferTarget implements Target {
    public static final BufferTarget instance = new BufferTarget();
    private final List<Data> buffer = new ArrayList<>();

    private BufferTarget() {}

    @Override
    public void write(RuntimeContext ctx, Data value) {
      buffer.add(value);
    }

    public void clearBuffer() {
      buffer.clear();
    }

    public List<Data> getBufferContent() {
      return ImmutableList.copyOf(buffer);
    }

    /**
     * A placeholder "Constructor" for {@link BufferTarget} that records arguments used for
     * constructing {@link BufferTarget} in its singleton's buffer.
     */
    public static class Constructor implements Target.Constructor {
      public static final String TARGET_NAME = "bufferTarget";

      @Override
      public Target construct(RuntimeContext ctx, Data... args) {
        for (Data arg : args) {
          BufferTarget.instance.write(ctx, arg);
        }
        return BufferTarget.instance;
      }

      @Override
      public String getName() {
        return TARGET_NAME;
      }
    }
  }
}
