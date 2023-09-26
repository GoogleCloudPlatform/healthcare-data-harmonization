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

package com.google.cloud.verticals.foundations.dataharmonization.exceptions;

import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashSet;
import java.util.Set;

/**
 * A RuntimeException occurring in Whistle Code. This class' stack trace will be filled with Whistle
 * code references, rather than Java code references.
 */
public class WhistleRuntimeException extends RuntimeException {
  /**
   * The name used in palce of the file for native stack frames (we don't want to burden the user
   * with the real java file name in the Whistle stacks).
   */
  public static final String NATIVE_FILE_NAME = "Native";

  private static final int MAX_STACK_TRACE_ELEMS = 1000;

  private static final ImmutableSet<FunctionType> OMITTED_FUNCTION_TYPES =
      ImmutableSet.of(FunctionType.BLOCK, FunctionType.IMPLICIT);
  private static final ImmutableSet<FunctionType> OMITTED_CALLSITE_SOURCES =
      ImmutableSet.of(FunctionType.NATIVE);
  private static final String NO_DEBUG_INFO = "<no debug info available>";

  private final StackFrame stackFrame;

  private StackTraceElement[] stackTrace;

  /**
   * Creates a new WhistleRuntimeException. This will (deep) copy the given stackframe to produce a
   * stack trace based upon the whistle code.
   *
   * @param stackFrame The stack frame at which the exception occurred.
   * @param cause The original cause of the exception.
   * @param hideExceptions True to remove the exception message. Useful if message may be logged but
   *     may also contain sensitive data.
   */
  protected WhistleRuntimeException(
      StackFrame stackFrame, Throwable cause, boolean hideExceptions) {
    this(
        stackFrame,
        hideExceptions && !(cause instanceof HiddenException) ? new HiddenException(cause) : cause);
  }

  /**
   * Creates a new WhistleRuntimeException. This will (deep) copy the given stackframe to produce a
   * stack trace based upon the whistle code.
   *
   * @param stackFrame The stack frame at which the exception occurred.
   * @param cause The original cause of the exception.
   */
  private WhistleRuntimeException(StackFrame stackFrame, Throwable cause) {
    super(concatMessage(cause), cause);
    if (!(cause instanceof StackOverflowError)) {
      this.stackFrame = stackFrame.stackCopy();
      this.fillInStackTrace();
    } else {
      this.stackFrame = stackFrame;
      this.stackTrace = cause.getStackTrace();
    }
  }

  private static String concatMessage(Throwable cause) {
    StringBuilder message = new StringBuilder();
    Set<String> messages = new HashSet<>();
    while (cause != null) {
      if (messages.add(cause.getMessage())) {
        message.append(cause.getClass().getSimpleName());
        message.append(": ");
        message.append(cause.getMessage());
        message.append('\n');
      }
      cause = cause.getCause();
    }

    return message.toString().stripTrailing();
  }

  public static WhistleRuntimeException fromCurrentContext(
      RuntimeContext context, Throwable cause) {
    if (cause instanceof WhistleRuntimeException) {
      return (WhistleRuntimeException) cause;
    }

    return new WhistleRuntimeException(
        context.top(), cause, Engine.isNoDataInExceptionsSet(context));
  }

  /**
   * Generate a full list of stack frames for presentation to a user, starting at the given frame
   * and traversing its parents.
   */
  public static ImmutableList<StackTraceElement> generateStackTrace(StackFrame stackFrame) {
    StackFrame cursor = stackFrame;
    ImmutableList.Builder<StackTraceElement> elements = ImmutableList.builder();
    Source callSiteToLastFn =
        cursor != null && cursor.getDebugInfo() != null
            ? cursor.getDebugInfo().getCallSiteToNextStackFrame()
            : DebugInfo.UNKNOWN_SOURCE;

    // Go down the stack, looking for frames that the user should care about (i.e. skipping ones in
    // OMITTED_FUNCTION_TYPES. Each frame should (might) have a callsite info indicating where
    // it transitioned-into (called) the frame above it. We retain that information until the next
    // frame that we care about. For example, assuming we omit blocks, and have a stack like
    //
    // Frame | callsite to next
    // func1     n/a
    //   ↑
    // blockA    x.wstl:9
    //   ↑
    // funcA     x.wstl:4 - funcA calls blockA, we don't care about that.
    // We want a stack like:
    // at x.func1
    // at x.funcA(x.wstl:9)
    int steps = 0;
    while (cursor != null && steps < MAX_STACK_TRACE_ELEMS) {
      DebugInfo info = cursor.getDebugInfo();

      StackTraceElement element;
      boolean skip;

      if (info == null) {
        skip = cursor.inheritsParentVars();
        element = new StackTraceElement(NO_DEBUG_INFO, cursor.getName(), "", -1);
      } else {
        skip = OMITTED_FUNCTION_TYPES.contains(info.getFunctionInfo().getType());
        element = formatElement(info, cursor, callSiteToLastFn);
      }

      if (!skip) {
        elements.add(element);

        // We only want to get the callsite that lead to this element if it's from a frame that
        // actually has that information (i.e. not something present in OMITTED_CALLSITE_SOURCES).
        boolean wasCalled = cursor.getParent() != null;
        boolean callerHadDebugInfo = wasCalled && cursor.getParent().getDebugInfo() != null;
        boolean callerWasNotNative =
            callerHadDebugInfo
                && !OMITTED_CALLSITE_SOURCES.contains(
                    cursor.getParent().getDebugInfo().getFunctionInfo().getType());

        callSiteToLastFn =
            callerWasNotNative
                ? cursor.getParent().getDebugInfo().getCallSiteToNextStackFrame()
                : DebugInfo.UNKNOWN_SOURCE;
      }

      // Steps count guards against weird scenarios of cycles or infinite loops when stack is mocked
      // in tests.
      steps++;
      cursor = cursor.getParent();
    }

    return elements.build();
  }

  private static StackTraceElement formatElement(
      DebugInfo info, StackFrame cursor, Source callSiteToLastFn) {
    switch (info.getFunctionInfo().getType()) {
      case NATIVE:
        return new StackTraceElement(
            info.getCurrentFile().getUrl(),
            cursor.getName(),
            NATIVE_FILE_NAME,
            callSiteToLastFn.getStart().getLine());
      case LAMBDA:
        return new StackTraceElement(
            info.getPackage(),
            String.format("<lambda on line %d>", info.getCurrentSource().getStart().getLine()),
            info.getCurrentFile().getUrl(),
            callSiteToLastFn.getStart().getLine());
      default:
        return new StackTraceElement(
            info.getPackage(),
            cursor.getName(),
            info.getCurrentFile().getUrl(),
            callSiteToLastFn.getStart().getLine());
    }
  }

  @CanIgnoreReturnValue
  @Override
  public synchronized Throwable fillInStackTrace() {
    if (stackTrace == null || stackTrace.length == 0) {
      stackTrace = generateStackTrace(stackFrame).toArray(StackTraceElement[]::new);
    }
    super.setStackTrace(stackTrace);
    return this;
  }

  @Override
  public StackTraceElement[] getStackTrace() {
    return stackTrace;
  }

  @Override
  public void setStackTrace(StackTraceElement[] stackTrace) {
    // TODO() - this is still causing issues
    // throw new UnsupportedOperationException(
    //    "StackTrace should not be set externally on WhistleRuntimeException");
  }

  /**
   * Returns a deep copy of the stackframe at which this exception occurred. This method does not
   * perform the copy, the copy was done when this exception was created.
   */
  public StackFrame getStackFrame() {
    return stackFrame;
  }
}
