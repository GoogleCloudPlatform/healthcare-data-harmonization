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

package com.google.cloud.verticals.foundations.dataharmonization.data.path;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Path represents a JSONPath style path through Data. */
public final class Path implements Serializable {
  /**
   * The escape character in a path, indicating that the next character is a literal part of the
   * path. It can also be used to escape itself.
   */
  static final char ESCAPE_CHAR = '\\';

  /** Character indicating start of an array index. */
  static final char INDEX_OPEN_CHAR = '[';

  /** Character indicating wildcard array access. */
  static final char WILDCARD_CHAR = '*';

  /** Character indicating end of an array index. */
  static final char INDEX_CLOSE_CHAR = ']';

  /** Character delimiting to path segments. */
  static final char SEGMENT_DELIM_CHAR = '.';

  private List<PathSegment> segments;

  private Path(List<PathSegment> segments) {
    this.segments = segments;
  }

  /**
   * Parses the given string into a Path instance. Acceptable formatting for a path segment is:
   *
   * <ul>
   *   <li>Any character that isn't a . (dot), [ (open square bracket), ] (close square bracket), or
   *       \ (backslash).
   *   <li>A \ (backslash) followed by: a . (dot), [ (open square bracket), ] (close square
   *       bracket), or \ (backslash).
   *   <li>An index in the format of [#] where # is any non-negative number.
   *   <li>A null index like [], literally; See {@link Index#Index(Integer)} for semantics of a null
   *       index.
   *   <li>A wildcard like [*], literally.
   * </ul>
   *
   * @param path The path to parse.
   */
  public static Path parse(CharSequence path) {
    if (path == null || path.length() == 0) {
      return empty();
    }

    // Estimate number of segments to avoid overgrowing these array lists.
    // Note: below code will likely trigger Mutants but has no effect on functionality, only
    // performance, so it should be ok.
    int approxNumSegs = 0;
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == SEGMENT_DELIM_CHAR || path.charAt(i) == INDEX_OPEN_CHAR) {
        approxNumSegs++;
      }
    }

    List<PathSegment> segments = new ArrayList<>(approxNumSegs);
    int lastWildcardIndex = -1;

    // Reuse the same result throughout to avoid a bunch of extra allocations.
    ConsumeResult result = new ConsumeResult();

    for (int i = 0; i < path.length(); ) {
      if (path.charAt(i) == SEGMENT_DELIM_CHAR) {
        i++;
        continue;
      }

      if (path.charAt(i) == INDEX_OPEN_CHAR
          && i < path.length() - 1
          && path.charAt(i + 1) == WILDCARD_CHAR) {
        consumeWildcard(result, i);
        lastWildcardIndex = segments.size();
      } else if (path.charAt(i) == INDEX_OPEN_CHAR) {
        consumeBracketted(result, path, i);
      } else {
        consumeField(result, path, i);
      }

      segments.add(result.seg);
      i = result.nextI;
    }

    // Only flatten arrays in a wildcard if they are produced by further wildcards. The last
    // wildcard needs to not flatten.
    if (lastWildcardIndex != -1) {
      segments.set(lastWildcardIndex, new Wildcard(/* flatten */ false));
    }

    return new Path(segments);
  }

  private static void consumeField(ConsumeResult result, CharSequence path, int i) {
    boolean escaped = false;

    // StringBuilder is inefficient and only will be used if there are escapes in the string that we
    // need to skip.
    StringBuilder builder = new StringBuilder();
    int start;
    int end;

    for (start = i, end = i;
        end < path.length()
            && (escaped
                || (path.charAt(end) != SEGMENT_DELIM_CHAR && path.charAt(end) != INDEX_OPEN_CHAR));
        end++) {
      if (!escaped && path.charAt(end) == ESCAPE_CHAR) {
        // Append everything up to this escape, and start the next sequence at the character
        // following.
        builder.append(path, start, end);
        start = end + 1;
        escaped = true;
        continue;
      }

      escaped = false;
    }

    // Append the last sequence, if we are using the string builder.
    if (start > i) {
      builder.append(path, start, end);
    }

    if (escaped) {
      throw new VerifyException(String.format("Field had trailing %s in %s", ESCAPE_CHAR, path));
    }

    result.seg = new Field(start == i ? path.subSequence(i, end).toString() : builder.toString());
    result.nextI = end;
  }

  private static void consumeBracketted(ConsumeResult result, CharSequence path, int i) {
    i++; // Skip [
    int start = i;
    int end = i;
    boolean isIndex = true;
    for (; i < path.length() && path.charAt(i) != INDEX_CLOSE_CHAR; i++) {
      end = i + 1;
      isIndex = isIndex && Character.isDigit(path.charAt(i));
    }

    CharSequence content = path.subSequence(start, end);
    if (i >= path.length() || path.charAt(i) != INDEX_CLOSE_CHAR) {
      throw new VerifyException(
          String.format(
              "Expected %s after %s in %s, found %s",
              INDEX_CLOSE_CHAR,
              content,
              path,
              i >= path.length() ? "the end of the path" : path.subSequence(i, i + 1)));
    }

    Integer index = null;
    String field = "";
    if (content.length() > 0 && isIndex) {
      index = Integer.parseInt(content, 0, content.length(), 10);
    } else if (content.length() > 0) {
      field = content.toString();
    }
    i++; // Skip ]
    result.seg = isIndex ? new Index(index) : new Field(field);
    result.nextI = i;
  }

  private static void consumeWildcard(ConsumeResult result, int i) {
    result.seg = new Wildcard(true);
    result.nextI = i + 3; // Skip [*]
  }

  private static class ConsumeResult {
    private PathSegment seg;
    private int nextI;
  }

  /** Creates an empty path. get(R) returns R, and set(R, x) returns x without modifying R. */
  public static Path empty() {
    return new Path(ImmutableList.of());
  }

  public static Path of(Collection<PathSegment> segments) {
    return new Path(ImmutableList.copyOf(segments));
  }
  /**
   * Applies this {@link Path} to the given {@link Data} and returns the value.
   *
   * @param root the value to apply the path to.
   * @return the value at the location under the given root, or {@link NullData} if some part of
   *     this path does not exist under the root.
   */
  public Data get(Data root) {
    if (segments.isEmpty()) {
      return root;
    }

    PathSegment head = segments.get(0);
    Path rest = new Path(segments.subList(1, segments.size()));
    return head.get(root, rest);
  }

  /**
   * Sets the given value at the location specified by this {@link Path} under the given {@link
   * Data}. Any missing intermediate values are filled in using {@link
   * DataTypeImplementation#emptyArray()} and {@link DataTypeImplementation#emptyContainer()}.
   *
   * @param dti used to supply empty containers and arrays when intermediate value is missing.
   * @param root the parent data to apply the path to, and under which to set the given value.
   * @param value the value to set. Can be {@link NullData}.
   * @return if a new root was created, this new parent is returned. Otherwise the given root is
   *     returned.
   */
  public Data set(DataTypeImplementation dti, Data root, Data value) {
    return set(dti, root, value, 0);
  }

  private Data set(DataTypeImplementation dti, Data container, Data value, int pathSegmentIndex) {
    if (pathSegmentIndex >= segments.size()) {
      return value;
    }
    if (container == null || (container.isNullOrEmpty() && !container.isWritable())) {
      container = segments.get(pathSegmentIndex).create(dti);
    }

    Data nextContainer = segments.get(pathSegmentIndex).get(container);
    value = set(dti, nextContainer, value, pathSegmentIndex + 1);
    container = segments.get(pathSegmentIndex).set(container, value);

    return container;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(ImmutableList.copyOf(segments));
  }

  @SuppressWarnings("unchecked") // The cast is fine, object is written above.
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    segments = (List<PathSegment>) stream.readObject();
  }

  // Auto-generated equality members.
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    return segments.equals(((Path) o).segments);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    for (PathSegment segment : segments) {
      if (segment.isField()) {
        str.append(".");
      }
      str.append(segment);
    }
    return str.toString();
  }

  @Override
  public int hashCode() {
    return segments.hashCode();
  }

  /** Returns true iff this path is an empty path. */
  public boolean isEmpty() {
    return segments.isEmpty();
  }

  public ImmutableList<PathSegment> getSegments() {
    return ImmutableList.copyOf(segments);
  }

  /** Gets the path to the last non leaf segment in the containing path. */
  public Path getPathToLastNonLeafSegment() {
    if (getSegments().size() <= 1) {
      return new Path(ImmutableList.of());
    }
    ImmutableList<PathSegment> lastContainerSegments =
        getSegments().subList(0, getSegments().size() - 1);
    return new Path(lastContainerSegments);
  }
}
