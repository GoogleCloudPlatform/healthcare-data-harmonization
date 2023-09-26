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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.data.Preconditions.requireNum;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;

/** Builtin operators. */
public final class Operators {

  /**
   * Returns a {@code Primitive} representing the sum of the arguments. {@code number} arguments are
   * added. {@code string} arguments are concatenated. If any argument is a {@code string}, every
   * argument is treated like a {@code string}. If arguments are only {@code number} and
   * {@code boolean} or only {@code boolean}, an IllegalArgumentException is thrown.
   *
   * <pre><code>
   * // Returns 10
   * sum(5, 5)
   *
   * // Returns hello world
   * sum("hello ", "world")
   *
   * // Returns 1 hello 2 world
   * // The + operator is shorthand for the sum function.
   * 1 + " hello " + 2 + " world"
   *
   * // If any argument is a string, every argument is treated like a string.
   * // Returns true1hello
   * sum(true, 1, "hello")
   * <code></pre>
   *
   * @param first the first value to sum
   * @param rest the remaining values to sum
   * @return {@link Primitive} representation of the sum
   * @throws IllegalArgumentException if the arguments are only {@code number} and {@code boolean}
   * or only {@code boolean}
   */
  @PluginFunction
  public static Primitive sum(RuntimeContext context, Primitive first, Primitive... rest) {
    if (first.string() != null || stream(rest).anyMatch(p -> p.string() != null)) {
      return context
          .getDataTypeImplementation()
          .primitiveOf(stream(rest).map(Object::toString).reduce(first.toString(), String::concat));
    } else if (first.bool() == null
        && stream(rest).filter(r -> !r.isNullOrEmpty()).allMatch(p -> p.num() != null)) {
      return context
          .getDataTypeImplementation()
          .primitiveOf(
              stream(rest)
                  .map(p -> requireNum(p, "operand"))
                  .reduce(requireNum(first, "operand"), Double::sum));
    }
    throw new IllegalArgumentException(
        String.format(
            "Cannot mix and match these argument types to sum: %s",
            stream(rest).map(Object::toString).reduce(first.toString(), String::concat)));
  }

  /**
   * Returns a {@code Primitive} {@code number} representing the difference of the arguments. {@code
   * null} arguments are treated like the {@code number} {@code 0}.
   *
   * <pre><code>
   * // Returns 1
   * sub(10, 5, 4)
   *
   * // Returns 2. The - operator is shorthand for the sub function.
   * 3 - 1
   *
   * // Returns 6. The null value is treated like the number 0
   * sub(10, "", 4)
   * <code></pre>
   *
   * @param first the first number
   * @param rest the remaining numbers to subtract from {@code first}
   * @return {@link Primitive} {@code number}
   * @throws IllegalArgumentException if one or more arguments isn't a {@code number}
   */
  @PluginFunction
  public static Primitive sub(RuntimeContext context, Primitive first, Primitive... rest) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(
            stream(rest)
                .map(d -> requireNum(d, "operand"))
                .reduce(requireNum(first, "operand"), (a, b) -> a - b));
  }

  /**
   * Returns {@code Primitive} {@code boolean} with the value {@code true} if all arguments are
   * equal.
   *
   * <pre><code>
   * // Returns true
   * eq(10, 10)
   *
   * // Returns true. The == operator is shorthand for the eq function.
   * "text" == "text"
   *
   * // Returns true
   * eq(5, 5, 5, 5, 5, 5)
   *
   * // Returns false
   * eq(true, 7)
   *
   * // Returns true (both are equivalent null data)
   * eq([], {})
   * <code></pre>
   *
   * @param first the first value to compare
   * @param second the second value to compare
   * @param rest the remaining values to compare
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive eq(RuntimeContext context, Data first, Data second, Data... rest) {
    boolean isEq = first.equals(second);
    for (int i = 0; i < rest.length && isEq; i++) {
      isEq = isEq && rest[i].equals(first);
    }

    return context.getDataTypeImplementation().primitiveOf(isEq);
  }

  /**
   * Returns a {@code Primitive} {@code boolean} with the value {@code true} if any two arguments
   * are unequal.
   *
   * <pre><code>
   * // Returns true
   * neq("text1", "text2")
   *
   * // Returns true. The != operator is shorthand for the neq function.
   * false != 7
   *
   * // Returns false
   * neq("text1", "text1")
   * <code></pre>
   *
   * @param first the first value to compare
   * @param second the second value to compare
   * @param rest the remaining values to compare
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive neq(RuntimeContext context, Data first, Data second, Data... rest) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(!eq(context, first, second, rest).bool());
  }

  /**
   * Returns a {@code Primitive} {@code number} representing the product of the arguments. {@code
   * null} arguments are treated like the {@code number} 0.
   *
   * <pre><code>
   * // Returns 100
   * mul(5, 10, 2)
   *
   * // Returns 50. The * operator is shorthand for the mul function.
   * 5 * 10
   * <code></pre>
   *
   * @param first the first number to multiply
   * @param rest the remaining numbers to multiply
   * @return {@link Primitive} {@code number}
   * @throws IllegalArgumentException if one or more arguments isn't a {@code number}
   */
  @PluginFunction
  public static Primitive mul(RuntimeContext context, Primitive first, Primitive... rest) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(
            stream(rest)
                .map(d -> requireNum(d, "operand"))
                .reduce(requireNum(first, "operand"), (a, b) -> a * b));
  }

  /**
   * Returns a {@code Primitive} {@code number} representing the quotient of two arguments. {@code
   * null} arguments are treated like the {@code number} {@code 0}.
   *
   * <pre><code>
   * // Returns 5
   * div(10, 2)
   *
   * // Returns 10. The / operator is shorthand for the div function.
   * 20/2
   *
   * // Returns 0.5
   * div(1, 2)
   * <code></pre>
   *
   * @param dividend the number to divide
   * @param divisor the number by which to divide the {@code dividend}
   * @return {@link Primitive} {@code number}
   * @throws IllegalArgumentException if one or more arguments isn't a {@code number}
   */
  @PluginFunction
  public static Primitive div(RuntimeContext context, Primitive dividend, Primitive divisor) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(requireNum(dividend, "dividend") / requireNum(divisor, "divisor"));
  }

  /**
   * Returns {@code true} if the value of {@code left} is greater than the value of {@code right}.
   * {@code null} values are treated like the {@code number} {@code 0}.
   *
   * <pre>{@code
   * // Returns true
   * gt(10, 1)
   *
   * // Returns true. The > operator is shorthand for the gt function.
   * 5 > 1
   *
   * // Returns false
   * gt(1, 10)
   * }</pre>
   *
   * @param left number to compare
   * @param right the second number to compare
   * @return {@link Primitive} {@code boolean}
   * @throws IllegalArgumentException if one or more values isn't a {@code number}
   */
  @PluginFunction
  public static Primitive gt(RuntimeContext context, Primitive left, Primitive right) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(requireNum(left, "left") > requireNum(right, "right"));
  }

  /**
   * Returns {@code true} if the value of {@code left} is greater than or equal to the value of
   * {@code right}. {@code null} values are treated like the {@code number} 0.
   *
   * <pre><code>
   * // Returns true
   * gtEq(10, 1)
   *
   * // Returns true. The >= operator is shorthand for the gtEq function.
   * 5 >= 5
   *
   * Returns false
   * gtEq(1, 10)
   * <code></pre>
   *
   * @param left the first number to compare
   * @param right the second number to compare
   * @return {@link Primitive} {@code boolean}
   * @throws IllegalArgumentException if one or more values isn't a {@code number}
   */
  @PluginFunction
  public static Primitive gtEq(RuntimeContext context, Primitive left, Primitive right) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(requireNum(left, "left") >= requireNum(right, "right"));
  }

  /**
   * Returns {@code true} if the value of {@code left} is less than the value of {@code right}.
   * {@code null} values are treated like the {@code number} 0.
   *
   * <pre>{@code
   * // Returns true
   * lt(1, 10)
   *
   * // Returns true. The < operator is shorthand for the lt function.
   * 1 < 2
   *
   * // Returns false
   * lt(2, 1)
   * }</pre>
   *
   * @param left the first number to compare
   * @param right the second number to compare
   * @return {@link Primitive} {@code boolean}
   * @throws IllegalArgumentException if one or more values isn't a {@code number}
   */
  @PluginFunction
  public static Primitive lt(RuntimeContext context, Primitive left, Primitive right) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(requireNum(left, "left") < requireNum(right, "right"));
  }

  /**
   * Returns {@code true} if the value of {@code left} is less than or equal to the value of {@code
   * right}. {@code null} values are treated like the {@code number} 0.
   *
   * <pre><code>
   * // Returns true
   * ltEq(5, 5)
   *
   * // Returns true. The <= operator is shorthand for the ltEq function.
   * 1 <= 10
   *
   * // Returns false
   * ltEq(10, 1)
   * <code></pre>
   *
   * @param left the first number to compare
   * @param right the second number to compare
   * @return {@link Primitive} {@code boolean}
   * @throws IllegalArgumentException if one or more values isn't a {@code number}
   */
  @PluginFunction
  public static Primitive ltEq(RuntimeContext context, Primitive left, Primitive right) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(requireNum(left, "left") <= requireNum(right, "right"));
  }

  /**
   * Returns {@code true} if the value of {@code Data} isn't null or empty, depending on the {@code
   * Data} implementation.
   *
   * <pre><code>
   * // Returns true because x isn't empty.
   * var x: "abc"
   * isNotNil(x)
   *
   * // Returns true because dir isn't empty. dir becomes "path/".
   * // The ? operator is shorthand for the isNotNil function.
   * var dir: "path"
   * if dir? then {
   *   var dir: dir + "/";
   * }
   *
   * // Returns false because dataContainer is empty.
   * var dataContainer: {}
   * isNotNil(dataContainer)
   * <code></pre>
   *
   * @param data a {@code Data} data type
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive isNotNil(RuntimeContext context, Data data) {
    return context.getDataTypeImplementation().primitiveOf(!data.isNullOrEmpty());
  }

  /**
   * Returns {@code true} if the value of {@code Data} is null or empty, depending on the {@code
   * Data} implementation.
   *
   * <pre><code>
   * // Returns true because dir is empty.
   * var dir: ""
   * isNil(dir)
   *
   * // Returns false because x isn't empty. The !? operator is shorthand for the isNil function.
   * var x: "abc"
   * !x?
   *
   * // Returns true because emptyContainer is empty.
   * var emptyContainer: {}
   * isNil(emptyContainer)
   * <code></pre>
   *
   * @param data a {@code Data} data type
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive isNil(RuntimeContext context, Data data) {
    return context.getDataTypeImplementation().primitiveOf(data.isNullOrEmpty());
  }

  /**
   * Returns {@code true} if {@code data} represents a {@code Primitive} {@code boolean} with a
   * falsey value. Returns {@code true} if {@code data} is null or empty.
   *
   * <pre><code>
   * // Returns true
   * var a: false
   * not(a)
   *
   * // Returns false. The ! operator is shorthand for the not function.
   * var a: true
   * !a
   *
   * // Returns true
   * var b: ""
   * not(b)
   * <code></pre>
   *
   * @param data a {@code Data} data type
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive not(RuntimeContext context, Data data) {
    return context.getDataTypeImplementation().primitiveOf(!Ternary.isTruthy(data));
  }

  /**
   * Returns {@code true} if any arguments are truthy. Evaluation stops after encountering a truthy
   * argument.
   *
   * <pre><code>
   * // Returns true
   * var a: "mystring"
   * var b: "yourstring"
   * (a == "mystring" or b == "yourstring")
   *
   * // Returns false
   * var c: "astring"
   * var d: "astring"
   * (b == "string" or c == "string")
   *
   * // Returns true. Evaluation continues after the first conditional even though it's false, then
   * // evaluation stops after encountering y == 2, which is true, and doesn't evaluate z == 3.
   * var x: 1
   * var y: 2
   * var z: 3
   * (a == 2 or y == 2 or z == 3)
   * <code></pre>
   *
   * @param args arguments to evaluate. A {@code Closure} contains a lazily evaluated expression
   * which will be evaluated by the function if and when appropriate.
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive or(RuntimeContext context, Closure... args) {
    for (Closure c : args) {
      if (Ternary.isTruthy(c.execute(context))) {
        return context.getDataTypeImplementation().primitiveOf(true);
      }
    }
    return context.getDataTypeImplementation().primitiveOf(false);
  }

  /**
   * Returns {@code true} if all arguments are truthy. Evaluation stops after encountering a falsey
   * argument.
   *
   * <pre><code>
   * // Returns true
   * var a: "string1"
   * var b: "string2"
   * (a == "string1" and b == "string2")
   *
   * // Returns false
   * var c: "string3"
   * var d: "string3"
   * (c == "string3" and d == "string4")
   *
   * // Returns false. Evaluation stops after encountering the falsey argument y == "string1".
   * // The actual value of y is "string2".
   * var x: "string1"
   * var y: "string2"
   * var z: "string3"
   * (x == "string1" and y == "string1" and z == "string3")
   * <code></pre>
   *
   * @param args arguments to evaluate. A {@code Closure} contains a lazily evaluated expression
   * which will be evaluated by the function if and when appropriate.
   * @return {@link Primitive} {@code boolean}
   */
  @PluginFunction
  public static Primitive and(RuntimeContext context, Closure... args) {
    if (args.length == 0) {
      return context.getDataTypeImplementation().primitiveOf(false);
    }
    for (Closure c : args) {
      if (!Ternary.isTruthy(c.execute(context))) {
        return context.getDataTypeImplementation().primitiveOf(false);
      }
    }
    return context.getDataTypeImplementation().primitiveOf(true);
  }

  /**
   * Returns the largest {@code Primitive} {@code number} integer value that is less than or equal
   * to the argument.
   *
   * @param data a {@code Data} data type
   * @throws IllegalArgumentException if the argument isn't a {@code number}
   */
  @PluginFunction
  public static Primitive floor(RuntimeContext context, Data data) {
    return context
        .getDataTypeImplementation()
        .primitiveOf((double) Math.floor(requireNum(data, "value")));
  }

  private Operators() {}
}
