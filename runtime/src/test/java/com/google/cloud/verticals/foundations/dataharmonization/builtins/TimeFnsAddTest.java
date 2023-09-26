/*
 * Copyright 2021 Google LLC.
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

import static com.google.cloud.verticals.foundations.dataharmonization.builtins.TimeFns.calculateNewDateTime;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Parameterized tests for Date addition. */
@RunWith(Enclosed.class)
public class TimeFnsAddTest {

  /** Parameterized test for Date addition with valid inputs. */
  @RunWith(Parameterized.class)
  public static class ParameterizedAddToDateTimeTest {

    @Parameter public String testName;

    @Parameter(1)
    public String dateTime;

    @Parameter(2)
    public Long timeOffset;

    @Parameter(3)
    public String timeScale;

    @Parameter(4)
    public String expectedDateTime;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              "adding years",
              "2021-11-01T00:00:00Z",
              Long.valueOf(5),
              "YEARS",
              "2026-10-31T00:00:00Z",
            },
            {
              "adding months",
              "2021-01-01T00:00:00Z",
              Long.valueOf(5),
              "MONTHS",
              "2021-06-02T02:00:00Z",
            },
            {
              "adding weeks",
              "2021-01-01T00:00:00Z",
              Long.valueOf(6),
              "WEEKS",
              "2021-02-12T00:00:00Z",
            },
            {
              "adding days",
              "2021-01-01T00:00:00Z",
              Long.valueOf(6),
              "DAYS",
              "2021-01-07T00:00:00Z",
            },
            {
              "adding hours",
              "2021-01-01T00:00:00Z",
              Long.valueOf(6),
              "HOURS",
              "2021-01-01T06:00:00Z",
            },
            {
              "adding minutes",
              "2021-01-01T00:00:00Z",
              Long.valueOf(6),
              "MINUTES",
              "2021-01-01T00:06:00Z",
            },
            {
              "adding seconds",
              "2021-01-01T00:00:00Z",
              Long.valueOf(6),
              "SECONDS",
              "2021-01-01T00:00:06Z",
            },
            {
              "adding milliseconds",
              "2021-01-01T00:00:00Z",
              Long.valueOf(6),
              "MILLIS",
              "2021-01-01T00:00:00.006Z",
            },
            {
              "subtracting years",
              "2021-11-01T00:00:00Z",
              Long.valueOf(-5),
              "YEARS",
              "2016-11-02T00:00:00Z",
            },
            {
              "subtracting months",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-5),
              "MONTHS",
              "2020-08-01T22:00:00Z",
            },
            {
              "subtracting weeks",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-6),
              "WEEKS",
              "2020-11-20T00:00:00Z",
            },
            {
              "subtracting days",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-6),
              "DAYS",
              "2020-12-26T00:00:00Z",
            },
            {
              "subtracting hours",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-6),
              "HOURS",
              "2020-12-31T18:00:00Z",
            },
            {
              "subtracting minutes",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-6),
              "MINUTES",
              "2020-12-31T23:54:00Z",
            },
            {
              "subtracting seconds",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-6),
              "SECONDS",
              "2020-12-31T23:59:54Z",
            },
            {
              "subtracting milliseconds",
              "2021-01-01T00:00:00Z",
              Long.valueOf(-6),
              "MILLIS",
              "2020-12-31T23:59:59.994Z",
            },
          });
    }

    @Test
    public void testAddingToDate() {
      assertEquals(
          expectedDateTime,
          calculateNewDateTime(new TestContext(), dateTime, timeOffset, timeScale).string());
    }
  }

  /** Parameterized test for Exceptions from Date addition */
  @RunWith(Parameterized.class)
  public static class ExceptionsTest {

    @Parameter public String testName;

    @Parameter(1)
    public String dateTime;

    @Parameter(2)
    public Long timeOffset;

    @Parameter(3)
    public String timeScale;

    @Parameter(4)
    public String expectedExceptionMessage;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              "null value",
              "2021-11-01T00:00:00Z",
              null,
              "YEARS",
              "The timeOffset to apply to an ISO 8601 formatted timestamp cannot be null.",
            },
            {
              "invalid time scale",
              "2021-01-01T00:00:00Z",
              Long.valueOf(5),
              "NANOS",
              "The time scale specified for the calculated duration is not currently supported."
                  + " Time scales currently supported include: \"YEARS\", \"MONTHS\", \"WEEKS\","
                  + " \"DAYS\", \"HOURS\", \"MINUTES\", \"SECONDS\", \"MILLIS\"",
            },
            {
              "invalid format of timestamp",
              "2021-01-0100:00:00Z",
              Long.valueOf(6),
              "WEEKS",
              "Input date was improperly formatted. Input must conform to ISO 8601 format"
                  + " (yyyy-MM-dd'T'HH:mm:ss.SSSZ). Unable to convert to milliseconds value.",
            },
          });
    }

    @Test
    public void testAddingToDateException() {
      IllegalArgumentException thrown =
          assertThrows(
              IllegalArgumentException.class,
              () -> calculateNewDateTime(new TestContext(), dateTime, timeOffset, timeScale));
      assertThat(thrown).hasMessageThat().contains(expectedExceptionMessage);
    }
  }
}
