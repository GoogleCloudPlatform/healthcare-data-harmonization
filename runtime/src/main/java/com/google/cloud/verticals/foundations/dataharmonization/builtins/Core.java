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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.Errors;
import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.SideTarget;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.google.errorprone.annotations.Var;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Core builtin functions. */
public class Core {
  private static final String NIL = "nil";
  private static final String STR = "str";
  private static final String NUM = "num";
  private static final String BOOL = "bool";
  private static final String KEY = "key";

  private Core() {}

  /**
   * Throws an exception with the given message. It will be caught by {@link Errors#withError} if
   * present.
   */
  @PluginFunction
  public static NullData fail(RuntimeContext ctx, String message) {
    throw WhistleRuntimeException.fromCurrentContext(ctx, new UserException(message));
  }

  /** Gets the value at the given path applied to the given value. */
  @PluginFunction
  public static Data get(Data source, String path) {
    return Path.parse(path).get(source);
  }

  /**
   * Returns a {@code container} who is the same as the provided {@code container} but with the
   * {@code field} removed. Depending on the container implementation used in {@link
   * RuntimeContext#getDataTypeImplementation()} the returned container may or may not be the same
   * object as the input.
   */
  @PluginFunction
  public static Container unset(Container container, String field) {
    return container.removeField(field);
  }

  /** Parses String and returns Primitive double. */
  @PluginFunction
  public static Primitive parseNum(RuntimeContext ctx, String str) {
    return ctx.getDataTypeImplementation().primitiveOf(Double.parseDouble(str));
  }

  /**
   * Tries to parse Primitive and returns Primitive double if primitive is String. Returns itself
   * otherwise.
   */
  @PluginFunction
  public static Primitive tryParseNum(RuntimeContext ctx, Primitive primitive) {
    String str = primitive.string();
    if (str != null) {
      return parseNum(ctx, str);
    }

    Boolean bool = primitive.bool();
    if (bool != null) {
      throw new IllegalArgumentException(
          "tryParseNum expects String or Num primitive, got Boolean.");
    }

    return primitive;
  }

  /**
   * Returns the type(s) of the given data. This will be an Array of strings. For example, a
   * Container may return {@code ["Container", "DefaultContainer"]} and an empty Array may return
   * {@code ["Array", "null", "DefaultArray"]}.
   *
   * <p>Possible basic types are: Array, Container, Primitive, Dataset, and null. Implementations,
   * like DefaultArray can vary depending on the execution environment.
   */
  @PluginFunction
  public static Array types(RuntimeContext ctx, Data data) {
    DataTypeImplementation dti = ctx.getDataTypeImplementation();
    return dti.arrayOf(types(data).stream().map(dti::primitiveOf).collect(Collectors.toList()));
  }

  /**
   * Returns the type(s) of the given data. For example, a Container may return {@code ["Container",
   * "DefaultContainer"]} and an empty Array may return {@code ["Array", "null", "DefaultArray"]}.
   *
   * <p>Possible basic types are: Array, Container, Primitive, Dataset, and null. Implementations,
   * like DefaultArray can vary depending on the execution environment.
   */
  public static ImmutableList<String> types(Data data) {
    ImmutableList.Builder<String> types = ImmutableList.builder();

    if (data.isArray() || data.isNullOrEmpty()) {
      types.add(Array.class.getSimpleName());
    }
    if (data.isContainer() || data.isNullOrEmpty()) {
      types.add(Container.class.getSimpleName());
    }
    if (data.isDataset() || data.isNullOrEmpty()) {
      types.add(Dataset.class.getSimpleName());
    }
    if (data.isPrimitive() || data.isNullOrEmpty()) {
      types.add(Primitive.class.getSimpleName());
    }
    if (data.isNullOrEmpty()) {
      types.add("null");
    }
    types.add(data.getClass().getSimpleName());

    return types.build();
  }

  /**
   * Returns a human-readable string containing the interfaces of the given data (i.e. Primitive,
   * Array, Container, Dataset), and nullity. Excludes the concrete implementation (e.x.
   * DefaultPrimitive).
   */
  public static String prettyTypes(Data data) {
    ImmutableList<String> types = types(data);
    return String.join("/", types.subList(0, types.size() > 1 ? types.size() - 1 : types.size()));
  }

  /**
   * Returns a deep copy of the input {@code Data}. Use this function to protect a variable from
   * being modified. See the following examples.
   *
   * <pre><code>
   * // A simple `billingAccount` container.
   * var billingAccount: {
   *    description: "Hospital charges"
   *    id: "example"
   *    resourceType: "Account"
   * }
   *
   * // Compare the following `modifierFunction()` and `modifierFunctionDeepCopy()` functions.
   * // `modifierFunction()` modifies the container passed to it and returns the modified value.
   * def modifierFunction(container) {
   *   var container.description: "MODIFIED"
   * }
   * // `modifierFunctionDeepCopy()` makes a deep copy of the container passed to it and doesn't
   * // modify the original container.
   * def modifierFunctionDeepCopy(container) {
   *   // Replaces `container` with a deep copy.
   *   var billingAccount: deepCopy(container)
   *   // Only modifies the copy in this function scope.
   *   var billingAccount.description: "MODIFIED"
   * }
   *
   * // Call `modifierFunction()` and pass the `billingAccount` container.
   * // `modifierFunction()` doesn't write any fields, so the returned value is null, but after
   * // calling this function, the value of `description` in `billingAccount` becomes `"MODIFIED"`.
   * var value: modifierFunction(billingAccount)
   *
   * // Remove the previous line `var value: modifierFunction(billingAccount)` from your Whistle
   * // configuration.
   * // Call `modifierFunctionDeepCopy()` instead and pass the `billingAccount` container.
   * // `modifierFunctionDeepCopy()` doesn't write any fields, so the returned value is null, but
   * // after calling this function using a deep copy, the value of `description` in
   * // `billingAccount` is unchanged.
   * var value: modifierFunctionDeepCopy(billingAccount)
   * </code></pre>
   *
   * @param data the {@code Data} to make a deep copy of
   * @return a deep copy of the original {@code Data}
   */
  @PluginFunction
  public static Data deepCopy(Data data) {
    return data.deepCopy();
  }

  /**
   * Returns true if the given data is of the given type (according to {@link #types}).
   *
   * <p>Possible basic types are: Array, Container, Primitive, Dataset, and null. Implementations,
   * like DefaultArray can vary depending on the execution environment.
   *
   * <p>This check is not sensitive to letter casing.
   */
  @PluginFunction
  public static Primitive is(RuntimeContext ctx, Data data, String type) {
    boolean isType =
        types(ctx, data).stream()
            .map(Data::asPrimitive)
            .map(Primitive::string)
            .anyMatch(s -> type != null && Ascii.equalsIgnoreCase(s, type));
    return ctx.getDataTypeImplementation().primitiveOf(isType);
  }

  /**
   * Generates a {@link Primitive} String hash from the given {@link Data} object. Key order is not
   * considered for {@link Container}s, ({@link Array} item order is). This is not cryptographically
   * secure and is not to be used for secure hashing. Uses murmur3 hashing for speed and stability.
   *
   * @param obj {@link Data} object to generate hash code for.
   * @return {@link Primitive} String hash code representing the input {@link Data} object.
   */
  @PluginFunction
  public static Primitive hash(RuntimeContext ctx, Data obj) {
    byte[] hashCode = hashcode(obj);
    return ctx.getDataTypeImplementation()
        .primitiveOf(BaseEncoding.base16().lowerCase().encode(hashCode));
  }

  /**
   * Generates an Integer hash from the given {@link Data} object. Key order is not considered for
   * {@link Container}s, ({@link Array} item order is). This is not cryptographically secure and is
   * not to be used for secure hashing. Uses murmur3 hashing for speed and stability.
   *
   * @param obj {@link Data} object to generate hash code for.
   * @return {@link Primitive} holding Integer hash code representing the input {@link Data} object.
   */
  @PluginFunction
  public static Primitive intHash(RuntimeContext ctx, Data obj) {
    return intHash(ctx.getDataTypeImplementation(), obj);
  }

  public static Primitive intHash(DataTypeImplementation dti, Data obj) {
    byte[] hashCode = hashcode(obj);
    HashFunction hashFunc = Hashing.murmur3_32();
    Hasher h = hashFunc.newHasher();
    h.putBytes(hashCode);
    return dti.primitiveOf((double) h.hash().asInt());
  }

  /**
   * Invokes the function provided in the {@code functionName} argument and passes the {@code Data}
   * arguments to the specified function. The function provided in the {@code functionName} argument
   * must be in the same package as the code that calls `callFn`.
   * 
   * This function uses reflection, which lets a program inspect, analyze, and modify its own
   * structure and behavior at runtime.
   *
   * <pre><code>
   * // A simple function that takes no arguments.
   * def packageFunction() {
   *   10
   * }
   * // Pass the simple function to the callFn function.
   * output: callFn("packageFunction")
   * // The result of the function call:
   * // {
   * //   output: 10
   * // }
   * </code></pre>
   *
   * @param context {@link RuntimeContext} where to execute this function
   * @param functionName the name of the function to call
   * @param args the arguments to pass to the function specified in {@code functionName}
   * @return the result of the function call
   */
  @PluginFunction
  public static Data callFn(RuntimeContext context, String functionName, Data... args) {
    return DefaultClosure.create(new FunctionReference(functionName), args).execute(context);
  }

  /**
   * Invokes the function provided in the {@code functionName} argument from the package provided in
   * the {@code packageName} argument. Passes the {@code Data} arguments to the specified function.
   *
   * <p><b>The following examples must be run from separate Whistle files. You can't define multiple
   * package names in a single file.</b>
   *
   * <p><code>my_file_1.wstl</code>:
   *
   * <pre><code>
   * // Declare the "foo" package.
   * package "foo"
   * // A simple function with no arguments.
   * def otherPackageFunction() {
   *   10
   * }
   * </code></pre>
   *
   * <code>my_file_2.wstl</code>:
   *
   * <pre><code>
   * // Declare the "bar" package.
   * package "bar"
   * // Import the file containing the "foo" package. In this example, the files are in the same
   * // directory.
   * import "./my_file_1.wstl"
   *
   * // Pass the `otherPackageFunction()` function from the "foo" package to a variable in this
   * // package, and call `callPackageFn()` on `otherPackageFunction()`.
   * output: callPackageFn("foo", "otherPackageFunction")
   * // The result of the function call:
   * // {
   * //   output: 10
   * // }
   * </code></pre>
   *
   * @param context {@link RuntimeContext} where to execute this function
   * @param packageName the package where the function specified in {@code functionName} resides
   * @param functionName the name of the function to call
   * @param args the arguments to pass to the function specified in {@code functionName}
   * @return the result of the function call
   */
  @PluginFunction
  public static Data callPackageFn(
      RuntimeContext context, String packageName, String functionName, Data... args) {
    return DefaultClosure.create(new FunctionReference(packageName, functionName), args)
        .execute(context);
  }

  /**
   * Executes the given expression, merging its output with any <code>side</code> outputs that are
   * written within (including by other functions/expressions called by this one).
   *
   * @param body the expression from which to capture and merge side outputs.
   * @return merged side and main outputs from the given expression.
   */
  @PluginFunction
  public static Data withSides(RuntimeContext context, Closure body) {
    // TODO(): Prevent cross-package access.
    // TODO(): Allow for writing to side catchers across dataset iteration.

    // This stack contains side output values.
    @Var Deque<Data> sideCatchers = context.getMetaData().getMeta(SideTarget.SIDES_STACK_META_KEY);
    if (sideCatchers == null) {
      sideCatchers = new ArrayDeque<>();
    }
    sideCatchers.push(NullData.instance);
    context.getMetaData().setMeta(SideTarget.SIDES_STACK_META_KEY, sideCatchers);

    Data bodyOutput = body.execute(context);

    Data sideOutput = sideCatchers.pop();

    // TODO(): Fail if the types are not mergeable.
    return bodyOutput.merge(sideOutput, context.getDataTypeImplementation());
  }

  /**
   * Generates a hash from the input {@link Data} object. Key order is not considered for {@link
   * Container}s, ({@link Array} item order is). This is not cryptographically secure and is not to
   * be used for secure hashing.
   *
   * @param obj {@link Data} object to generate hash code for.
   * @return byte array hash code representing the input Data object.
   */
  public static byte[] hashcode(Data obj) {
    HashFunction hashFunc = Hashing.murmur3_128();
    Hasher h = hashFunc.newHasher();
    hashObj(obj, h);
    return h.hash().asBytes();
  }

  /**
   * Adds hash for the given {@link Data} object to the specified {@link Hasher}.
   *
   * @param obj {@link Data} object to generate hash code for.
   * @param h {@link Hasher} object to update with input Data object hash.
   */
  private static void hashObj(Data obj, Hasher h) {
    if (obj.isNullOrEmpty()) {
      hashBytes(NIL.getBytes(UTF_8), new byte[] {}, h);
    } else if (obj.isPrimitive()) {
      Primitive primitiveObj = obj.asPrimitive();
      if (primitiveObj.string() != null) {
        hashBytes(STR.getBytes(UTF_8), primitiveObj.string().getBytes(UTF_8), h);
      } else if (primitiveObj.num() != null) {
        byte[] b = ByteBuffer.wrap(new byte[8]).putDouble(primitiveObj.num()).array();
        hashBytes(NUM.getBytes(UTF_8), b, h);
      } else { // Primitive represents boolean value
        byte[] b = new byte[1];
        b[0] = 1;
        if (primitiveObj.bool()) {
          b[0] = 2;
        }
        hashBytes(BOOL.getBytes(UTF_8), b, h);
      }
    } else if (obj.isArray()) {
      Array arrayObj = obj.asArray();
      for (int i = 0; i < arrayObj.size(); ++i) {
        hashObj(arrayObj.getElement(i), h);
      }
    } else if (obj.isContainer()) {
      Container containerObj = obj.asContainer();
      @Var Map<String, Data> sorted = new HashMap<>();

      for (String field : containerObj.nonNullFields()) {
        sorted.put(field, containerObj.getField(field));
      }
      sorted =
          sorted.entrySet().stream()
              .sorted(comparingByKey())
              .collect(
                  toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (e1 /* merge value old */, e2 /* merge value new */) -> e2 /* keep new */,
                      LinkedHashMap::new));
      sorted.forEach(
          (key, val) -> {
            hashBytes(KEY.getBytes(UTF_8), key.getBytes(UTF_8), h);
            hashObj(val, h);
          });
    } else {
      throw new UnsupportedOperationException("Unsupported type to hash: " + obj.getClass() + ".");
    }
  }

  /**
   * Helper to hash data bytes with given salt bytes and add to {@link Hasher}.
   *
   * @param salt byte array to use to salt the hash.
   * @param data byte array to hash.
   * @param h {@link Hasher} to push salted byte array to.
   */
  private static void hashBytes(byte[] salt, byte[] data, Hasher h) {
    byte[] saltedBytes = Bytes.concat(salt, data);
    h.putBytes(saltedBytes);
  }

  /** An exception thrown from Whistle. */
  public static class UserException extends RuntimeException {
    public UserException(String message) {
      super(message);
    }
  }
}
