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
package com.google.cloud.verticals.foundations.dataharmonization.diagnostics.export;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for CSVWriter. */
@RunWith(JUnit4.class)
public class CSVWriterTest {
  private ByteArrayOutputStream testBuffer;
  private CSVWriter<TestData, TestRow> writer;

  @Before
  public void setup() {
    testBuffer = new ByteArrayOutputStream();
    writer = new CSVWriter<>(d -> d.rows().iterator());
  }

  public String testBufferContents() {
    return testBuffer.toString(UTF_8);
  }

  @Test
  public void write_noCols_throws() {
    assertThrows(IllegalStateException.class, () -> writer.write(testBuffer, mock(TestData.class)));
  }

  @Test
  public void write_noRows_writesHeader() throws Exception {
    writer =
        writer
            .withColumn("a", TestRow::a)
            .withColumn("b", TestRow::b)
            .withColumn("c", r -> r.a() + r.b()); // Computed column.

    TestData data = mock(TestData.class);
    when(data.rows()).thenReturn(ImmutableList.of());

    writer.write(testBuffer, data);
    String got = testBufferContents();
    assertThat(got).isEqualTo("a,b,c\n");
  }

  @Test
  public void write_headersWithSpecialChars_escapes() throws Exception {
    writer =
        writer
            .withColumn("a,a,a", TestRow::a)
            .withColumn("b\"b", TestRow::b)
            .withColumn("c", r -> r.a() + r.b()); // Computed column.

    TestData data = mock(TestData.class);
    when(data.rows()).thenReturn(ImmutableList.of());

    writer.write(testBuffer, data);
    String got = testBufferContents();
    assertThat(got).isEqualTo("\"a,a,a\",\"b\"\"b\",c\n");
  }

  @Test
  public void write_rowsWithSpecialChars_escapes() throws Exception {
    writer =
        writer
            .withColumn("a", TestRow::a)
            .withColumn("b", TestRow::b)
            .withColumn("c", r -> r.a() + r.b()); // Computed column.

    TestData data = mock(TestData.class);
    when(data.rows())
        .thenReturn(
            ImmutableList.of(
                TestRow.of("a1", "b1"),
                TestRow.of("a2,a2", "b2,b2"),
                TestRow.of("a3\"a3", "b3\"b3"),
                TestRow.of("a4\na4", "b4\nb4")));

    writer.write(testBuffer, data);
    String got = testBufferContents();
    assertThat(got)
        .isEqualTo(
            "a,b,c\n"
                + "a1,b1,a1b1\n" // No special chars.
                + "\"a2,a2\",\"b2,b2\",\"a2,a2b2,b2\"\n" // Commas.
                + "\"a3\"\"a3\",\"b3\"\"b3\",\"a3\"\"a3b3\"\"b3\"\n" // Quotes.
                + "\"a4\n" // Newlines ...
                + "a4\",\"b4\n"
                + "b4\",\"a4\n"
                + "a4b4\n"
                + "b4\"\n"); // ... end of row here.
  }

  private interface TestData {
    List<TestRow> rows();
  }

  private interface TestRow {
    String a();

    String b();

    static TestRow of(String a, String b) {
      return new TestRow() {
        @Override
        public String a() {
          return a;
        }

        @Override
        public String b() {
          return b;
        }
      };
    }
  }
}
