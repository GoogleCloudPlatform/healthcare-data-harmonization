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

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.integration.IntegrationTest;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockClosure;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicDouble;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Enclosing class for TimeFns Tests - allows support for parameterized and non-parameterized test
 * classes
 */
@RunWith(JUnit4.class)
public class TimeFnsTest {
  private static final String SUBDIR = "functioncall/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);
  private static final Instant CURRENT_TIME_EPOCH =
      Instant.ofEpochSecond(1577840400L); // 2020-01-01 01:00:00" in epoch.

  private TimeFns timeFns;

  @Before
  public void setup() {
    Clock clock = Clock.fixed(CURRENT_TIME_EPOCH, ZoneOffset.UTC);
    timeFns = new TimeFns(clock);
  }

  @Test
  public void currentTime_withFormat() {
    String format = "YYYY-MM-dd hh:mm:ss";
    String expectedTimeString = "2020-01-01 01:00:00";
    assertEquals(
        expectedTimeString, timeFns.currentTime(new TestContext(), format).asPrimitive().string());
  }

  @Test
  public void currentTime_directoryFormat() {
    String format = "yyyy/MM/dd";
    String expectedTimeString = "2020/01/01";
    assertEquals(
        expectedTimeString, timeFns.currentTime(new TestContext(), format).asPrimitive().string());
  }

  @Test
  public void parseDateTime_matchesFormatNoTimezone() {
    String datetime = "20190410102049.123";
    String format = "yyyyMMddHHmmss.SSS";
    Primitive expected = testDTI().primitiveOf("2019-04-10T10:20:49.123Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_matchesFormatWithUTCTimezone() {
    String datetime = "20190410102049.123Z";
    String format = "yyyyMMddHHmmss.SSSZ";
    Primitive expected = testDTI().primitiveOf("2019-04-10T10:20:49.123Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_matchesFormatWithCustomTimezone_convertsToUTC() {
    String datetime = "20190410102049.123+0330";
    String format = "yyyyMMddHHmmss.SSSZ";
    Primitive expected = testDTI().primitiveOf("2019-04-10T06:50:49.123Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_doesNotMatchFormat_returnsNullData() {
    String datetime = "04/10/2019";
    String format = "yyyyMMddHHmmss.SSS";
    Primitive expected = NullData.instance;
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_missingComponents_defaults() {
    String datetime = "2019/07 12";
    String format = "yyyy/MM/dd HH";
    Primitive expected = testDTI().primitiveOf("2019-07-01T12:00:00.000Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_closeToISOFormat() {
    String datetime = "2007-07-16 15:11:25 UTC";
    String format = "yyyy-MM-dd HH:mm:ss ZZZ";
    Primitive expected = testDTI().primitiveOf("2007-07-16T15:11:25.000Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_closeTo12HourISOFormat() {
    String datetime = "2007-07-16 02:05:25 pm UTC";
    String format = "yyyy-MM-dd hh:mm:ss a ZZZ";
    Primitive expected = testDTI().primitiveOf("2007-07-16T14:05:25.000Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_isoFormat() {
    String datetime = "2002-08-31T08:18:29.000Z";
    String format = "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'";
    Primitive expected = testDTI().primitiveOf("2002-08-31T08:18:29.000Z");
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void parseDateTime_literals_notOptional() {
    String datetime = "2019/07 12";
    String format = "yyyy/MM 'X' HH";
    Primitive expected = NullData.instance;
    assertEquals(expected, TimeFns.parseDateTime(new TestContext(), format, datetime));
  }

  @Test
  public void formatDateTime_noTimezone_assumesUTC() {
    String input = "2020-01-01T01:00:00";
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    Primitive expected = testDTI().primitiveOf("2020/01/01/01/00/00/000/+0000");
    assertEquals(expected, TimeFns.formatDateTime(new TestContext(), format, input));
  }

  @Test
  public void formatDateTime_withTimezone_convertsToUTC() {
    String input = "2020-01-01T01:00:00-02:23";
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    Primitive expected = testDTI().primitiveOf("2020/01/01/03/23/00/000/+0000");
    assertEquals(expected, TimeFns.formatDateTime(new TestContext(), format, input));
  }

  @Test
  public void formatDateTime_doesNotMatchISO8601_returnsNull() {
    String input = "2020 01 01";
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    Primitive expected = NullData.instance;
    assertEquals(expected, TimeFns.formatDateTime(new TestContext(), format, input));
  }

  @Test
  public void formatDateTimeZ_withOffset_outputsWithOffset() {
    String input = "2020-01-01T01:00:00+0000";
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    String timezone = "+0400";
    Primitive expected = testDTI().primitiveOf("2020/01/01/05/00/00/000/+0400");
    assertEquals(expected, TimeFns.formatDateTimeZ(new TestContext(), format, timezone, input));
  }

  @Test
  public void formatDateTimeZ_inputWithOffset_outputsWithOffset() {
    String input = "2020-01-01T10:00:00+0100";
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    String timezone = "-0500";
    Primitive expected = testDTI().primitiveOf("2020/01/01/04/00/00/000/-0500");
    assertEquals(expected, TimeFns.formatDateTimeZ(new TestContext(), format, timezone, input));
  }

  @Test
  public void formatDateTimeZ_inputWithName_outputsWithOffset() {
    String input = "2020-01-01T10:00:00+0100";
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    String timezone = "EET";
    Primitive expected = testDTI().primitiveOf("2020/01/01/11/00/00/000/+0200");
    assertEquals(expected, TimeFns.formatDateTimeZ(new TestContext(), format, timezone, input));
  }

  @Test
  public void formatDateTimeZ_inputWithId_outputsWithOffsetAndDST() {
    String input = "2022-01-01T10:00:00-0400"; // No Daylight savings
    String inputDst = "2022-08-01T10:00:00-0400"; // Daylight savings
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    String timezone = "America/Toronto";
    Primitive expected = testDTI().primitiveOf("2022/01/01/09/00/00/000/-0500");
    assertEquals(expected, TimeFns.formatDateTimeZ(new TestContext(), format, timezone, input));

    expected = testDTI().primitiveOf("2022/08/01/10/00/00/000/-0400");
    assertEquals(expected, TimeFns.formatDateTimeZ(new TestContext(), format, timezone, inputDst));
  }

  @Test
  public void formatDateTimeZ_invalidTimezone_throws() {
    String input = "2022-01-01T10:00:00-0400"; // No Daylight savings
    String format = "yyyy/MM/dd/HH/mm/ss/SSS/Z";
    String timezone = "TICKTOCKONTHECLOCKBUTTHEPARTYDONTSTOP";

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> TimeFns.formatDateTimeZ(new TestContext(), format, timezone, input));
    assertThat(ex)
        .hasMessageThat()
        .contains("The datetime zone id 'TICKTOCKONTHECLOCKBUTTHEPARTYDONTSTOP' is not recognised");
  }

  @Test
  public void getEpochMillis_beginningOfEra() {
    String datetime = "1970-01-01T00:00:00Z";
    Primitive expected = testDTI().primitiveOf(0.);

    assertEquals(expected, TimeFns.getEpochMillis(new TestContext(), datetime));
  }

  @Test
  public void getEpochMillis_oneDayIntoEra() {
    String datetime = "1970-01-02T00:00:00Z";
    // 24 hr/day * 60 min/hr * 60 sec/min * 1000 msec/sec = 86,400,000 ms
    Primitive expected = testDTI().primitiveOf(86400000.);
    assertEquals(expected, TimeFns.getEpochMillis(new TestContext(), datetime));
  }

  @Test
  public void getEpochMillis_fiftyYearsIntoEra() {
    String datetime = "2020-01-01T00:00:00Z";
    // 50 yr * 365 day/yr * 24 hr/day * 60 min/hr * 60 sec/min * 1000 msec/sec= 1,576,800,000,000 ms
    // + 12 leap days over 1970-2020 = 1,577,836,800,000 ms
    Primitive expected = testDTI().primitiveOf(1577836800000.);

    assertEquals(expected, TimeFns.getEpochMillis(new TestContext(), datetime));
  }

  @Test
  public void getEpochMillis_oneMinuteDifference() {
    String datetimeOne = "2021-04-06T16:40:26.822065-05:00";
    String datetimeTwo = "2021-04-06T16:41:26.822065-05:00";
    double expectedDelta = 60000.; // 60s * 1000ms/s = 60,000 ms

    assertEquals(
        TimeFns.getEpochMillis(new TestContext(), datetimeTwo).num()
            - TimeFns.getEpochMillis(new TestContext(), datetimeOne).num(),
        expectedDelta,
        0.0);
  }

  @Test
  public void getEpochMillis_dateTimeDoesNotMatchISO8601_returnsNullData() {
    String datetime = "2020 01 01";
    Primitive expected = NullData.instance;

    assertEquals(expected, TimeFns.getEpochMillis(new TestContext(), datetime));
  }

  @Test
  public void parseEpochMillis_success() {
    long epochMillis = 1623994085123L;
    Primitive expected = testDTI().primitiveOf("2021-06-18T05:28:05.123Z");

    assertEquals(expected, TimeFns.parseEpochMillis(new TestContext(), epochMillis));
  }

  @Test
  public void parseEpochMillis_null() {
    assertEquals(NullData.instance, TimeFns.parseEpochMillis(new TestContext(), null));
  }

  @Test
  public void calculateElapsedDuration_years_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2023-09-17T14:00:00.123Z";
    Double expectedDelta = 2.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "YEARS").num());
  }

  @Test
  public void calculateElapsedDuration_months_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2023-09-17T14:00:00.123Z";
    Double expectedDelta = 24.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "MONTHS").num());
  }

  @Test
  public void calculateElapsedDuration_weeks_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2023-09-17T14:00:00.123Z";
    Double expectedDelta = 105.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "WEEKS").num());
  }

  @Test
  public void calculateElapsedDuration_days_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    Double expectedDelta = 5.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "DAYS").num());
  }

  @Test
  public void calculateElapsedDuration_hours_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    Double expectedDelta = 120.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "HOURS").num());
  }

  @Test
  public void calculateElapsedDuration_minutes_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    Double expectedDelta = 7200.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "MINUTES").num());
  }

  @Test
  public void calculateElapsedDuration_seconds_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    Double expectedDelta = 432000.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "SECONDS").num());
  }

  @Test
  public void calculateElapsedDuration_millis_success() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    Double expectedDelta = 432000000.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "MILLIS").num());
  }

  @Test
  public void calculateElapsedDuration_unmatched_timescale() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2023-09-17T14:00:00.123Z";
    assertThrows(
        IllegalArgumentException.class,
        () -> TimeFns.calculateElapsedDuration(new TestContext(), start, end, "NANOS").num());
  }

  @Test
  public void calculateElapsedDuration_invalid_start() {
    String start = "2021-09-1214:00:00.123Z";
    String end = "2023-09-17T14:00:00.123Z";
    assertThrows(
        IllegalArgumentException.class,
        () -> TimeFns.calculateElapsedDuration(new TestContext(), start, end, "YEARS").num());
  }

  @Test
  public void calculateElapsedDuration_invalid_end() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2023-09-1714:00:00.123Z";
    assertThrows(
        IllegalArgumentException.class,
        () -> TimeFns.calculateElapsedDuration(new TestContext(), start, end, "YEARS").num());
  }

  @Test
  public void calculateElapsedDuration_start_end_reversed_success() {
    String start = "2021-09-17T14:00:00.123Z";
    String end = "2021-09-12T14:00:00.123Z";
    Double expectedDelta = 120.0;
    assertEquals(
        expectedDelta,
        TimeFns.calculateElapsedDuration(new TestContext(), start, end, "HOURS").num());
  }

  @Test
  public void isDateTimeBetween_datetime_between_true() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    String compareValue = "2021-09-15T00:00:00.000Z";
    boolean expectedValue = true;
    assertEquals(
        expectedValue,
        TimeFns.isDateTimeBetween(new TestContext(), start, end, compareValue).bool());
  }

  @Test
  public void isDateTimeBetween_start_end_reversed_false() {
    String start = "2021-09-17T14:00:00.123Z";
    String end = "2021-09-12T14:00:00.123Z";
    String compareValue = "2021-09-15T00:00:00.000Z";
    boolean expectedValue = false;
    assertEquals(
        expectedValue,
        TimeFns.isDateTimeBetween(new TestContext(), start, end, compareValue).bool());
  }

  @Test
  public void isDateTimeBetween_compare_value_earlier_false() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    String compareValue = "2021-09-10T00:00:00.000Z";
    boolean expectedValue = false;
    assertEquals(
        expectedValue,
        TimeFns.isDateTimeBetween(new TestContext(), start, end, compareValue).bool());
  }

  @Test
  public void isDateTimeBetween_compare_value_later_false() {
    String start = "2021-09-12T14:00:00.123Z";
    String end = "2021-09-17T14:00:00.123Z";
    String compareValue = "2021-09-21T00:00:00.000Z";
    boolean expectedValue = false;
    assertEquals(
        expectedValue,
        TimeFns.isDateTimeBetween(new TestContext(), start, end, compareValue).bool());
  }

  @Test
  public void isDateTimeBetween_invalid_input_throws() {
    String start = "2021-09-1214:00:00.123Z";
    String end = "2023-09-17T14:00:00.123Z";
    String compareValue = "2021-09-2100:00:00.000Z";
    assertThrows(
        IllegalArgumentException.class,
        () -> TimeFns.isDateTimeBetween(new TestContext(), start, end, compareValue).num());
  }

  @Test
  public void timed_handlerGetsElapsed() throws Exception {
    AtomicDouble sentry = new AtomicDouble();
    RuntimeContext ctx = RuntimeContextUtil.testContext();
    Data expected = ctx.getDataTypeImplementation().primitiveOf("A return value");
    Data returned =
        timeFns.timed(
            ctx,
            new MockClosure(0, (args, rtx) -> expected),
            new MockClosure(
                0,
                (args, rtx) -> {
                  sentry.set(args.get(0).asPrimitive().num());
                  return NullData.instance;
                }));
    assertEquals(expected, returned);
    assertThat(sentry.get()).isEqualTo(0.0);
  }

  @Test
  public void timed_returnsResultCallsHandler() throws Exception {
    Engine engine = TESTER.initializeTestFile("timed_func.wstl");
    Data actual = engine.transform(NullData.instance);
    Container expected =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "funcOutput", testDTI().primitiveOf(3.0),
                    "handlerOutput", testDTI().primitiveOf("logged time")));
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void withTimeout_returnsResultCallsHandler() throws Exception {
    Engine engine = TESTER.initializeTestFile("with_timeout.wstl");
    Data actual = engine.transform(NullData.instance);
    Container expected =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "noTimeoutOutput", testDTI().primitiveOf(3.0),
                    "timeoutOutput", testDTI().primitiveOf("timed out")));
    assertDCAPEquals(expected, actual);
  }
}
