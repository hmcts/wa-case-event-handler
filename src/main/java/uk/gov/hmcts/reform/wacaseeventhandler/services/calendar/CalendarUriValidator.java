package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.InvalidRequestParametersException;

import java.util.Arrays;
import java.util.List;

@Component
public class CalendarUriValidator {

    private static final String DEFAULT_ALLOWED_PREFIXES =
        "https://www.gov.uk/bank-holidays/,https://raw.githubusercontent.com/hmcts/";
    private static final String INVALID_URI_MESSAGE =
        "Invalid delayUntilNonWorkingCalendar value '%s'. "
            + "Only HTTPS calendar URLs from allowed prefixes are supported.";

    private final List<String> allowedPrefixes;

    public CalendarUriValidator(@Value("${calendar.allowed-prefixes:" + DEFAULT_ALLOWED_PREFIXES + "}")
                                String allowedPrefixes) {
        this.allowedPrefixes = Arrays.stream(allowedPrefixes.split(","))
            .map(String::trim)
            .filter(prefix -> !prefix.isBlank())
            .toList();
    }

    public List<String> validateCalendarUris(String nonWorkingCalendars) {
        return Arrays.stream(nonWorkingCalendars.split(","))
            .map(this::validateCalendarUri)
            .toList();
    }

    public String validateCalendarUri(String calendarUri) {
        String trimmedUri = calendarUri == null ? null : calendarUri.trim();
        boolean allowed = trimmedUri != null
            && !trimmedUri.isBlank()
            && allowedPrefixes.stream().anyMatch(trimmedUri::startsWith);

        if (!allowed) {
            throw invalidCalendarUri(trimmedUri);
        }

        return trimmedUri;
    }

    private InvalidRequestParametersException invalidCalendarUri(String calendarUri) {
        return new InvalidRequestParametersException(String.format(INVALID_URI_MESSAGE, calendarUri));
    }
}
