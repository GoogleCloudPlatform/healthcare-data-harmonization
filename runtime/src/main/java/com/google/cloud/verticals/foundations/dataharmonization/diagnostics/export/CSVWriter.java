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
package com.google.cloud.verticals.foundations.dataharmonization.diagnostics.export;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Exports data in CSV format. Example usage:
 *
 * <pre>{@code
 * new CSVWriter<Table<String, String, FunctionData>, Cell<String, String, FunctionData>>(
 *               t -> t.cellSet().iterator())
 *           .withColumn("Caller", Cell::getRowKey)
 *           .withColumn("Callee", Cell::getColumnKey)
 *           .withColumn("Num Calls", c -> String.valueOf(c.getValue().numCalls))
 *           .withColumn("Total Time (ms)", c -> String.valueOf(c.getValue().total.toMillis()))
 *           .withColumn(
 *               "Total Self Time (ms)", c -> String.valueOf(c.getValue().totalSelf.toMillis()))
 * }</pre>
 */
public class CSVWriter<DataT, RowT> implements Writer<DataT> {
  // TODO(rpolyano): Make these configurable if/when there is a usecase.
  private static final String DELIMITER = ",";
  private static final byte[] DELIMITER_BYTES = DELIMITER.getBytes(UTF_8);
  private static final String NEWLINE = "\n";
  private static final byte[] NEWLINE_BYTES = NEWLINE.getBytes(UTF_8);

  private final List<Column<RowT>> columns = new ArrayList<>();
  private final RowIterator<DataT, RowT> iterator;

  public CSVWriter(RowIterator<DataT, RowT> iterator) {
    this.iterator = iterator;
  }

  /** Add a column with the given value extractor and header title/name. */
  @CanIgnoreReturnValue
  public CSVWriter<DataT, RowT> withColumn(String name, Function<RowT, String> valueExtractor) {
    columns.add(
        new Column<>() {

          @Override
          public String name() {
            return name;
          }

          @Override
          public String value(RowT row) {
            return valueExtractor.apply(row);
          }
        });
    return this;
  }

  @Override
  public void write(OutputStream outputStream, DataT data) throws IOException {
    if (columns.isEmpty()) {
      throw new IllegalStateException("Cannot write CSV with no columns");
    }

    // Header row.
    ImmutableList<String> row = columns.stream().map(Column::name).collect(toImmutableList());
    writeRow(row, outputStream);

    // Content.
    Iterator<RowT> rows = iterator.rows(data);
    while (rows.hasNext()) {
      RowT rowData = rows.next();
      row = columns.stream().map(c -> c.value(rowData)).collect(toImmutableList());
      writeRow(row, outputStream);
    }
  }

  private void write(OutputStream outputStream, String value) throws IOException {
    value = value.replace("\"", "\"\"");
    if (value.contains(DELIMITER) || value.contains(NEWLINE) || value.contains("\"")) {
      value = String.format("\"%s\"", value);
    }

    outputStream.write(value.getBytes(UTF_8));
  }

  private void writeRow(List<String> cells, OutputStream outputStream) throws IOException {
    for (int i = 0; i < cells.size(); i++) {
      write(outputStream, cells.get(i));
      if (i < cells.size() - 1) {
        outputStream.write(DELIMITER_BYTES);
      }
    }
    outputStream.write(NEWLINE_BYTES);
  }

  private interface Column<RowT> {
    String name();

    String value(RowT row);
  }

  /**
   * Extracts iterable rows from a data object.
   *
   * @param <DataT> Type of data object.
   * @param <RowT> Type of row object.
   */
  @FunctionalInterface
  public interface RowIterator<DataT, RowT> {
    Iterator<RowT> rows(DataT data);
  }
}
