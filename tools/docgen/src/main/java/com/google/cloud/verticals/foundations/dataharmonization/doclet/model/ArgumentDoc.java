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

/** Represents the documentation of a single plugin function argument. */
@AutoValue
public abstract class ArgumentDoc {
  public abstract String body();

  public abstract String name();

  public abstract String type();

  public static Builder builder() {
    return new AutoValue_ArgumentDoc.Builder().setBody("").setType("???");
  }

  /** Builder for ArgumentDoc. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String value);

    public abstract Builder setType(String value);

    public abstract Builder setBody(String value);

    public abstract ArgumentDoc build();
  }
}
