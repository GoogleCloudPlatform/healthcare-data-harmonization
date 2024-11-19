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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.beam.sdk.metrics.MetricsContainer;
import org.apache.beam.sdk.metrics.MetricsEnvironment;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;

/** Builtin functions for time. */
public class TimeFns implements Serializable {

  // Map containing the number of milliseconds for time scale, keyed by time scale identifier.
  private static final ImmutableMap<String, Long> TIME_SCALE_MAP;

  // Looks for 3 alternatives of format components:
  // ^X (where ^ start of string) | 'X' | -X or :X or 'X (quote from previous alt as look behind and
  // thus not captured)
  private static final Pattern FORMAT_SPLITTER =
      Pattern.compile("^[\\w]+?(?=[^\\w]|$)|'[^']*'|(?:(?<=')|[^\\w])[\\w]+?(?=[^\\w]|'|$)");

  private static final Locale LOCALE = Locale.US;

  // TODO(): Determine if you want to re-examine the method for initializing the time
  // scale map. More details on options in bug description.
  static {
    Map<String, Long> aMap = new HashMap<>();
    aMap.put("YEARS", 31536000000L);
    aMap.put("MONTHS", 2628000000L);
    aMap.put("WEEKS", 604800000L);
    aMap.put("DAYS", 86400000L);
    aMap.put("HOURS", 3600000L);
    aMap.put("MINUTES", 60000L);
    aMap.put("SECONDS", 1000L);
    aMap.put("MILLIS", 1L);
    TIME_SCALE_MAP = ImmutableMap.copyOf(aMap);
  }

  private final Clock clock;

  public TimeFns(Clock clock) {
    this.clock = clock;
  }

  private static double convertIso8601DatetimeToMillis(String iso8601DateTime) {
    try {
      return ISODateTimeFormat.dateTimeParser()
          .withLocale(LOCALE)
          .withZoneUTC()
          .parseDateTime(iso8601DateTime)
          .getMillis();
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "\nInput date was improperly formatted. Input must conform to ISO 8601 format"
              + " (yyyy-MM-dd'T'HH:mm:ss.SSSZ). Unable to convert to milliseconds value.",
          e);
    }
  }

  /**
   * Returns the current local date-time in the provided Joda-Time formatted string
   * (https://www.joda.org/joda-time/key_format.html).
   *
   * <pre><code>
   * // The result of calling the function is similar to `"2020-01-01T01:02:03.123Z"`.
   * timestamp: currentTime("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
   *
   * // The result of calling the function is similar to `"2020-01-01"`.
   * timestampYearMonthDate: currentTime("yyyy-MM-dd")
   * </code></pre>
   *
   * @param format the format for representing the current local time
   * @return {@code string} representing the current local date-time in the provided format
   */
  @PluginFunction
  public Primitive currentTime(RuntimeContext ctx, String format) {
    java.time.format.DateTimeFormatter formatter =
        java.time.format.DateTimeFormatter.ofPattern(format).withLocale(LOCALE);
    return ctx.getDataTypeImplementation().primitiveOf(ZonedDateTime.now(clock).format(formatter));
  }

  /**
   * Parses String timestamp into the ISO 8601 "Complete date plus hours, minutes, seconds and a
   * decimal fraction of a second" format, in the UTC timezone (https://www.w3.org/TR/NOTE-datetime,
   * yyyy-MM-dd'T'HH:mm:ss.SSSZ) from the format specified according to Java formatting rules:
   * https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
   *
   * <p>Missing components are defaulted, except literals which are non-optional.
   *
   * @param format format String for parsing the provided timestamp.
   * @param datetime timestamp String to be parsed.
   * @return Primitive string holding an ISO 8601 representation of the provided timestamp;
   *     NullData.instance if parse of timestamp using provided format fails.
   */
  @PluginFunction
  public static Primitive parseDateTime(RuntimeContext ctx, String format, String datetime) {
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
    Matcher matcher = FORMAT_SPLITTER.matcher(format);
    while (matcher.find()) {
      String match = matcher.group();
      DateTimeParser parser = DateTimeFormat.forPattern(match).getParser();
      if (match.startsWith("'") && match.endsWith("'")) {
        builder.append(parser);
      } else {
        builder.appendOptional(parser);
      }
    }

    return reformatDateTime(
        ctx,
        builder.toFormatter().withLocale(LOCALE).withZoneUTC(),
        ISODateTimeFormat.dateTime().withLocale(LOCALE).withZoneUTC(),
        datetime);
  }

  /**
   * Parses the given timestamp (which must be in ISO 8601 - https://www.w3.org/TR/NOTE-datetime,
   * yyyy-MM-dd'T'HH:mm:ss.SSSZ format), and reformats it according to the given format (forcing a
   * UTC timezone). The given format must be specified according to Java formatting rules:
   * https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
   *
   * @param format String indicating the destination format of the given timestamp.
   * @param iso8601DateTime a timestamp in ISO 8601 format.
   * @return Primitive string holding a representation of the provided timestamp formatted according
   *     to the given format; NullData.instance if input datetime is not valid as ISO 8601.
   */
  @PluginFunction
  public static Primitive formatDateTime(
      RuntimeContext ctx, String format, String iso8601DateTime) {
    return reformatDateTime(
        ctx,
        ISODateTimeFormat.dateTimeParser().withLocale(LOCALE).withZoneUTC(),
        DateTimeFormat.forPattern(format).withLocale(LOCALE).withZoneUTC(),
        iso8601DateTime);
  }

  /**
   * Parses the given timestamp (which must be in ISO 8601 - https://www.w3.org/TR/NOTE-datetime,
   * yyyy-MM-dd'T'HH:mm:ss.SSSZ format), and reformats it according to the given format with the
   * given timezone. The given format must be specified according to Java formatting rules:
   * https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
   *
   * @param format String indicating the destination format of the given timestamp.
   * @param timezone String describing a timezone either by id like "America/Toronto" or by offset
   *     like "+08:00".
   * @param iso8601DateTime a timestamp in ISO 8601 format.
   * @return Primitive string holding a representation of the provided timestamp formatted according
   *     to the given format; NullData.instance if input datetime is not valid as ISO 8601.
   */
  @PluginFunction
  public static Primitive formatDateTimeZ(
      RuntimeContext ctx, String format, String timezone, String iso8601DateTime) {
    return reformatDateTime(
        ctx,
        ISODateTimeFormat.dateTimeParser().withLocale(LOCALE).withZoneUTC(),
        DateTimeFormat.forPattern(format).withLocale(LOCALE).withZone(DateTimeZone.forID(timezone)),
        iso8601DateTime);
  }

  private static Primitive reformatDateTime(
      RuntimeContext ctx,
      DateTimeFormatter inputFormat,
      DateTimeFormatter outputFormat,
      String dateTime) {
    try {
      return ctx.getDataTypeImplementation()
          .primitiveOf(outputFormat.print(inputFormat.parseDateTime(dateTime)));
    } catch (IllegalArgumentException exception) {
      return NullData.instance;
    }
  }

  /**
   * Gets the milliseconds from the Java epoch of 1970-01-01T00:00:00Z for the provided datetime,
   * which must be in ISO 8601 (https://www.w3.org/TR/NOTE-datetime) format.
   *
   * @param iso8601DateTime a timestamp in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSZ).
   * @return {@link Primitive} double of the milliseconds since the Unix epoch for the provided
   *     datetime or {@link NullData} if parse of timestamp using the ISO 8601 format fails.
   */
  @PluginFunction
  public static Primitive getEpochMillis(RuntimeContext ctx, String iso8601DateTime) {
    try {
      return ctx.getDataTypeImplementation()
          .primitiveOf(
              (double)
                  ISODateTimeFormat.dateTimeParser()
                      .withLocale(LOCALE)
                      .withZoneUTC()
                      .parseDateTime(iso8601DateTime)
                      .getMillis());
    } catch (IllegalArgumentException exception) {
      return NullData.instance;
    }
  }

  /**
   * Gets the Java epoch of 1970-01-01T00:00:00Z from the milliseconds for the provided datetime.
   *
   * @param input the number of milliseconds from 1970-01-01T00:00:00Z.
   * @return {@link Primitive} String of a timestamp in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSZ)
   *     or {@link NullData} if parse fails.
   */
  @PluginFunction
  public static Primitive parseEpochMillis(RuntimeContext ctx, Long input) {
    if (input == null) {
      return NullData.instance;
    }
    try {
      Instant instant = Instant.ofEpochMilli(input.longValue());
      return ctx.getDataTypeImplementation().primitiveOf(instant.toString());
    } catch (IllegalArgumentException exception) {
      return NullData.instance;
    }
  }

  /**
   * Calculates the duration between two points in time as follows:
   *
   * <ol>
   *   <li>Converts the ISO 8601-formatted start and end timestamp values to millisecond values.
   *   <li>Subtracts the start time from the end time.
   *   <li>Divides the remainder by the number of milliseconds in the provided time scale.
   * </ol>
   *
   * ISO 8601 timestamps use the format {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ}.
   *
   * <pre><code>
   * var start: "2020-01-01T01:01:01.111Z"
   * var end: "2020-01-01T05:05:05.555Z"
   *
   * // Returns 14644
   * calculateElapsedDuration(start, end, "SECONDS")
   *
   * // Returns 244
   * calculateElapsedDuration(start, end, "MINUTES")
   *
   * // Returns 4
   * calculateElapsedDuration(start, end, "HOURS")
   *
   * // Returns 0
   * calculateElapsedDuration(start, end, "DAYS")
   *
   * // Throws an IllegalArgumentException because "seconds" isn't uppercase.
   * calculateElapsedDuration(start, end, "seconds")
   * </code></pre>
   *
   * @param iso8601StartDateTime start timestamp
   * @param iso8601EndDateTime end timestamp
   * @param timeScale time scale to represent the difference between the two points in time.
   *     Case-sensitive and must be uppercase. Supported time scales:
   *     <ul>
   *       <li>YEARS
   *       <li>MONTHS
   *       <li>WEEKS
   *       <li>DAYS
   *       <li>HOURS
   *       <li>MINUTES
   *       <li>SECONDS
   *       <li>MILLIS
   *     </ul>
   *
   * @return {@link Primitive} {@code number}
   * @throws IllegalArgumentException if the start time or end time aren't ISO 8601-formatted or if
   *     an unsupported time scale is used
   */
  @PluginFunction
  public static Primitive calculateElapsedDuration(
      RuntimeContext ctx,
      String iso8601StartDateTime,
      String iso8601EndDateTime,
      String timeScale) {
    double startMillis = convertIso8601DatetimeToMillis(iso8601StartDateTime);
    double endMillis = convertIso8601DatetimeToMillis(iso8601EndDateTime);
    Long timeScaleValue = TIME_SCALE_MAP.get(timeScale);
    if (timeScaleValue == null) {
      throw new IllegalArgumentException(
          "The time scale specified for the calculated duration is not currently supported. Time"
              + " scales currently supported include: \"YEARS\", \"MONTHS\", \"WEEKS\", \"DAYS\","
              + " \"HOURS\", \"MINUTES\", \"SECONDS\", \"MILLIS\"");
    }
    return ctx.getDataTypeImplementation()
        .primitiveOf(Math.floor(Math.abs((endMillis - startMillis) / timeScaleValue)));
  }

  /**
   * If {@code timeOffset} is positive, adds it to {@code iso8601DateTime}. If {@code timeOffset} is
   * negative, subtracts it from {@code iso8601DateTime}. Uses the provided time scale.
   *
   * <p>ISO 8601 timestamps use the format {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ}.
   *
   * <pre><code>
   * var start: "2020-01-01T01:01:01.111Z"
   * var timeOffset: 5
   * // Adds 5 hours to the initial timestamp and returns "2020-01-01T06:01:01.111Z"
   * calculateNewDateTime(start, timeOffset, "HOURS")
   *
   * var start: "2020-01-01T01:01:01.111Z"
   * var timeOffset: 10
   * // Adds 10 days to the initial timestamp "2020-01-11T01:01:01.111Z"
   * calculateNewDateTime(start, timeOffset, "DAYS")
   *
   * var start: "2020-01-01T01:01:01.111Z"
   * var timeOffset: -50
   * // Subtracts 50 days from the initial timestamp and returns "2019-11-12T01:01:01.111Z"
   * calculateNewDateTime(start, timeOffset, "DAYS")
   *
   * // Throws an IllegalArgumentException because "seconds" isn't uppercase.
   * calculateNewDateTime((start, timeOffset, "seconds")
   * </code></pre>
   *
   * @param iso8601DateTime timestamp to modify
   * @param timeOffset timeOffset to add or subtract to the {@code iso8601DateTime} timestamp.
   *     Cannot be {@code null}.
   * @param timeScale the time scale to add or subtract {@code timeOffset} to or from.
   *     Case-sensitive and must be uppercase. Supported time scales:
   *     <ul>
   *       <li>YEARS
   *       <li>MONTHS
   *       <li>WEEKS
   *       <li>DAYS
   *       <li>HOURS
   *       <li>MINUTES
   *       <li>SECONDS
   *       <li>MILLIS
   *     </ul>
   *
   * @return {@link Primitive} {@code string} a new ISO 8601-formatted timestamp
   * @throws IllegalArgumentException if the start time or end time aren't ISO 8601-formatted, an
   *     unsupported time scale is used, or the value of {@code timeOffset} is {@code null}
   */
  @PluginFunction
  public static Primitive calculateNewDateTime(
      RuntimeContext ctx, String iso8601DateTime, Long timeOffset, String timeScale) {
    double startMillis = convertIso8601DatetimeToMillis(iso8601DateTime);
    if (timeOffset == null) {
      throw new IllegalArgumentException(
          "The timeOffset to apply to an ISO 8601 formatted timestamp cannot be null.");
    }
    Long timeScaleValue = TIME_SCALE_MAP.get(timeScale);
    if (timeScaleValue == null) {
      throw new IllegalArgumentException(
          "The time scale specified for the calculated duration is not currently supported. Time"
              + " scales currently supported include: \"YEARS\", \"MONTHS\", \"WEEKS\", \"DAYS\","
              + " \"HOURS\", \"MINUTES\", \"SECONDS\", \"MILLIS\"");
    }
    return parseEpochMillis(ctx, (long) (startMillis + (timeOffset * timeScaleValue)));
  }

  /**
   * Calculates whether the value specified by iso8601CompareDateTime exists within the window
   * defined by iso8601StartDateTime and iso8601EndDateTime. All ISO 8601 timestamps are converted
   * to millisecond values and a comparison is done inclusive of the start and end points.
   *
   * @param iso8601StartDateTime timestamp in ISO 8601 format(yyyy-MM-dd'T'HH:mm:ss.SSSZ)
   * @param iso8601EndDateTime timestamp in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSZ)
   * @param iso8601CompareDateTime timestamp in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSZ)
   * @return {@link Primitive} boolean value true if inside window, false otherwise.
   * @throws IllegalArgumentException if any of ISO 8601 formatted input values are improperly
   *     formatted.
   */
  @PluginFunction
  public static Primitive isDateTimeBetween(
      RuntimeContext ctx,
      String iso8601StartDateTime,
      String iso8601EndDateTime,
      String iso8601CompareDateTime) {
    try {
      double startMillis = convertIso8601DatetimeToMillis(iso8601StartDateTime);
      double endMillis = convertIso8601DatetimeToMillis(iso8601EndDateTime);
      double compareMillis = convertIso8601DatetimeToMillis(iso8601CompareDateTime);
      return ctx.getDataTypeImplementation()
          .primitiveOf(compareMillis >= startMillis && compareMillis <= endMillis);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "One or more of the input date(s) were improperly formatted. All date values should"
              + " be provided in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSZ)",
          e);
    }
  }

  /**
   * Executes the code given in {@code body}, while timing the execution. The return value of this
   * function is the return value of {@code body}, and the elapsed time is passed as a {@code $time}
   * parameter of type {@code Primitive} {@code number} to the code provided in {@code timeHandler}.
   * Note that the {@code timeHandler} won't be called if there's an exception in {@code body}.
   *
   * <p>Example usage:
   *
   * <pre><code>
   * result: timed(functionToTime(1, 2), functionToHandleMetrics($time))
   * def functionToTime(arg1, arg2) {
   *   "Passed {arg1} and {arg2}."
   * }
   * def functionToHandleMetrics($time) {
   *   logging::logInfo(): "Timed function executed in {$time} milliseconds."
   * }
   *
   * // Output:
   * result: "Passed 1 and 2."
   * // Log output:
   * INFO: Timed function executed in 9.0 milliseconds.
   * </code></pre>
   *
   * @param body code to execute and time
   * @param timeHandler code to handle the elapsed time result, in milliseconds
   * @return result of executing {@code body}
   */
  @PluginFunction
  public Data timed(RuntimeContext context, Closure body, Closure timeHandler) {
    long start = clock.millis();
    Data result = body.execute(context);
    long end = clock.millis();
    Primitive elapsedMS = context.getDataTypeImplementation().primitiveOf((double) (end - start));
    timeHandler.bindNextFreeParameter(elapsedMS).execute(context);
    return result;
  }

  /**
   * Executes the code given in {@code body}, timing out after the specified number of milliseconds.
   * If the execution finishes before the time limit is reached, the return value of this function
   * is the return value of {@code body}. If, on the other hand, the time limit is reached, the
   * execution will be aborted and the {@code timeoutHandler} will be called.
   *
   * @param body code to execute
   * @param millis number of milliseconds to wait
   * @param timeoutHandler code to execute when timing out
   * @return result of executing {@code body}
   */
  @PluginFunction
  public Data withTimeout(
      RuntimeContext context, Closure body, Long millis, Closure timeoutHandler) {
    // TODO(): Move withTimeout plugin to cloud_healthcare_data_harmoinzation
    MetricsContainer container = MetricsEnvironment.getCurrentContainer();
    TimeLimiter timeLimiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());
    try {
      return timeLimiter.callWithTimeout(
          () -> {
            MetricsEnvironment.setCurrentContainer(container);
            return body.execute(context);
          },
          millis,
          MILLISECONDS);
    } catch (TimeoutException e) {
      return timeoutHandler.execute(context);
    } catch (ExecutionException | InterruptedException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Sleep for the specified number of milliseconds.
   *
   * @param millis number of milliseconds to sleep
   * @return @link NullData}
   */
  @PluginFunction
  public Data sleep(RuntimeContext context, Long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new IllegalArgumentException(e);
    }
    return NullData.instance;
  }
}
