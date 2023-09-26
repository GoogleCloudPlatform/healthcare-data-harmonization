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

package com.google.cloud.verticals.foundations.dataharmonization.builtins.error;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.impl.DefaultErrorConverter;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.NativeUnaryClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Builtins and utility functions for handling errors. */
public final class Errors {
  private Errors() {}

  private static final String WITH_ERROR_RETHROWN = "rethrowError";
  private static final String ORIGINAL_EXCEPTION = "originalException";

  /**
   * withError executes the code given in {@code body}. If any errors/exceptions occur, the code in
   * {@code errorHandler} is called, with {@code $error} representing information about the error
   * (structure below).
   *
   * <p>{@code $error} looks like:
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
   *
   * @param body The code to handle errors from.
   * @param errorHandler The code to handle errors with.
   * @return the result value of either the body, or the error handler if it was called.
   */
  @PluginFunction
  public static Data withError(RuntimeContext ctx, Closure body, Closure errorHandler) {
    ErrorHandlingContext ehc =
        WrapperContext.getWrapper(ctx, ErrorHandlingContext.class, c -> true);
    if (ehc != null) {
      ctx = ErrorHandlingContext.fromParent(ehc, errorHandler, new DefaultErrorConverter());
    } else {
      ctx = ErrorHandlingContext.fromContext(ctx, errorHandler, new DefaultErrorConverter());
    }

    return body.execute(ctx);
  }

  /**
   * rethrowError calls {@link #withError} to execute the code given in {@code body}. The behavior
   * is the same as withError, but if there is an exception, it is rethrown after handling. An error
   * rethrown by this method will not be handled again when caught by any other withError or
   * rethrowError handlers.
   *
   * @param body The code to handle errors from.
   * @param errorHandler The code to handle errors with.
   * @return the result value of either the body, or the error handler if it was called.
   */
  @PluginFunction
  public static Data rethrowError(RuntimeContext ctx, Closure body, Closure errorHandler) {
    ErrorHandlingContext ehc =
        WrapperContext.getWrapper(ctx, ErrorHandlingContext.class, c -> true);
    StackFrame emptyTop = DefaultStackFrame.EMPTY_FRAME;
    if (ehc != null) {
      // rethrowing to top of parent with handled BubbleUpErrorException, which will be ignored
      // unless the parent is also a rethrowError, in which case we want to chain into
      // the next level of handleAndRethrow
      StackFrame parentTop = ehc.trap.top();
      StackFrame top = parentTop.getName().equals(WITH_ERROR_RETHROWN) ? emptyTop : parentTop;
      ctx =
          ErrorHandlingContext.fromParent(
              ehc, handleAndRethrow(top, errorHandler), new DefaultErrorConverter());
      return body.execute(ctx);
    } else {
      // rethrowing without top, so it escapes the body and doesn't continue after handling
      ctx =
          ErrorHandlingContext.fromContext(
              ctx, handleAndRethrow(emptyTop, errorHandler), new DefaultErrorConverter());
      // If there's an exception here, there is no upper withError to return results to;
      // We want to throw an unhandled WhistleRuntimeException with original cause
      try {
        return body.execute(ctx);
      } catch (BubbleUpErrorException e) {
        throw e.originalCause;
      }
    }
  }

  /**
   * Helper function to wrap an error handler in a closure which will execute the handler, then
   * throw a new BubbleUpErrorException to bubble up to the specified StackFrame.
   */
  private static Closure handleAndRethrow(StackFrame top, Closure errorHandler) {
    return new NativeUnaryClosure(
        (innerCtx, error) -> {
          Data result = errorHandler.bindNextFreeParameter(error).execute(innerCtx);
          WhistleRuntimeException originalException =
              innerCtx.getMetaData().getMeta(ORIGINAL_EXCEPTION);
          throw new BubbleUpErrorException(top, result, originalException);
        });
  }

  /**
   * Error handling is done by having a runtime context Wrapper, which catches exceptions. Rather
   * than letting the exceptions go down the stack, unwinding it, this error handling strategy
   * handles the exception immediately, then unwinds the stack. This allows us, for example, to have
   * an error handler around a dataset iteration, be called with every row that generates an
   * exception.
   */
  private static class ErrorHandlingContext extends WrapperContext<ErrorHandlingContext> {
    private final RuntimeContext trap;
    private final RuntimeContext innerContext;
    private final ErrorHandlingContext parent;
    private final Closure errorHandler;
    private final ErrorConverter converter;

    private ErrorHandlingContext(
        RuntimeContext innerContext,
        RuntimeContext trap,
        ErrorHandlingContext parent,
        Closure errorHandler,
        ErrorConverter converter) {
      super(innerContext, ErrorHandlingContext.class);
      this.trap = trap;
      this.innerContext = innerContext;
      this.parent = parent;
      this.errorHandler = errorHandler;
      this.converter = converter;
    }

    private static ErrorHandlingContext fromContext(
        RuntimeContext context, Closure handler, ErrorConverter converter) {
      return new ErrorHandlingContext(context, context, null, handler, converter);
    }

    private static ErrorHandlingContext fromParent(
        ErrorHandlingContext parent, Closure handler, ErrorConverter converter) {
      return new ErrorHandlingContext(
          parent.innerContext, parent.innerContext, parent, handler, converter);
    }

    @Override
    protected ErrorHandlingContext rewrap(RuntimeContext innerContext) {
      return new ErrorHandlingContext(innerContext, trap, parent, errorHandler, converter);
    }

    @Override
    public Data wrap(
        CallableFunction function, Data[] args, BiFunction<RuntimeContext, Data[], Data> delegate) {
      return tryDo(() -> super.wrap(function, args, delegate));
    }

    @Override
    public Data evaluate(ValueSource valueSource) {
      return tryDo(() -> super.evaluate(valueSource));
    }

    private Data tryDo(Supplier<Data> op) {
      try {
        return op.get();
      } catch (BubbleUpErrorException ex) {
        // If this is the end of the bubble up, just return the handled result.
        if (ex.target.equals(top())) {
          return ex.handleResult;
        }

        // Rethrow otherwise to keep bubbling up.
        throw ex;
      } catch (RuntimeException ex) {
        // If this is where we are meant to handle it, then handle and return result.
        if (trap.top().equals(top())) {
          return handle(ex, converter);
        }

        // Otherwise we need to start bubbling up to the intended handler.
        throw new BubbleUpErrorException(trap.top(), handle(ex, converter), null);
      }
    }

    private Data handle(Throwable ex, ErrorConverter converter) {
      if (!(ex instanceof WhistleRuntimeException)) {
        ex = WhistleRuntimeException.fromCurrentContext(innerContext, ex);
      }
      Data exData = converter.convert(innerContext, ex);
      innerContext.getMetaData().setMeta(ORIGINAL_EXCEPTION, ex);
      Supplier<Data> op = () -> errorHandler.bindNextFreeParameter(exData).execute(trap);
      if (parent != null) {
        return parent.tryDo(op);
      }

      try {
        return op.get();
      } catch (BubbleUpErrorException handled) {
        throw handled;
      } catch (RuntimeException nestedEx) {
        throw new BubbleUpErrorException(bottom(), nestedEx, Engine.isNoDataInExceptionsSet(this));
      }
    }
  }

  /**
   * This exception is meant to interrupt execution. It will be thrown when some other exception is
   * handled, and caught where the error handler was originally declared.
   */
  public static final class BubbleUpErrorException extends WhistleRuntimeException {
    private final StackFrame target;
    private final Data handleResult;
    private final WhistleRuntimeException originalCause;

    private BubbleUpErrorException(
        StackFrame target, Data handleResult, WhistleRuntimeException originalCause) {
      // Target here is not actually meaningful.
      super(target, null, false);
      this.target = target;
      this.handleResult = handleResult;
      this.originalCause = originalCause;
    }

    private BubbleUpErrorException(StackFrame target, Throwable unhandled, boolean hideExceptions) {
      // Target here is not actually meaningful.
      super(target, unhandled, hideExceptions);
      this.target = target;
      this.handleResult = NullData.instance;
      // Not meaningful
      this.originalCause = null;
    }

    public Data getHandleResult() {
      return handleResult;
    }

    public boolean isHandled() {
      return !handleResult.isNullOrEmpty();
    }
  }

  /**
   * Handles an error if possible, otherwise converts and throws a {@link WhistleRuntimeException}.
   *
   * <p>If an error handler is available (i.e. this is called in the body of {@link #withError}),
   * then that error handler is used to deal with the given error. Otherwise it is wrapped in a
   * {@link WhistleRuntimeException} and thrown immediately.
   *
   * <p>Uses the {@link DefaultErrorConverter} to convert given error to {@link Data}.
   *
   * @param context The current RuntimeContext
   * @param error The error to handle.
   * @return the result of the error handler.
   */
  public static Data rethrowOrHandle(RuntimeContext context, Throwable error) {
    return rethrowOrHandle(context, error, new DefaultErrorConverter());
  }

  /**
   * Handles an error if possible, otherwise converts and throws a {@link WhistleRuntimeException}.
   *
   * <p>If an error handler is available (i.e. this is called in the body of {@link #withError}),
   * then that error handler is used to deal with the given error. Otherwise it is wrapped in a
   * {@link WhistleRuntimeException} and thrown immediately.
   *
   * @param context The current RuntimeContext
   * @param error The error to handle.
   * @param converter The converter to use to convert the error to data.
   * @return the result of the error handler.
   */
  public static Data rethrowOrHandle(
      RuntimeContext context, Throwable error, ErrorConverter converter) {
    ErrorHandlingContext ehc =
        WrapperContext.getWrapper(context, ErrorHandlingContext.class, c -> true);
    if (ehc != null) {
      return ehc.handle(error, converter);
    }

    throw WhistleRuntimeException.fromCurrentContext(context, error);
  }

  public static RuntimeContext ehcWithConverter(RuntimeContext context, ErrorConverter converter) {
    ErrorHandlingContext ehc =
        WrapperContext.getWrapper(context, ErrorHandlingContext.class, c -> true);
    if (ehc == null) {
      return context;
    }
    return ErrorHandlingContext.fromParent(ehc, ehc.errorHandler, converter);
  }
}
