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
}
