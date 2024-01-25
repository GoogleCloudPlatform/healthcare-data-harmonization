// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.doclet.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/** Represents the documentation of a single plugin function. */
@AutoValue
public abstract class FunctionDoc {
  public abstract String body();

  public abstract String name();

  public abstract ImmutableList<ArgumentDoc> arguments();

  public abstract ImmutableList<ReturnDoc> thrownExceptions();

  public abstract ReturnDoc returns();

  public static Builder builder() {
    return new AutoValue_FunctionDoc.Builder();
  }

  /** Builder for FunctionDoc. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setName(String value);

    public abstract Builder setReturns(ReturnDoc value);

    public abstract Builder setBody(String value);

    protected abstract ImmutableList.Builder<ArgumentDoc> argumentsBuilder();

    protected abstract ImmutableList.Builder<ReturnDoc> thrownExceptionsBuilder();

    public final Builder addArgument(ArgumentDoc doc) {
      argumentsBuilder().add(doc);
      return this;
    }

    public final Builder addThrows(ReturnDoc doc) {
      thrownExceptionsBuilder().add(doc);
      return this;
    }

    public abstract FunctionDoc build();
  }
}
