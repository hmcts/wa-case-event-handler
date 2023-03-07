package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;

public interface DelayUntilCalculator {

    String DEFAULT_NON_WORKING_CALENDAR = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    String DEFAULT_DATE_TIME = "16:00";
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2)
        .withHour(16).withMinute(0).withSecond(0);
    LocalDateTime DEFAULT_DATE = LocalDateTime.now().plusDays(2);

    boolean supports(DelayUntilObject delayUntilObject);

    LocalDateTime calculateDate(DelayUntilObject delayUntilObject);

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
                return LocalDateTime.parse(inputDate, DATE_TIME_FORMATTER);
            } else {
                return LocalDate.parse(inputDate, DATE_FORMATTER).atStartOfDay();
            }
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
