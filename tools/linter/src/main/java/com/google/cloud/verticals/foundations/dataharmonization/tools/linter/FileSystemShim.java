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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter;

import java.io.IOException;
import java.io.Writer;

/**
 * Used for reading and writing to Whistle files. Takes in file paths inside the resources folder,
 * include a forward slash (e.g. "/testFile.wstl" or "/subdirectory/testFile.wstl").
 */
public interface FileSystemShim {

  Writer createWriter(String filePath) throws IOException;

  String readString(String filePath) throws IOException;
}
