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

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.MergeModeExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.merge.MergeMode;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import java.util.Locale;

/** Writes to a variable on the stack frame currently on top. */
public class VarTarget extends MappingTarget {
  private static final MergeMode DEFAULT_MERGE_MODE = MergeMode.MERGE;
  private final String var;

  public VarTarget(String var, Path path) {
    // Legacy behaviour
    this(
        var,
        path,
        WhistleFunction.OUTPUT_VAR.equals(var) || !path.equals(Path.empty())
            ? MergeMode.MERGE
            : MergeMode.REPLACE);
  }

  public VarTarget(String var, Path path, MergeMode mergeMode) {
    super(path, mergeMode);
    this.var = var;
  }

  @Override
  protected void updateTarget(RuntimeContext ctx, Data updatedTarget) {
    ctx.top().setVar(var, updatedTarget);
  }

  @Override
  protected Data getTarget(RuntimeContext ctx) {
    return ctx.top().getVar(var);
  }

  @Override
  protected boolean shouldSkipWrite(RuntimeContext context, Data value) {
    // Match legacy whistle behaviour. If the target is a variable (that isn't the output or an
    // output field), we still want to run the merge, as setting a var to null should be doable.
    // TODO(): This logic needs refining pending UX.
    boolean isOutput = WhistleFunction.OUTPUT_VAR.equals(var);
    boolean isField = !path.equals(Path.empty());
    return value.isNullOrEmpty() && (isOutput || isField);
  }

  public String getVar() {
    return var;
  }

  /**
   * Constructor class for creating an instance of the VarTarget target to allow users to generate a
   * Container or Array entry using a {@link Path} variable.
   */
  public static final class Constructor implements Target.Constructor {

    public static final String TARGET_NAME = "set";

    /**
     * Writes to the specified path on either the specified variable or to the output of the
     * enclosing block (if var is omitted).
     *
     * <p>For example:
     *
     * <pre><code>
     * result: setSomeVars(100)
     *
     * def setSomeVars(num) {
     *   var my_var: 0 // Although the below statements will be able to set the vars without them
     *                 // being declared here, we need these declarations to read them later,
     *                 // or the transpiler will complain.
     *   var my_var2: 0
     *
     *   set("my_var", ""): num // Sets directly to my_var, overwriting the 0 with num (100)
     *   set("hundred"): my_var // Reads the prior value of 100, and sets it on field "hundred"
     *                          // on the output of this block. Same as doing:
     *                          // hundred: my_var
     *
     *   set("my_var2", "field"): num + 1 // my_var2 becomes a container, and sets field "field"
     *                                    // on it to 101
     *   set("my_var2", "field2.value"): num + 10 // Add a new field to my_var2 (alongside the
     *                                            // prior) and set the nested value to 110.
     *   set("$this", "nested"): my_var2 // Sets field "nested" on the output of this block.
     * }
     *
     * // result is now {
     * //    hundred: 100
     * //    nested: {
     * //      field: 101
     * //      field2: {
     * //        value: 110
     * //      }
     * //    }
     * //}
     * </code></pre>
     *
     * <param name="var" type="optional String"> The variable to write to. Defaults to
     * $this.</param>
     *
     * <p><param name="field" type="String"> The path on the var to write. An empty string will
     * write directly to the var.</param>
     *
     * <p><param name="mergeMode" type="optional String"> The mode to use for merging. Possible
     * options are:
     *
     * <ul>
     *   <li>replace - replace the given var's given field with the data.
     *   <li>merge - recursively merge the given var's given field with the data.
     *   <li>append - append the data as the last element in given var's given (array) field.
     *   <li>extend - extend the given var's given field with the data. For arrays this
     *       concatenates, for containers this adds only missing fields.
     * </ul>
     *
     * Note: Primitives will always be replaced, regardless of merge mode.</param>
     */
    @Override
    public Target construct(RuntimeContext ctx, Data... args) {
      if (args.length > 3
          || args.length == 0
          || stream(args).anyMatch(a -> !a.isPrimitive() || a.asPrimitive().string() == null)) {
        throw new IllegalArgumentException(
            String.format(
                "Must provide one to three string arguments to %s (provided %d) that specifies the"
                    + " variable to write to (optional, defaults to %s), the path on it to write,"
                    + " and the merge mode to use (optional, defaults to 'replace', if specified,"
                    + " var name must be specified too)."
                    + " E.x %1$s(\"my_var\",\".field1[0].field2\"):..., or"
                    + " %1$s(\".field1[0].field2\"):..., or"
                    + " %1$s(\"my_var\",\".field1[0].field2\", \"extend\"):...",
                getName(), args.length, WhistleFunction.OUTPUT_VAR));
      }

      String var = WhistleFunction.OUTPUT_VAR;
      Path path = Path.empty();
      MergeMode mode = DEFAULT_MERGE_MODE;

      if (args.length == 1) {
        // Merge with $this
        path = Path.parse(args[0].asPrimitive().string());

        // TODO(rpolyano): Uncomment when default merge mode depends on options (rather than being
        //  const)
        // if (path.isEmpty()) {
        //   mode = MergeMode.MERGE;
        // }
      }
      if (args.length >= 2) {
        var = args[0].asPrimitive().string();
        path = Path.parse(args[1].asPrimitive().string());

        if (!var.equals(WhistleFunction.OUTPUT_VAR) && path.isEmpty()) {
          mode = MergeMode.REPLACE;
        }
      }
      if (args.length == 3) {
        if (!MergeModeExperiment.isEnabled(ctx)) {
          throw new UnsupportedOperationException(
              String.format(
                  "Merge modes are an experimental feature and must be enabled by adding option"
                      + " \"%s\" to the top of your file.",
                  new MergeModeExperiment().getName()));
        }
        mode = MergeMode.valueOf(args[2].asPrimitive().string().toUpperCase(Locale.ROOT));
      }

      return new VarTarget(var, path, mode);
    }

    @Override
    public String getName() {
      return TARGET_NAME;
    }
  }
}
