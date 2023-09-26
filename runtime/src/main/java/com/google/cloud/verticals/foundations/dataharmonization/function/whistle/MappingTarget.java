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

package com.google.cloud.verticals.foundations.dataharmonization.function.whistle;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.merge.MergeMode;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;

/**
 * Target for writing fields and variables. This executes the writes for a single mapping
 * (corresponding to {@link FieldMapping#getField()} or {@link FieldMapping#getVar()}).
 *
 * <p>The implementation of this class is guided by go/dh-whistle-elp-merges.
 */
public abstract class MappingTarget implements Target {

  protected final Path path;
  private final MergeMode mergeMode;

  protected MappingTarget(Path path, MergeMode mergeMode) {
    this.path = path;
    this.mergeMode = mergeMode;
  }

  @Override
  public void write(RuntimeContext ctx, Data value) {
    if (shouldSkipWrite(ctx, value)) {
      return;
    }

    Data target = getTarget(ctx);

    target =
        mergeMode.getStrategy(target).merge(ctx.getDataTypeImplementation(), target, value, path);

    updateTarget(ctx, target);
  }

  /** Writes back the updated target value. */
  protected abstract void updateTarget(RuntimeContext ctx, Data updatedTarget);

  /** Returns the target value onto which to apply the path. */
  protected abstract Data getTarget(RuntimeContext ctx);

  /** Returns true if a write should be skipped given the state of the context and/or value. */
  protected abstract boolean shouldSkipWrite(RuntimeContext context, Data value);
}
