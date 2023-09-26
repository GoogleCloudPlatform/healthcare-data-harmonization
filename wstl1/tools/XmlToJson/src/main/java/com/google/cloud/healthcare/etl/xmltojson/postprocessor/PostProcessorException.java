// Copyright 2020 Google LLC.
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

package com.google.cloud.healthcare.etl.xmltojson.postprocessor;

/** Exception to capture errors when post processing an XML to JSON. */
public class PostProcessorException extends Exception{
  /**
   * PostProcessorException constructor with error message.
   *
   * @param errorMessage description of error
   */
  public PostProcessorException(String errorMessage) {
    super(errorMessage);
  }

  /**
   * PostProcessorException constructor with error message and exception to wrap.
   *
   * @param errorMessage description of error
   * @param e exception to be wrapped
   */
  public PostProcessorException(String errorMessage, Exception e) {
    super(errorMessage, e);
  }
}
