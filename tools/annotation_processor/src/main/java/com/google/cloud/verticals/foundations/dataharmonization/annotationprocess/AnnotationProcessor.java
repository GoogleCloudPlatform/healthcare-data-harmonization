/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.annotationprocess;

import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/** Annotation processor for the PluginFunction annotation. */
@SupportedAnnotationTypes({
  "com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

  private PluginFunctionDocStringProcessor pluginFunctionDocStringProcessor;
  private PluginFunctionSignatureProcessor pluginFunctionSignatureProcessor;
  private static String PLUGIN_FUNCTION_ANNOTATION = "PluginFunction";

  @Override
  public void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    pluginFunctionDocStringProcessor = new PluginFunctionDocStringProcessor(processingEnv);
    pluginFunctionSignatureProcessor = new PluginFunctionSignatureProcessor(processingEnv);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    if (!roundEnv.errorRaised() && !roundEnv.processingOver()) {
      boolean docStringsProcessed = false;
      boolean functionSignatureProcessed = false;
      for (TypeElement annotation : annotations) {
        if (annotation.getSimpleName().toString().equals(PLUGIN_FUNCTION_ANNOTATION)) {
          Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
          docStringsProcessed =
              pluginFunctionDocStringProcessor.processAnnotation(annotatedElements);
          functionSignatureProcessed =
              pluginFunctionSignatureProcessor.processAnnotation(annotatedElements);
        }
      }
      return docStringsProcessed && functionSignatureProcessed;
    }
    return false;
  }
}
