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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.targets;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultOverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.ExamplePlugin;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableList;
import java.util.stream.IntStream;

/**
 * This class displays an example of a Target implementation. Targets consist of two components -
 * the {@link Constructor} and the Target itself.
 *
 * <p>The {@link Constructor#construct(RuntimeContext, Data...)} method is called with the arguments
 * provided to the target:
 *
 * <pre>
 * <code>
 *  example(1, 2, 3): value
 * </code>
 * </pre>
 *
 * in the above example, <code>1, 2, 3</code> are passed to the Constructor, which then returns an
 * instance of ExampleTarget. <code>value</code> is then passed to the {@link
 * ExampleTarget#write(RuntimeContext, Data)} method of that returned instance.
 */
public class ExampleTarget implements Target {

  private final Double a;
  private final Double b;
  private final Double c;

  public ExampleTarget(Double a, Double b, Double c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  @Override
  public void write(RuntimeContext ctx, Data value) {
    System.out.printf("example target called with %f, %f, %f and got value %s%n", a, b, c, value);
  }

  /**
   * The Constructor is the entry point for the target. See the documentation on {@link
   * ExampleTarget} for the full workflow.
   */
  public static class Constructor implements Target.Constructor {
    // Create a Signature manually to help with checking incoming args. Some day, this will happen
    // automatically. This is not a required thing for targets and just convenient.
    private static final Signature SIGNATURE =
        new Signature(
            ExamplePlugin.NAME,
            "example",
            ImmutableList.of(Primitive.class, Primitive.class, Primitive.class),
            false);

    @Override
    public String getName() {
      // This is the name by which Whistle code will refer to the target.
      return SIGNATURE.getName();
    }

    /**
     * Javadoc for the example Target, which expects three double arguments.
     *
     */
    @Override
    public Target construct(RuntimeContext ctx, Data... args) {
      // Note: The runtime does not know (and therefore cannot check) the signature of the target.
      // We can, however, use some helpful Runtime APIs to do this easily. This is just a
      // convenience, we could just as easily check each argument manually.
      if (args.length != SIGNATURE.getArgs().size()
          || Double.isInfinite(DefaultOverloadSelector.distance(SIGNATURE, args))) {
        throw new IllegalArgumentException(
            String.format(
                "example target expects 3 double arguments, got %s",
                stream(args).map(d -> d.getClass().getSimpleName()).collect(joining(", "))));
      }

      // Since Primitives can be string or num or bool, let's make sure these are all doubles.
      if (stream(args).anyMatch(a -> a.asPrimitive().num() == null)) {
        String[] offendingArgIndices =
            IntStream.range(0, args.length)
                .filter(i -> args[i].asPrimitive().num() == null)
                .mapToObj(String::valueOf)
                .toArray(String[]::new);
        // Did somebody say localization? I hope not.
        throw new IllegalArgumentException(
            String.format(
                "example target expects 3 double arguments, but arg%s at %s %s not numeric",
                offendingArgIndices.length > 1 ? "s" : "",
                String.join(", ", offendingArgIndices),
                offendingArgIndices.length > 1 ? "were" : "was"));
      }

      // We have verified the args array, now we construct the target.
      return new ExampleTarget(
          args[0].asPrimitive().num(), args[1].asPrimitive().num(), args[2].asPrimitive().num());
    }
  }
}
