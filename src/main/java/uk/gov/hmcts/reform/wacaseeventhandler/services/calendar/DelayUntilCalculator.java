package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface DelayUntilCalculator {

    String DEFAULT_NON_WORKING_CALENDAR = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm")
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalEnd()
        .optionalStart()
        .appendLiteral('.')
        .appendValue(ChronoField.MILLI_OF_SECOND, 1, 3, SignStyle.NORMAL)
        .optionalEnd()
        .toFormatter();
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime DEFAULT_DATE_TIME = LocalDateTime.now();

    boolean supports(DelayUntilRequest delayUntilRequest);

    LocalDateTime calculateDate(DelayUntilRequest delayUntilRequest);

    default LocalDateTime addTimeToDate(String dueDateTime, LocalDateTime date) {
        return useDateTime(date, dueDateTime);
    }

    default LocalDateTime parseDateTime(String inputDate) {
        try {
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(inputDate).withZoneSameLocal(zoneId);
            return zonedDateTime.toLocalDateTime();
        } catch (DateTimeParseException p) {
            if (dateContainsTime(inputDate)) {
                Optional<LocalDateTime> calculated = parseDateTime(inputDate, DATE_TIME_FORMATTER);
                return calculated
                        .orElseThrow(() -> new RuntimeException("Provided date has invalid format: " + inputDate));
            } else {
                return LocalDate.parse(inputDate, DATE_FORMATTER).atStartOfDay();
            }
        }
    }

    default Optional<LocalDateTime> parseDateTime(String inputDate, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(inputDate, formatter));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    default boolean dateContainsTime(String dueDate) {
        return dueDate.contains("T");
    }

    default LocalDateTime useDateTime(LocalDateTime date, String dueDateTime) {

        List<String> split = Arrays.asList(dueDateTime.replace("T", "").trim().split(":"));
        return date
            .with(ChronoField.HOUR_OF_DAY, Long.parseLong(split.get(0)))
            .with(ChronoField.MINUTE_OF_HOUR, Long.parseLong(split.get(1)))
            .with(ChronoField.SECOND_OF_MINUTE, 0)
            .with(ChronoField.NANO_OF_SECOND, 0);
    }
}
