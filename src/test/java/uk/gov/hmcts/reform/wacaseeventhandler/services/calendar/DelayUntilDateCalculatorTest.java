package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DATE_FORMATTER;

@ExtendWith(MockitoExtension.class)
class DelayUntilDateCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);

    private DelayUntilDateCalculator delayUntilDateCalculator;

    @BeforeEach
    public void before() {
        delayUntilDateCalculator = new DelayUntilDateCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_delay_until_origin_and_time() {
        String expectedDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(expectedDelayUntil + "T16:00")
            .delayUntilTime("16:00")
            .build();

        assertThat(delayUntilDateCalculator.supports(delayUntilObject)).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_only_delay_until_time() {
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilTime("16:00")
            .build();

        assertThat(delayUntilDateCalculator.supports(delayUntilObject)).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_delay_until_but_not_origin() {
        String expectedDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(expectedDelayUntil + "T16:00")
            .delayUntilTime("16:00")
            .build();

        assertThat(delayUntilDateCalculator.supports(delayUntilObject)).isTrue();
    }


    @Test
    void should_calculate_delay_until_when_delay_until_is_given() {
        String expectedDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(expectedDelayUntil + "T16:00")
            .build();

        LocalDateTime dateValue = delayUntilDateCalculator.calculateDate(delayUntilObject);
        assertThat(dateValue).isEqualTo(GIVEN_DATE.withHour(16));
    }

    @Test
    void should_calculate_delay_until_when_delay_until_and_time_are_given() {
        String expectedDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(expectedDelayUntil + "T16:00")
            .delayUntilTime("20:00")
            .build();

        LocalDateTime dateValue = delayUntilDateCalculator.calculateDate(delayUntilObject);
        assertThat(dateValue).isEqualTo(GIVEN_DATE.withHour(20));
    }
}
