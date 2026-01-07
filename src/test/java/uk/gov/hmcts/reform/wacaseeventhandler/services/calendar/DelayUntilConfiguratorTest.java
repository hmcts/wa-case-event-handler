package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar.DelayUntilIntervalData.MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DATE_FORMATTER;

@ExtendWith(MockitoExtension.class)
class DelayUntilConfiguratorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    private DelayUntilConfigurator delayUntilConfigurator;

    @Nested
    class DefaultWithoutAnyDateCalculator {

        @BeforeEach
        void setUp() {
            delayUntilConfigurator = new DelayUntilConfigurator(List.of());
        }

        @Test
        void should_return_default_calculated_dates_when_there_are_no_dmn_responses() {
            LocalDateTime calculateDelayUntil = delayUntilConfigurator.calculateDelayUntil(
                DelayUntilRequest.builder().build());
            assertThat(calculateDelayUntil).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.SECONDS));
        }

        @Test
        void should_set_date_as_current_day_when_delay_until_is_given() {
            LocalDateTime calculateDelayUntil = delayUntilConfigurator.calculateDelayUntil(
                DelayUntilRequest.builder().delayUntil("2023-01-10T16:00").build()
            );

            assertThat(calculateDelayUntil).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.SECONDS));
        }
    }

    @Nested
    class DefaultWithDateCalculators {
        @Mock
        private PublicHolidaysCollection publicHolidaysCollection;

        @BeforeEach
        void setUp() {
            delayUntilConfigurator = new DelayUntilConfigurator(List.of(
                new DelayUntilDateCalculator(),
                new DelayUntilDateTimeCalculator(),
                new DelayUntilIntervalCalculator(new WorkingDayIndicator(publicHolidaysCollection))
            ));

            Set<LocalDate> localDates = Set.of(
                LocalDate.of(2022, 1, 3),
                LocalDate.of(2022, 4, 15),
                LocalDate.of(2022, 4, 18),
                LocalDate.of(2022, 5, 2),
                LocalDate.of(2022, 6, 2),
                LocalDate.of(2022, 6, 3),
                LocalDate.of(2022, 8, 29),
                LocalDate.of(2022, 9, 19),
                LocalDate.of(2022, 12, 26),
                LocalDate.of(2022, 12, 27)
            );

            lenient().when(publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI))).thenReturn(localDates);

        }

        @Test
        void should_return_default_calculated_dates_when_there_are_no_dmn_responses() {
            LocalDateTime calculateDelayUntil = delayUntilConfigurator.calculateDelayUntil(
                DelayUntilRequest.builder().build());
            assertThat(calculateDelayUntil).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.SECONDS));
        }

        @Test
        void should_calculate_delay_until_when_delay_until_is_given() {
            String expectedDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);

            DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
                .delayUntil(expectedDelayUntil + "T16:00")
                .build();

            LocalDateTime dateValue = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
            assertThat(dateValue).isEqualTo(GIVEN_DATE.withHour(16));
        }

        @Test
        void should_calculate_delay_until_when_time_is_given() {
            String localDateTime = LocalDateTime.now().format(DATE_FORMATTER);

            DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
                .delayUntilTime("16:00")
                .build();

            LocalDateTime responseValue = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
            assertThat(responseValue).isEqualTo(localDateTime + "T16:00");
        }

        @Test
        void should_calculate_when_interval_is_greater_than_0_and_given_holidays() {
            String localDateTime = GIVEN_DATE.format(DATE_FORMATTER);

            DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
                .delayUntilOrigin(localDateTime + "T20:00")
                .delayUntilIntervalDays(5)
                .delayUntilNonWorkingCalendar(CALENDAR_URI)
                .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
                .delayUntilSkipNonWorkingDays(true)
                .delayUntilMustBeWorkingDay(MUST_BE_WORKING_DAY_NEXT)
                .delayUntilTime("18:00")
                .build();

            LocalDateTime delayUntilDate = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

            assertThat(delayUntilDate).isEqualTo(GIVEN_DATE.plusDays(7).withHour(18));
        }
    }
}
