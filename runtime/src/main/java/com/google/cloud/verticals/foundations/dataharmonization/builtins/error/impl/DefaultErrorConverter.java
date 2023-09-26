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

package com.google.cloud.verticals.foundations.dataharmonization.builtins.error.impl;

import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.ErrorConverter;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.common.collect.ImmutableMap;
import java.util.stream.Collectors;

/**
 * Creates a Data representation of the given exception, including message, stack trace, and vars.
 * This
 *
 * <p>The output looks like:
 *
 * <pre><code>
 *   {
 *     "cause": "Some string explaining what went wrong",
 *     "stack": [{ // Stack trace of where the error occurred. The top most stack frame is where
 *                 // it occurred.
 *         "package": "Whistle or Java package name",
 *         "file": "file://the/original/file/path.wstl",
 *         "function": "myFunctionName",
 *         "line": 99 // Line in the file where the error occurs
 *        },
 *        {
 *         "package": "Whistle or Java package name",
 *         "file": "file://the/original/file/path.wstl",
 *         "function": "myOtherFunctionName",
 *         "line": 33 // The line where myFunctionName in the stack frame above is called.
 *        }, ...],
 *     "vars": {
 *       "x": ..., // value of x
 *       "y": ... // value of y
 *     }
 *   }
 * </code></pre>
 */
public class DefaultErrorConverter implements ErrorConverter {
  @Override
  public Data convert(RuntimeContext ctx, Throwable ex) {
    DataTypeImplementation dti = ctx.getDataTypeImplementation();
    Data hiddenVars =
        dti.containerOf(
            ImmutableMap.of(
                "hidden",
                dti.primitiveOf(
                    "Variables are hidden to prevent data leaking through error messages."
                        + " This is controlled through a flag upon engine"
                        + " initialization.")));

    return dti.containerOf(
        ImmutableMap.<String, Data>builder()
            .put("cause", ctx.getDataTypeImplementation().primitiveOf(ex.getMessage()))
            .put(
                "stack",
                dti.arrayOf(
                    stream(ex.getStackTrace())
                        .map(e -> stackToData(e, dti))
                        .collect(Collectors.toList())))
            .put(
                "vars",
                Engine.isNoDataInExceptionsSet(ctx)
                    ? hiddenVars
                    : localVars(
                        ctx.getDataTypeImplementation(),
                        ex instanceof WhistleRuntimeException
                            ? ((WhistleRuntimeException) ex).getStackFrame()
                            : ctx.top()))
            .buildOrThrow());
  }

  /**
   * Creates a map of variables in the top stackframe (and parents, down to the next non-inherited
   * stack frame).
   */
  private static Data localVars(DataTypeImplementation dti, StackFrame top) {
    ImmutableMap.Builder<String, Data> builder = ImmutableMap.<String, Data>builder();

    StackFrame current = top;
    while (current.getDebugInfo() != null
        && current.getDebugInfo().getFunctionInfo().getType() == FunctionType.NATIVE) {
      current = current.getParent();
    }

    addStackFrameVars(current, builder);
    while (current.inheritsParentVars()) {
      current = current.getParent();
      addStackFrameVars(current, builder);
    }

    return dti.containerOf(builder.buildOrThrow());
  }

  /** Adds the variables in this stackframe to the given maps builder. */
  private static void addStackFrameVars(
      StackFrame frame, ImmutableMap.Builder<String, Data> builder) {
    frame
        .getVars()
        .forEach(
            v -> {
              String name = v;
              if (WhistleFunction.OUTPUT_VAR.equals(v)) {
                StackFrame mainFrame = frame;
                while (mainFrame.inheritsParentVars()) {
                  mainFrame = mainFrame.getParent();
                }
                name = mainFrame.getName() + "." + v;
              }
              builder.put(name, frame.getVar(v));
            });
  }

  /** "Pretty prints" a StackTraceElement as structured Data. */
  private static Data stackToData(StackTraceElement elem, DataTypeImplementation dti) {
    return dti.containerOf(
        ImmutableMap.<String, Data>builder()
            .put("package", dti.primitiveOf(elem.getClassName()))
            .put("file", dti.primitiveOf(elem.getFileName()))
            .put("function", dti.primitiveOf(elem.getMethodName()))
            .put("line", dti.primitiveOf(Double.valueOf(elem.getLineNumber())))
            .buildOrThrow());
  }
}
