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

package com.google.cloud.verticals.foundations.dataharmonization.debug;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.protobuf.Any;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Contains information useful for debugging about a particular state of execution. That is, a
 * DebugInfo contains what is essentially metadata information about a snapshot of the Program
 * Counter (if Whistle had a Program Counter). A DebugInfo should be created when a function is
 * entered and `callSiteToNextStackFrame` should be updated as this function is pushed down the
 * stack.
 */
public final class DebugInfo implements Serializable {
  /** FileInfo representing the lack of any file information. */
  public static final FileInfo UNKNOWN_FILE = FileInfo.newBuilder().setUrl("unknown").build();

  /** SourcePosition representing the lack of any source position information. */
  public static final SourcePosition UNKNOWN_SOURCE_POSITION =
      SourcePosition.newBuilder().setLine(-1).build();

  /** Source representing the lack of any source information. */
  public static final Source UNKNOWN_SOURCE =
      Source.newBuilder().setStart(UNKNOWN_SOURCE_POSITION).setEnd(UNKNOWN_SOURCE_POSITION).build();

  /** FunctionInfo representing the lack of any function information. */
  private static final FunctionInfo UNKNOWN_FUNCTION_INFO = FunctionInfo.getDefaultInstance();

  private final FileInfo currentFile;
  private final Source currentSource;
  private final FunctionInfo functionInfo;
  private final String pkg;
  private Source callSiteToNextStackFrame;

  private DebugInfo(
      FileInfo currentFile, Source currentSource, FunctionInfo functionInfo, String pkg) {
    this.currentFile = currentFile;
    this.currentSource = currentSource;
    this.functionInfo = functionInfo;
    this.pkg = pkg;
    this.callSiteToNextStackFrame = UNKNOWN_SOURCE;
  }

  /** Create a DebugInfo for a Whistle proto-based Function. */
  public static DebugInfo fromFunction(PipelineConfig config, FunctionDefinition def) {
    return new DebugInfo(
        getMetaEntry(config.getMeta(), TranspilerData.FILE_META_KEY, FileInfo.class, UNKNOWN_FILE),
        getMetaEntry(def.getMeta(), TranspilerData.SOURCE_META_KEY, Source.class, UNKNOWN_SOURCE),
        getMetaEntry(
            def.getMeta(),
            TranspilerData.FUNCTION_INFO_META_KEY,
            FunctionInfo.class,
            UNKNOWN_FUNCTION_INFO),
        config.getPackageName());
  }

  /** Create a DebugInfo for a Java reflection-based Function. */
  public static DebugInfo fromJavaFunction(String packageName, Method javaFunction) {
    FileInfo file =
        FileInfo.newBuilder().setUrl(javaFunction.getDeclaringClass().getName()).build();

    FunctionType type = FunctionType.NATIVE;
    if (javaFunction.isAnnotationPresent(PluginFunction.class)
        && javaFunction.getAnnotation(PluginFunction.class).inheritParentVars()) {
      // Native funcs that inherit like this are a very special case, and should not be present in
      // stack traces. As of time of writing only ternary does this (and only ternary should be
      // allowed to do this).
      type = FunctionType.IMPLICIT;
    }

    FunctionInfo info = FunctionInfo.newBuilder().setType(type).build();

    // TODO(): pass packageName into the pkg field of debug info after making sure HDE
    // doesn't have test dependencies on error message formatting related to this.
    return new DebugInfo(
        file, UNKNOWN_SOURCE, info, javaFunction.getDeclaringClass().getPackage().getName());
  }

  /** Create a DebugInfo for an import statement. */
  public static DebugInfo fromImport(PipelineConfig config, Import importMsg) {
    return new DebugInfo(
        getMetaEntry(config.getMeta(), TranspilerData.FILE_META_KEY, FileInfo.class, UNKNOWN_FILE),
        getMetaEntry(
            importMsg.getMeta(), TranspilerData.SOURCE_META_KEY, Source.class, UNKNOWN_SOURCE),
        UNKNOWN_FUNCTION_INFO,
        config.getPackageName());
  }

  /** Create a DebugInfo given basic information manually. */
  public static DebugInfo simpleFunction(String file, FunctionType type) {
    return new DebugInfo(
        FileInfo.newBuilder().setUrl(file).build(),
        UNKNOWN_SOURCE,
        FunctionInfo.newBuilder().setType(type).build(),
        file);
  }

  /** Gets the parser for the Meta entry's proto class */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T extends Message> Parser<T> getMetaEntryParser(Class<T> entryType) {
    Parser parser;
    if (entryType == Source.class) {
      parser = Source.parser();
    } else if (entryType == FileInfo.class) {
      parser = FileInfo.parser();
    } else if (entryType == FunctionInfo.class) {
      parser = FunctionInfo.parser();
    } else {
      throw new IllegalArgumentException(
          String.format("Class %s is not a valid Meta entry type", entryType.getName()));
    }

    return (Parser<T>) parser;
  }

  /**
   * Utility method to pull a specific entry out of the Meta proto.
   *
   * @param meta the Meta proto
   * @param key the key of the Meta value
   * @param entryType the Java class of the Meta value
   * @param defaultValue the default value to return if no entry exists under this key
   * @param <T> the type of the Meta value.
   */
  public static <T extends Message> T getMetaEntry(
      Meta meta, String key, Class<T> entryType, T defaultValue) {
    if (meta == null) {
      return defaultValue;
    }
    Any entry = meta.getEntriesOrDefault(key, Any.pack(defaultValue));
    try {
      // We purposely avoid using `Any.unpack` because it manually calls
      // `java.lang.reflect.Method.invoke()`.
      // This may wind up trying to load method's bytecode to a new class and ask for
      // `createClassLoader` runtime permission which prevents us from running
      // Whistle Engine in sandboxed environments.
      return getMetaEntryParser(entryType)
          .parseFrom(entry.getValue(), ExtensionRegistry.getEmptyRegistry());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          String.format(
              "Meta contained invalid meta entry at %s. Expected a %s, got %s",
              key, FileInfo.getDescriptor().getFullName(), entry.getTypeUrl()),
          e);
    }
  }

  /**
   * Sets the callsite to the next stack frame on this DebugInfo. This is useful for tracking calls
   * along with the context information of the place where the call was made.
   */
  public void setCallsiteToNextStackFrame(FunctionCall call) {
    callSiteToNextStackFrame =
        getMetaEntry(call.getMeta(), TranspilerData.SOURCE_META_KEY, Source.class, currentSource);
  }

  /** Sets the callsite to the current mapping. This helps with tracing callsite of target write. */
  public void setCallsiteToNextStackFrame(FieldMapping mapping) {
    callSiteToNextStackFrame =
        getMetaEntry(
            mapping.getMeta(), TranspilerData.SOURCE_META_KEY, Source.class, currentSource);
  }

  /** The current file represented by this point of execution. */
  public FileInfo getCurrentFile() {
    return currentFile;
  }

  /** A location in the original source code represented by this point of execution. */
  public Source getCurrentSource() {
    return currentSource;
  }

  /** Function-specific information about the function represented by this DebugInfo. */
  public FunctionInfo getFunctionInfo() {
    return functionInfo;
  }

  /**
   * A location of a function call in the original source code that resulted in the stack frame
   * above this one being pushed.
   */
  public Source getCallSiteToNextStackFrame() {
    return callSiteToNextStackFrame;
  }

  /** The package of the function represented by this DebugInfo */
  public String getPackage() {
    return pkg;
  }
}
