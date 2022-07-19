package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DueDateServiceTest {

    @Mock
    HolidayService holidayService;


    private DueDateService dueDateService;

    @BeforeEach
    void setUp() {
        dueDateService = new DueDateService(holidayService);
    }

    @Test
    void should_return_same_event_date_4_pm_when_working_days_allowed_is_less_than_1() {
        ZonedDateTime eventDate =
            ZonedDateTime.of(
                2022, 7, 1,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        ZonedDateTime actualDate = dueDateService.calculateDueDate(eventDate, 0);
        ZonedDateTime expectedDueDateTime = eventDate.with(
            LocalTime.of(16, 0, 0, 0)
        );
        assertThat(actualDate, is(expectedDueDateTime));
        verifyNoInteractions(holidayService);
    }

    @Test
    void should_return_next_working_day_4_pm_when_calculated_due_date_matches_holiday() {
        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        int workingDaysAllowed = 2;

        when(holidayService.isHoliday(eventDateTime.plusDays(1)))
            .thenReturn(true);

        ZonedDateTime expectedDueDate = eventDateTime.plusDays(workingDaysAllowed + 1);
        ZonedDateTime expectedDueDateTime = expectedDueDate.with(
            LocalTime.of(16, 0, 0, 0)
        );
        ZonedDateTime actualDateTime = dueDateService.calculateDueDate(eventDateTime, workingDaysAllowed);

        assertThat(actualDateTime, is(expectedDueDateTime));
        verify(holidayService, times(3)).isHoliday(any());
    }

    @Test
    void should_return_next_working_day_4_pm_when_calculated_due_date_not_matches_holiday() {
        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        int workingDaysAllowed = 2;

        when(holidayService.isHoliday(eventDateTime.plusDays(workingDaysAllowed)))
            .thenReturn(false);

        ZonedDateTime expectedDueDate = eventDateTime.plusDays(workingDaysAllowed);
        ZonedDateTime expectedDueDateTime = expectedDueDate.with(
            LocalTime.of(16, 0, 0, 0)
        );
        ZonedDateTime actualDateTime = dueDateService.calculateDelayUntil(eventDateTime, workingDaysAllowed);

        assertThat(actualDateTime, is(expectedDueDateTime));
        verify(holidayService, times(1)).isHoliday(any());
    }

    @Test
    void should_return_same_event_date_when_delay_duration_is_less_than_1() {
        ZonedDateTime eventDate =
            ZonedDateTime.of(
                2022, 7, 1,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        ZonedDateTime actualDate = dueDateService.calculateDelayUntil(eventDate, 0);

        assertThat(actualDate, is(eventDate));
        verifyNoInteractions(holidayService);
    }

    @Test
    void should_return_next_working_day_4_pm_when_delay_date_matches_holiday() {
        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        int delayDuration = 2;

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration)))
            .thenReturn(true);

        ZonedDateTime expectedDelayDate = eventDateTime.plusDays(delayDuration + 1);
        ZonedDateTime expectedDelayDateTime = expectedDelayDate.with(LocalTime.of(16, 0, 0, 0));
        ZonedDateTime actualDateTime = dueDateService.calculateDelayUntil(eventDateTime, delayDuration);

        assertThat(actualDateTime, is(expectedDelayDateTime));
        verify(holidayService, times(2)).isHoliday(any());
    }

    @Test
    void should_return_event_date_4_pm_plus_delay_duration_when_delay_date_not_matches_holiday() {
        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        int delayDuration = 2;

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration)))
            .thenReturn(false);

        ZonedDateTime expectedDelayDate = eventDateTime.plusDays(delayDuration);
        ZonedDateTime expectedDelayDateTime = expectedDelayDate.with(LocalTime.of(16, 0, 0, 0));
        ZonedDateTime actualDateTime = dueDateService.calculateDelayUntil(eventDateTime, delayDuration);

        assertThat(actualDateTime, is(expectedDelayDateTime));
        verify(holidayService, times(1)).isHoliday(any());
    }

}

