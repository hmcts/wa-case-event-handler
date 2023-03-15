package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DATE_FORMATTER;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_DATE_TIME;

@ExtendWith(MockitoExtension.class)
class DelayUntilDateTimeCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    private DelayUntilDateTimeCalculator delayUntilDateTimeCalculator;

    @BeforeEach
    public void before() {
        delayUntilDateTimeCalculator = new DelayUntilDateTimeCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date() {
        String localDateTime = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(localDateTime + "T16:00")
            .delayUntilTime("16:00")
            .build();

        assertThat(delayUntilDateTimeCalculator.supports(delayUntilObject)).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date_origin() {
        String localDateTime = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(localDateTime + "T16:00")
            .delayUntilOrigin(localDateTime + "T20:00")
            .build();

        assertThat(delayUntilDateTimeCalculator.supports(delayUntilObject)).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_due_date_time() {
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilTime("16:00")
            .build();

        assertThat(delayUntilDateTimeCalculator.supports(delayUntilObject)).isTrue();
    }

    @Test
    void should_calculate_due_date_when_time_is_given() {
        String localDateTime = DEFAULT_DATE_TIME.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilTime("16:00")
            .build();

        LocalDateTime responseValue = delayUntilDateTimeCalculator.calculateDate(delayUntilObject);
        assertThat(responseValue).isEqualTo(localDateTime + "T16:00");
    }
}