package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.Builder;
import lombok.ToString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class DueDateServiceTest {

    private DueDateService underTest;
    private HolidayService holidayService;

    @BeforeEach
    void setUp() {
        holidayService = mock(HolidayService.class);
        when(holidayService.isWeekend(any(ZonedDateTime.class))).thenCallRealMethod();
        underTest = new DueDateService(holidayService);
    }

    @Test
    void ifADueDateIsAlreadySetDoNotCalculateANewOne() {
        ZonedDateTime providedDueDate = ZonedDateTime.now();
        final LocalTime fourPmTime = LocalTime.of(16, 0);

        final ZonedDateTime zonedDateTimeAt4Pm = providedDueDate.of(
            providedDueDate.toLocalDate(),
            fourPmTime,
            providedDueDate.getZone()
        );

        ZonedDateTime calculatedDueDate = underTest.calculateDueDate(
            zonedDateTimeAt4Pm, 0
        );

        assertThat(calculatedDueDate, is(zonedDateTimeAt4Pm));
    }

    @Test
    void calculateDueDateAllWorkingDays() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 1, 16, 0, 0, 0, ZoneId.systemDefault()),
            2, ZonedDateTime.of(2020, 9, 1, 16, 0, 0, 0, ZoneId.systemDefault()).plusDays(2)
        );
    }

    @Test
    void calculateDueDateWhenFallInAWeekend() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 16, 0, 0, 0, ZoneId.systemDefault()), 2,
            ZonedDateTime.of(2020, 9, 7, 16, 0, 0, 0, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhenStraddlesAWeekend() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 16, 0, 0, 0, ZoneId.systemDefault()), 4,
            ZonedDateTime.of(2020, 9, 9, 16, 0, 0, 0, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichStraddlesMultipleWeekends() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 16, 0, 0, 0, ZoneId.systemDefault()), 10,
            ZonedDateTime.of(2020, 9, 17, 16, 0, 0, 0, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichFallsOnAWeekend() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 16, 0, 0, 0, ZoneId.systemDefault()), 10,
            ZonedDateTime.of(2020, 9, 17, 16, 0, 0, 0, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichFallsOnAHoliday() {
        when(holidayService.isHoliday(ZonedDateTime.of(2020, 9, 3, 16, 0, 0, 0, ZoneId.systemDefault())))
            .thenReturn(true);
        checkWorkingDays(ZonedDateTime.of(2020, 9, 1, 16, 0, 0, 0, ZoneId.systemDefault()), 2,
            ZonedDateTime.of(2020, 9, 4, 16, 0, 0, 0, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichStraddlesAHoliday() {
        when(holidayService.isHoliday(ZonedDateTime.of(2020, 9, 1, 16, 0, 0, 0, ZoneId.systemDefault()).plusDays(1)))
            .thenReturn(true);
        checkWorkingDays(ZonedDateTime.of(2020, 9, 1, 16, 0, 0, 0, ZoneId.systemDefault()), 2,
            ZonedDateTime.of(2020, 9, 4, 16, 0, 0, 0, ZoneId.systemDefault())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {00, 11, 16, 17})
    void calculateDelayUntilWithDelayDuration(int hour) {
        final ZonedDateTime actual = ZonedDateTime.of(2021, 3, 1, hour, 0, 0, 0, ZoneId.systemDefault());
        final ZonedDateTime expected = ZonedDateTime.of(2021, 3, 2, 16, 0, 0, 0, ZoneId.systemDefault());

        final ZonedDateTime zonedDateTime = underTest.calculateDelayUntil(actual, 1);

        assertEquals(expected, zonedDateTime);
    }

    @ParameterizedTest
    @ValueSource(ints = {00, 11, 16, 17})
    void calculate_delay_until_without_delay_duration(int hour) {
        final ZonedDateTime given = ZonedDateTime.of(2021, 3, 1, hour, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime actual = underTest.calculateDelayUntil(given, 0);
        assertEquals(given, actual);

        actual = underTest.calculateDelayUntil(given, -1);
        assertEquals(given, actual);
    }

    @ParameterizedTest
    @MethodSource(value = "scenarioProvider")
    void calculateDelayUntil(ZonedDateTimeScenario scenario) {
        ZonedDateTime zonedDateTime = underTest.calculateDelayUntil(scenario.actualDateTime, scenario.days);
        assertEquals(scenario.expectedDateTime, zonedDateTime);
    }

    @Test
    void should_return_event_date_when_delay_duration_is_0() {

        ZonedDateTime eventDate = ZonedDateTime.of(2022, 7, 1, 9, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime actualDelayUntil = underTest.calculateDelayUntil(eventDate, 0);

        assertEquals(eventDate, actualDelayUntil);
    }

    @Test
    void should_not_call_holiday_service_when_delay_duration_is_0() {
        ZonedDateTime eventDate = ZonedDateTime.of(2022, 7, 1, 9, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime actualDelayUntil = underTest.calculateDelayUntil(eventDate, 0);

        assertEquals(eventDate, actualDelayUntil);
        verifyNoInteractions(holidayService);
    }

    @Test
    void should_return_event_date_when_add_working_days_for_delay_duration_is_0() throws Exception {
        ZonedDateTime eventDate = ZonedDateTime.of(2022, 7, 1, 9, 0, 0, 0, ZoneId.systemDefault());

        var method = DueDateService.class.getDeclaredMethod(
            "addWorkingDaysForDelayDuration",
            ZonedDateTime.class,
            int.class
        );
        method.setAccessible(true);

        ZonedDateTime actual = (ZonedDateTime) method.invoke(underTest, eventDate, 0);

        assertEquals(eventDate, actual);
        verifyNoInteractions(holidayService);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void should_return_next_working_day_when_event_date_is_friday_and_has_delay_duration(int delayDuration) {

        ZonedDateTime eventDate = ZonedDateTime.of(2022, 7, 1, 9, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime expectedDelayUntilDate = eventDate
            .plusDays(3 + (delayDuration - 1))
            .withHour(16).withMinute(0).withSecond(0).withNano(0);

        ZonedDateTime actualDelayUntilDate = underTest.calculateDelayUntil(eventDate, delayDuration);

        assertEquals(expectedDelayUntilDate, actualDelayUntilDate);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void should_return_next_working_day_when_event_date_plus_delay_duration_matches_holiday(int delayDuration) {
        ZonedDateTime eventDate = ZonedDateTime.of(2022, 8, 26, 9, 0, 0, 0, ZoneId.systemDefault());

        when(holidayService.isHoliday(eventDate.plusDays(3)))
            .thenReturn(true);

        ZonedDateTime expectedDelayUntilDate = ZonedDateTime.of(2022, 8, 30, 16, 0, 0, 0, ZoneId.systemDefault())
            .plusDays(delayDuration - 1L);

        ZonedDateTime actualDelayUntilDate = underTest.calculateDelayUntil(eventDate, delayDuration);

        assertEquals(expectedDelayUntilDate, actualDelayUntilDate);
    }

    @Test
    void should_skip_weekend_and_holiday_when_calculating_delay_duration() {
        ZonedDateTime eventDate = ZonedDateTime.of(2022, 8, 25, 9, 0, 0, 0, ZoneId.systemDefault());

        when(holidayService.isHoliday(any(ZonedDateTime.class))).thenReturn(false);
        when(holidayService.isHoliday(ZonedDateTime.of(2022, 8, 29, 9, 0, 0, 0, ZoneId.systemDefault())))
            .thenReturn(true);

        ZonedDateTime expectedDelayUntilDate = ZonedDateTime.of(2022, 8, 31, 16, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime actualDelayUntilDate = underTest.calculateDelayUntil(eventDate, 3);

        assertEquals(expectedDelayUntilDate, actualDelayUntilDate);
    }

    private void checkWorkingDays(ZonedDateTime startDay, int leadTimeDays, ZonedDateTime expectedDueDate) {
        ZonedDateTime calculatedDueDate = underTest.calculateDueDate(startDay, leadTimeDays);

        assertThat(calculatedDueDate, is(expectedDueDate));
    }

    private static Stream<ZonedDateTimeScenario> scenarioProvider() {
        return Stream.of(
            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(1)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 3, 2, 16, 0, 0, 0, ZoneId.systemDefault())
                ).build(),

            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(5)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 3, 8, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),

            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(18)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 3, 25, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),

            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 2, 28, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(1)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),

            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2020, 2, 28, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(1)
                .expectedDateTime(
                    ZonedDateTime.of(2020, 3, 2, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),

            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(30)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 4, 12, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),

            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(31)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 4, 13, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build()
        );
    }

    @Builder
    @ToString
    private static class ZonedDateTimeScenario {
        ZonedDateTime actualDateTime;
        int days;
        ZonedDateTime expectedDateTime;
    }

}
