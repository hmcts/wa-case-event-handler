package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.Builder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class DueDateServiceTest {

    private DueDateService underTest;
    private HolidayService holidayService;

    @BeforeEach
    void setUp() {
        holidayService = mock(HolidayService.class);
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
    @ValueSource(ints = {00, 11, 16, 17}) // hours
    void calculateDelayUntilWithDelayDuration(int hour) {
        final ZonedDateTime actual = ZonedDateTime.of(2021, 3, 1, hour, 0, 0, 0, ZoneId.systemDefault());
        final ZonedDateTime expected = ZonedDateTime.of(2021, 3, 2, 16, 0, 0, 0, ZoneId.systemDefault());

        final ZonedDateTime zonedDateTime = underTest.calculateDelayUntil(actual, 1);

        assertEquals(expected, zonedDateTime);
    }

    @ParameterizedTest
    @ValueSource(ints = {00, 11, 16, 17}) // hours
    void calculateDelayUntilWithOutDelayDuration(int hour) {
        final ZonedDateTime actual = ZonedDateTime.of(2021, 3, 1, hour, 0, 0, 0, ZoneId.systemDefault());

        ZonedDateTime zonedDateTime = underTest.calculateDelayUntil(actual, 0);
        assertEquals(actual, zonedDateTime);

        zonedDateTime = underTest.calculateDelayUntil(actual, -1);
        assertEquals(actual, zonedDateTime);
    }

    @ParameterizedTest
    @MethodSource(value = "scenarioProvider")
    void calculateDelayUntil(ZonedDateTimeScenario scenario) {
        ZonedDateTime zonedDateTime = underTest.calculateDelayUntil(scenario.actualDateTime, scenario.days);
        assertEquals(scenario.expectedDateTime, zonedDateTime);
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
                    ZonedDateTime.of(2021, 3, 6, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),
            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(18)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 3, 19, 16, 0, 0, 0, ZoneId.systemDefault()))
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
                    ZonedDateTime.of(2020, 2, 29, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),
            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(30)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 3, 31, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build(),
            ZonedDateTimeScenario.builder()
                .actualDateTime(
                    ZonedDateTime.of(2021, 3, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .days(31)
                .expectedDateTime(
                    ZonedDateTime.of(2021, 4, 1, 16, 0, 0, 0, ZoneId.systemDefault()))
                .build()
        );
    }

    @Builder
    private static class ZonedDateTimeScenario {
        ZonedDateTime actualDateTime;
        int days;
        ZonedDateTime expectedDateTime;
    }

}
