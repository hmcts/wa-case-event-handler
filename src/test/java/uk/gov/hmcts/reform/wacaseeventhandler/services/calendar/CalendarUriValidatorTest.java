package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.InvalidRequestParametersException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarUriValidatorTest {

    private final CalendarUriValidator calendarUriValidator = new CalendarUriValidator(
        "https://www.gov.uk/bank-holidays/,https://raw.githubusercontent.com/hmcts/"
    );

    @Test
    void should_accept_allowed_https_calendars() {
        List<String> uris = calendarUriValidator.validateCalendarUris(
            "https://www.gov.uk/bank-holidays/england-and-wales.json, "
                + "https://raw.githubusercontent.com/hmcts/wa-task-management-api/master/src/test/resources/calendar.json"
        );

        assertThat(uris).containsExactly(
            "https://www.gov.uk/bank-holidays/england-and-wales.json",
            "https://raw.githubusercontent.com/hmcts/wa-task-management-api/master/src/test/resources/calendar.json"
        );
    }

    @Test
    void should_reject_non_https_calendar_uri() {
        assertThatThrownBy(() -> calendarUriValidator.validateCalendarUris("http://www.gov.uk/bank-holidays/test.json"))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessageContaining("Invalid delayUntilNonWorkingCalendar value");
    }

    @Test
    void should_reject_calendar_uri_with_disallowed_prefix() {
        assertThatThrownBy(
            () -> calendarUriValidator.validateCalendarUris(
                "https://raw.githubusercontent.com/other-org/calendar.json"
            )
        )
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessageContaining("Invalid delayUntilNonWorkingCalendar value");
    }

    @Test
    void should_accept_calendar_uri_with_query_string_when_prefix_is_allowed() {
        List<String> uris = calendarUriValidator.validateCalendarUris(
            "https://www.gov.uk/bank-holidays/england-and-wales.json?format=json"
        );

        assertThat(uris).containsExactly("https://www.gov.uk/bank-holidays/england-and-wales.json?format=json");
    }

    @Test
    void should_ignore_blank_allowed_prefix_entries() {
        CalendarUriValidator validator = new CalendarUriValidator(
            " https://www.gov.uk/bank-holidays/ , , https://raw.githubusercontent.com/hmcts/ , "
        );

        assertThat(validator.validateCalendarUri("https://raw.githubusercontent.com/hmcts/calendar.json"))
            .isEqualTo("https://raw.githubusercontent.com/hmcts/calendar.json");
    }

    @Test
    void should_trim_calendar_uri_before_validation() {
        assertThat(calendarUriValidator.validateCalendarUri("  https://www.gov.uk/bank-holidays/test.json  "))
            .isEqualTo("https://www.gov.uk/bank-holidays/test.json");
    }

    @Test
    void should_reject_null_calendar_uri() {
        assertThatThrownBy(() -> calendarUriValidator.validateCalendarUri(null))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessageContaining("Invalid delayUntilNonWorkingCalendar value 'null'"
                                      + ". Only HTTPS calendar URLs from allowed prefixes are supported.");
    }

    @Test
    void should_reject_blank_calendar_uri() {
        assertThatThrownBy(() -> calendarUriValidator.validateCalendarUri("   "))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessageContaining("Invalid delayUntilNonWorkingCalendar value ''"
                                      + ". Only HTTPS calendar URLs from allowed prefixes are supported.");
    }
}
