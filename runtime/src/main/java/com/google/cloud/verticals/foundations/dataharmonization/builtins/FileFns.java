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
package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Functions for file (and path) management. */
public final class FileFns {
  private static FileSystem fileSystem = FileSystems.getDefault();
  private static final Pattern GLOB_CHAR = Pattern.compile("[*\\[?]");

  private FileFns() {}

  /**
   * Joins the given path segments, then returns the normalized absolute path as a {@code Primitive}
   * {@code string}. Normalization removes relative path segments, such as {@code ./} and {@code
   * ../}. If the path segments are relative, the returned path is relative to the file
   * where the function is called.
   *
   * <pre><code>
   * // Suppose that the file calling the following functions is located at
   * // file:///foo/bar/baz/myfile.wstl. The `baz` directory contains the subdirectories
   * // `one`, `two`, and `three`.
   *
   * // Returns "file:///foo/bar/baz/one/two/three"
   * absPath("./one/two", "three")
   *
   * // Returns "file:///foo/bar/baz/one/two"
   * absPath("./one/two")
   *
   * // Returns "file:///foo/bar/baz/one/three"
   * absPath("../one/two")
   *
   * // Returns "prefix:///newdir/one/two"
   * absPath("prefix:///newdir/one/two")
   * </code></pre>
   *
   * @param first the first path segment
   * @param rest the remaining path segments
   * @return {@link Primitive} {@code string}
   */
  @PluginFunction
  public static Primitive absPath(RuntimeContext context, String first, String... rest) {
    ImportPath currentPath = context.getCurrentPackageContext().getCurrentImportPath();
    String joined = joinPath(context, first, rest).string();

    return context
        .getDataTypeImplementation()
        .primitiveOf(ImportPath.resolve(currentPath, joined).toString());
  }

  /**
   * Joins the given path segments, then returns the normalized path. Normalized means all redundant
   * relative segments such as non-leading {@code ./} and {@code ../} are resolved away.
   *
   * <p>Example:
   *
   * <pre><code>
   * joinPath("./one/../two", "three") == "./two/three"
   * joinPath("../one/../two", "three", "four") == "../two/three/four"
   * joinPath("/one/two", "../three") == "/one/three"
   * joinPath("one/two", "three") == "one/two/three"
   * joinPath("hello:///bucket/one/two", "../three", "four") == "hello:///bucket/one/three/four"
   * </code></pre>
   *
   * @param first The first element of the path.
   * @param rest The remaining elements of the path.
   */
  @PluginFunction
  public static Primitive joinPath(RuntimeContext context, String first, String... rest) {
    Optional<String> scheme = ImportPath.getLoader(first);
    String prefix = "";
    if (scheme.isPresent()) {
      prefix = scheme.get() + "://";
      first = first.replaceFirst("^" + scheme.get() + "://", "");
    }

    // Preserve the initial relativity.
    if (first.startsWith("./")) {
      prefix += "./";
    } else if (first.startsWith("../")) {
      prefix += "../";
      first = first.replaceFirst("^\\.\\./", "");
    }

    return context
        .getDataTypeImplementation()
        .primitiveOf(prefix + Path.of(first, rest).normalize());
  }

  /**
   * Loads the json data at the given path, and returns it as a Data.
   *
   * @param path The path to the resource. Relative paths are resolved relative to the current file.
   *     This path can use the same loaders/schemes as imports (e.x. file:///hello/world or
   *     gs://my-bucket/hello/world if a GCP plugin is imported).
   */
  @PluginFunction
  public static Data loadJson(RuntimeContext context, String path) {
    byte[] data = load(context, path);
    return JsonSerializerDeserializer.jsonToData(data);
  }

  /**
   * Loads the UTF-8 text data at the given path, and returns it as a string primitive.
   *
   * @param path The path to the resource. Relative paths are resolved relative to the current file.
   *     This path can use the same loaders/schemes as imports (e.x. file:///hello/world or
   *     gs://my-bucket/hello/world if a GCP plugin is imported).
   */
  @PluginFunction
  public static Primitive loadText(RuntimeContext context, String path) {
    byte[] data = load(context, path);
    return context.getDataTypeImplementation().primitiveOf(new String(data, UTF_8));
  }

  /**
   * Returns true iff the file at the given path exists (regardless of whether it is empty).
   *
   * @param path The path to the resource. Relative paths are resolved relative to the current file.
   *     This path can use the same loaders/schemes as imports (e.x. file:///hello/world or
   *     gs://my-bucket/hello/world if a GCP plugin is imported).
   */
  @PluginFunction
  public static Primitive fileExists(RuntimeContext context, String path) {
    ImportPath currentPath = context.getCurrentPackageContext().getCurrentImportPath();
    ImportPath nextPath = ImportPath.resolve(currentPath, path);

    return context.getDataTypeImplementation().primitiveOf(Files.exists(nextPath.getAbsPath()));
  }

  /**
   * Lists all files matching the given pattern. Relative paths are resolved against the current
   * file.
   *
   * <p>Example:
   *
   * <pre><code>
   * Given directories:
   * /
   * ├── current.wstl <------ we are in this file.
   * ├── aaa.json
   * ├── one
   * │     ├── bar
   * │     │     ├── fff.json
   * │     │     └── ggg.json
   * │     ├── bbb.zzz.json
   * │     └── foo
   * │         ├── ccc.json
   * │         ├── ddd.json
   * │         └── eee.zzz.json
   * └── two
   *     ├── hhh.zzz.json
   *     └── iii.json
   *
   * listFiles("./*.json") == ["aaa.json"]
   * listFiles("./**&#47;*.json") == ["/one/bar/ggg.json", "/one/foo/ccc.json",
   *           "/one/foo/eee.zzz.json", "/two/iii.json", "/one/bar/fff.json", "/one/bbb.zzz.json",
   *           "/one/foo/ddd.json", "/two/hhh.zzz.json"]
   * listFiles("/**&#47;*.zz?.json") == ["./one/bbb.zzz.json", "./one/foo/eee.zzz.json",
   *                                  "./two/hhh.zzz.json"]
   * </code></pre>
   *
   * @param pattern A file pattern to match against, in glob form. Currently (b/230104706), only
   *     local files are supported. For more info on glob syntax, see
   *     [getPathMatcher("glob")](https://docs.oracle.com/javase/9/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-)
   */
  @PluginFunction
  public static Array listFiles(RuntimeContext context, String pattern) throws IOException {
    // TODO(): Don't use default FS.
    ImportPath currentPath = context.getCurrentPackageContext().getCurrentImportPath();

    // Find a prefix of the pattern without a wild card to use as our base path
    // Given      /x/y/z*            ./x/*          *.json
    // basePath   /x/y               ./x            ''
    Matcher globMatcher = GLOB_CHAR.matcher(pattern);
    int firstWc = globMatcher.find() ? globMatcher.start() : -1;
    String basePath = pattern.substring(0, firstWc >= 0 ? firstWc : pattern.length());
    int firstSlashBeforeWc = basePath.lastIndexOf("/");
    basePath =
        basePath.substring(
            0, firstSlashBeforeWc >= 0 ? (firstSlashBeforeWc + 1) : basePath.length());

    ImportPath nextPath = ImportPath.resolve(currentPath, basePath);

    pattern = nextPath.getAbsPath().resolve(pattern.substring(basePath.length())).toString();
    PathMatcher pm = fileSystem.getPathMatcher("glob:" + pattern);

    List<Primitive> files = new ArrayList<>();
    Files.walkFileTree(
        nextPath.getAbsPath(),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file != null && pm.matches(file)) {
              files.add(
                  context.getDataTypeImplementation().primitiveOf(file.normalize().toString()));
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return context.getDataTypeImplementation().arrayOf(files);
  }

  /**
   * Returns the file/dir name specified by the given path (including extension). This simply
   * returns the last path segment after the last /. If there are query parameters or other suffixes
   * after this segment, they are included.
   *
   * <p>For example:
   *
   * <pre><code>
   * fileName("hello") == "hello"
   * fileName("/hello") == "hello"
   * fileName("/hello/") == fileName("") == ""
   * fileName("/hello/world/this/is/a/path.json.zip.tar.gz") == "path.json.zip.tar.gz"
   * </code></pre>
   */
  @PluginFunction
  public static Primitive fileName(RuntimeContext context, String path) {
    return context
        .getDataTypeImplementation()
        .primitiveOf(Iterables.getLast(Arrays.asList(path.split("/", -1)), path));
  }

  public static byte[] load(RuntimeContext context, String path) {
    ImportPath currentPath = context.getCurrentPackageContext().getCurrentImportPath();
    ImportPath nextPath = ImportPath.resolve(currentPath, path);

    Loader loader = context.getRegistries().getLoaderRegistry().get(nextPath.getLoader());
    if (loader == null) {
      throw new IllegalArgumentException(
          String.format(
              "Could not find loader %s. Are you missing a plugin import?", nextPath.getLoader()));
    }
    try {
      return loader.load(nextPath);
    } catch (IOException e) {
      throw WhistleRuntimeException.fromCurrentContext(
          context, new IOException(String.format("Could not load resource %s", path), e));
    }
  }

  @VisibleForTesting
  public static void setFs(FileSystem fs) {
    fileSystem = fs;
  }
}
