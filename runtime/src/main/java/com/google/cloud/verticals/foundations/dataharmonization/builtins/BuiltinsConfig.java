/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import com.google.auto.value.AutoValue;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.IdGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.RandomUUIDGenerator;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.time.Clock;
import java.util.Set;

/** Configuration options for the default Whistle builtin functions. */
@AutoValue
public abstract class BuiltinsConfig implements Serializable {
  /** Whether the filesystem access is enabled/allowed. True by default. */
  public abstract boolean allowFsFuncs();

  /**
   * Whether any syntax issues in Whistle files being parsed by the Whistle Parser should throw a
   * TranspilationException when the file is being parsed and transpiled. True by default.
   */
  public abstract boolean throwWhistleParserTranspilationException();

  /** The Clock implementation to use. {@link Clock#systemUTC()} by default. */
  public abstract Clock clock();

  /**
   * IdGenerator implementation to use for GUID functions. {@link RandomUUIDGenerator} by default.
   */
  public abstract IdGenerator<String> idGenerator();

  /**
   * Sets an allowlist of class names of plugins that can be imported (through the {@link
   * com.google.cloud.verticals.foundations.dataharmonization.imports.impl.PluginClassParser}. If
   * this set is empty, any plugin classes on the classpath are allowed.
   */
  public abstract ImmutableSet<String> importablePluginAllowlist();

  public static Builder builder() {
    return new AutoValue_BuiltinsConfig.Builder()
        .setAllowFsFuncs(true)
        .setThrowWhistleParserTranspilationException(true)
        .setClock(Clock.systemUTC())
        .setImportablePluginAllowlist(ImmutableSet.of())
        .setIdGenerator(new RandomUUIDGenerator());
  }

  /** Builder for BuiltinsConfig. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAllowFsFuncs(boolean value);

    public abstract Builder setThrowWhistleParserTranspilationException(boolean value);

    public abstract Builder setClock(Clock value);

    public abstract Builder setIdGenerator(IdGenerator<String> value);

    public abstract Builder setImportablePluginAllowlist(Set<String> value);

    public abstract BuiltinsConfig build();
  }
}
