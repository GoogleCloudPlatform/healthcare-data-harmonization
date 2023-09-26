/*
 * Copyright 2021 Google LLC.
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

import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Core;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.MergeModeExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.merge.MergeMode;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import java.util.Deque;
import java.util.Locale;

/** Writes to a side (formerly known as root) target. */
public class SideTarget extends MappingTarget {

  /** Meta key for the side target data. */
  public static final String SIDES_STACK_META_KEY = "sideTargets";

  public SideTarget(Path path) {
    // Legacy behaviour
    super(path, MergeMode.MERGE);
  }

  public SideTarget(Path path, MergeMode mergeMode) {
    super(path, mergeMode);
  }

  @Override
  protected void updateTarget(RuntimeContext ctx, Data updatedTarget) {
    Deque<Data> sideCatchers = ctx.getMetaData().getMeta(SIDES_STACK_META_KEY);
    if (sideCatchers != null) {
      sideCatchers.push(updatedTarget);
      return;
    }

    // If there is no side output catcher set (i.e. Core.withSides has not been called, resort
    // to legacy whistle behaviour, and just write to root frame).
    ctx.bottom().setVar(WhistleFunction.OUTPUT_VAR, updatedTarget);
  }

  @Override
  protected Data getTarget(RuntimeContext ctx) {
    Deque<Data> sideCatchers = ctx.getMetaData().getMeta(SIDES_STACK_META_KEY);
    if (sideCatchers != null && !sideCatchers.isEmpty()) {
      return sideCatchers.pop();
    }

    // If there is no side output catcher set (i.e. Core.withSides has not been called, resort
    // to legacy whistle behaviour, and just write to root frame).
    return ctx.bottom().getVar(WhistleFunction.OUTPUT_VAR);
  }

  @Override
  protected boolean shouldSkipWrite(RuntimeContext context, Data value) {
    return value.isNullOrEmpty();
  }

  /** Creates a SideTarget. */
  public static class Constructor implements Target.Constructor {

    public static final String TARGET_NAME = "side";

    @Override
    public String getName() {
      return TARGET_NAME;
    }

    /**
     * Writes to a side output (for using with {@link Core#withSides}.
     *
     * <p>For example:
     *
     * <pre><code>
     * // or equivalently side my_field:...
     * side("my_field"):... // will write to the my_field field of the side output which is then
     *                        // merged in withSides.
     *
     * // A complete usecase:
     *
     * result: withSides({
     *   one: 1
     *   addSomeSides(333)
     *   two: 2
     * })
     *
     * // ... somewhere later on
     *
     * def addSomeSides(num) {
     *   side sideNum.value: num
     *   // or equivalently side("sideNum.value"): num
     * }
     *
     *
     * // result above will be {
     * //  one: 1
     * //  two: 2
     * //  sideNum: {
     * //    value: 333
     * //  }
     * // }
     * // Do note that the field ordering here is for illustration, no ordering is guaranteed
     * // by the side target.
     * </code></pre>
     *
     * For more information see {@link Core#withSides}.
     *
     * <p><param name="path" type="String">Path on the side output to write to. These are then
     * merged with the main output of withSides.</param>
     *
     * <p><param name="mergeMode" type="optional String"> The mode to use for merging. Possible
     * options are:
     *
     * <ul>
     *   <li>replace - replace the given field with the data.
     *   <li>merge - recursively merge the given field with the data (default).
     *   <li>append - append the data as the last element in the given (array) field.
     *   <li>extend - extend the given field with the data. For arrays this concatenates, for
     *       containers this adds only missing fields.
     * </ul>
     *
     * Note: Primitives will always be replaced, regardless of merge mode.</param>
     */
    @Override
    public Target construct(RuntimeContext ctx, Data... args) {
      if (args.length > 2
          || args.length == 0
          || stream(args).anyMatch(a -> !a.isPrimitive() || a.asPrimitive().string() == null)) {
        throw new IllegalArgumentException(
            String.format(
                "Must provide one or two string argument specifying the path on the side output to"
                    + " write to, and optionally the merge mode to use. E.x. %1$s(\"my_field\"):"
                    + " ... will write to (i.e. merge with) the my_field field of the side output"
                    + " which is then merged in withSides. %1$s(\"my_field\", \"replace\"): ..."
                    + " will replace the my_field field of the side output (it will still be merged"
                    + " in withSides).",
                getName()));
      }

      MergeMode mode = MergeMode.MERGE;
      if (args.length == 2) {
        if (!MergeModeExperiment.isEnabled(ctx)) {
          throw new UnsupportedOperationException(
              String.format(
                  "Merge modes are an experimental feature and must be enabled by adding option"
                      + " \"%s\" to the top of your file.",
                  new MergeModeExperiment().getName()));
        }
        mode = MergeMode.valueOf(args[1].asPrimitive().string().toUpperCase(Locale.ROOT));
      }

      return new SideTarget(Path.parse(args[0].asPrimitive().string()), mode);
    }
  }
}
