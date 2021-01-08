package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class DueDateServiceTest {

    private FixedDateService dateService;
    private DueDateService underTest;
    private HolidayService holidayService;

    @BeforeEach
    void setUp() {
        dateService = new FixedDateService();
        holidayService = mock(HolidayService.class);
        underTest = new DueDateService(dateService, holidayService);
    }

    @Test
    void haveToSetEitherADueDateOrHaveWorkingDays() {
        assertThrows(IllegalStateException.class, () -> {
            underTest.calculateDueDate(
                null,
                0
            );
        });
    }

    @Test
    void ifADueDateIsAlreadySetDoNotCalculateANewOne() {
        ZonedDateTime providedDueDate = ZonedDateTime.now();
        ZonedDateTime calculatedDueDate = underTest.calculateDueDate(
            providedDueDate, 0
        );

        assertThat(calculatedDueDate, is(providedDueDate));
    }

    @Test
    void calculateDueDateAllWorkingDays() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 1, 1, 2, 3, 4, ZoneId.systemDefault()),
                         2, ZonedDateTime.of(2020, 9, 1, 1, 2, 3, 4, ZoneId.systemDefault()).plusDays(2)
        );
    }

    @Test
    void calculateDueDateWhenFallInAWeekend() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 1, 2, 3, 4, ZoneId.systemDefault()), 2,
                         ZonedDateTime.of(2020, 9, 7, 1, 2, 3, 4, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhenStraddlesAWeekend() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 1, 2, 3, 4, ZoneId.systemDefault()), 4,
                         ZonedDateTime.of(2020, 9, 9, 1, 2, 3, 4, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichStraddlesMultipleWeekends() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 1, 2, 3, 4, ZoneId.systemDefault()), 10,
                         ZonedDateTime.of(2020, 9, 17, 1, 2, 3, 4, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichFallsOnAWeekend() {
        checkWorkingDays(ZonedDateTime.of(2020, 9, 3, 1, 2, 3, 4, ZoneId.systemDefault()), 10,
                         ZonedDateTime.of(2020, 9, 17, 1, 2, 3, 4, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichFallsOnAHoliday() {
        when(holidayService.isHoliday(ZonedDateTime.of(2020, 9, 3, 1, 2, 3, 4, ZoneId.systemDefault())))
            .thenReturn(true);
        checkWorkingDays(ZonedDateTime.of(2020, 9, 1, 1, 2, 3, 4, ZoneId.systemDefault()), 2,
                         ZonedDateTime.of(2020, 9, 4, 1, 2, 3, 4, ZoneId.systemDefault())
        );
    }

    @Test
    void calculateDueDateWhichStraddlesAHoliday() {
        when(holidayService.isHoliday(ZonedDateTime.of(2020, 9, 1, 1, 2, 3, 4, ZoneId.systemDefault()).plusDays(1)))
            .thenReturn(true);
        checkWorkingDays(ZonedDateTime.of(2020, 9, 1, 1, 2, 3, 4, ZoneId.systemDefault()), 2,
                         ZonedDateTime.of(2020, 9, 4, 1, 2, 3, 4, ZoneId.systemDefault())
        );
    }

    private void checkWorkingDays(ZonedDateTime startDay, int leadTimeDays, ZonedDateTime expectedDueDate) {
        dateService.setCurrentDateTime(startDay);

        ZonedDateTime calculatedDueDate = underTest.calculateDueDate(
            null, leadTimeDays
        );

        assertThat(calculatedDueDate, is(expectedDueDate));
    }

}
