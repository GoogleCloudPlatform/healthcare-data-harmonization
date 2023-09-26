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

package com.google.cloud.verticals.foundations.dataharmonization.imports;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** Specifies the (absolute) path to an imported file, as well as some context for it. */
public final class ImportPath implements Serializable {
  // TODO(): Don't use default FS.
  private static FileSystem fileSystem = FileSystems.getDefault();

  private static final Pattern SCHEME_EXTRACTOR = Pattern.compile("^([^:]+):(//)?");
  private Path absolutePath;
  private String loader;
  private Path importsRoot;

  private ImportPath(String loader, Path absolutePath, Path importsRoot) {
    this.absolutePath = absolutePath;
    this.loader = loader;
    this.importsRoot = importsRoot;
  }

  /**
   * Initialize an ImportPath directly from the given parameters.
   *
   * @param loader The {@link Loader} to use when loading the file.
   * @param file The absolute path to a file.
   * @param importsRoot The root to resolve project-relative imports against.
   */
  public static ImportPath of(String loader, Path file, Path importsRoot) {
    return new ImportPath(loader, file, importsRoot);
  }

  /**
   * Resolve the given import string (which may be relative or absolute) against the given
   * ImportPath.
   *
   * <p>This method attempts to resolve the given file path relative to some directory. If the given
   * path is absolute, then this method simply wraps it in an ImportPath, copying the project
   * directory from current, and the loader if not specified.
   *
   * <p>A relative path can be resolved relative to one of two directories:
   *
   * <ol>
   *   <li>An imports root, specified as the parent directory of the main file's path
   *   <li>The directory of the current file that was imported (i.e. the file that contained the
   *       import for the next path).
   * </ol>
   *
   * <p>For example, given a <code>current</code> (see method params) file located at /proj/y/z.wstl
   * (with a imports root /proj, acceptable formats and their semantics for <code>next
   * </code> import path follow these examples:
   *
   * <ul>
   *   <li>someloader://some/path/here or someloader:///some/path/here - Absolute ImportPath - it
   *       points to /some/path/here, with someloader as the loader, and its imports root will be
   *       set to the same as <code>current</code>'s imports root.
   *   <li>/some/path/here - Absolute ImportPath, using the loader from <current>
   *   <li>some/path/here - ImportPath relative to <code>current</code>'s imports root - it points
   *       to /proj/some/path/here (case 1 above).
   *   <li>./some/path/here - ImportPath relative to <code>current</code> - it points to
   *       /proj/y/some/path/here, with same imports root and loader as <code>current</code> (case 2
   *       above).
   * </ul>
   *
   * @param current The ImportPath of the current file imported. That is, the file that triggered
   *     the import of next.
   * @param next The relative or absolute import string of the file being resolved.
   */
  public static ImportPath resolve(ImportPath current, String next) {
    Optional<String> loader = getLoader(next);
    String filePath = SCHEME_EXTRACTOR.matcher(next).replaceFirst("");

    Path relativeTo;
    if (filePath.startsWith("./") || filePath.startsWith("../")) {
      relativeTo = current.dir();
    } else if (!current.importsRoot.toString().isEmpty() || loader.isPresent()) {
      relativeTo = current.importsRoot;
    } else {
      throw new IllegalStateException("root relative import is disabled without imports root");
    }

    // If a loader is present normalize the path to be absolute.
    if (loader.isPresent()
        && !filePath.startsWith("/")
        && !filePath.startsWith("./")
        && !filePath.startsWith("../")) {
      filePath = "/" + filePath;
    }

    Path path = fileSystem.getPath(filePath);

    // If this import is a relative path, resolve it. If path is absolute, .resolve is noop.
    path = relativeTo.resolve(path).normalize();
    return new ImportPath(loader.orElse(current.loader), path, current.importsRoot);
  }

  /** Extract the loader (if it is specified) from a path like loader://path. */
  public static Optional<String> getLoader(String path) {
    Matcher matcher = SCHEME_EXTRACTOR.matcher(path);
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    }
    return Optional.empty();
  }

  /** The loader for this ImportPath (maybe inherited from a relative parent). */
  public String getLoader() {
    return loader;
  }

  /** The directory of the file specified by {@link #getAbsPath()}. */
  public Path dir() {
    return absolutePath.getParent();
  }

  /** The absolute path of the file this ImportPath refers to. */
  public Path getAbsPath() {
    return absolutePath;
  }

  /** The name of the file specified by {@link #getAbsPath()} (including any extensions). */
  public String getFileName() {
    return getAbsPath().getFileName().toString();
  }

  @Override
  public String toString() {
    return String.format("%s://%s", loader, absolutePath);
  }

  public Path getImportsRoot() {
    return importsRoot;
  }

  /**
   * Non-generated override of equals method to provide a logical equivalence check for ImportPath.
   *
   * @param object The object on which to execute the comparison
   * @return true if equal, otherwise false
   */
  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof ImportPath)) {
      return false;
    }
    ImportPath that = (ImportPath) object;
    return Objects.equals(this.getAbsPath(), that.getAbsPath())
        && Objects.equals(this.getImportsRoot(), that.getImportsRoot())
        && Objects.equals(this.getLoader(), that.getLoader());
  }

  /**
   * Non-generated Override of hashCode on the class to provide the basis for testing for logical
   * equivalence.
   *
   * @return int hash value of the class.
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getAbsPath(), this.getImportsRoot(), this.getLoader());
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    // Serialization
    oos.writeUTF(this.loader);
    oos.writeUTF(this.absolutePath.toString());
    oos.writeUTF(this.importsRoot.toString());
  }

  private void readObject(ObjectInputStream ois) throws IOException {
    // Deserialization
    this.loader = ois.readUTF();
    this.absolutePath = FileSystems.getDefault().getPath(ois.readUTF());
    this.importsRoot = FileSystems.getDefault().getPath(ois.readUTF());
  }

  public FileInfo toFileInfo() {
    return FileInfo.newBuilder().setUrl(toString()).build();
  }

  @VisibleForTesting
  public static void setFs(FileSystem fs) {
    fileSystem = fs;
  }
}
