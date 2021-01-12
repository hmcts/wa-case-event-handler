package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;

@Component
public class DueDateService {

    private final DateService dateService;
    private final HolidayService holidayService;

    public DueDateService(DateService dateService, HolidayService holidayService) {
        this.dateService = dateService;
        this.holidayService = holidayService;
    }

    public ZonedDateTime calculateDueDate(ZonedDateTime dueDate, int workingDaysAllowed) {
        if (dueDate != null) {
            return dueDate;
        }
        if (workingDaysAllowed == 0) {
            throw new IllegalStateException(
                "Should either have a due date or have got the working days allowed for task"
            );
        }
        return addWorkingDays(workingDaysAllowed);
    }

    public ZonedDateTime addWorkingDays(int numberOfDays) {
        return addWorkingDays(dateService.now(), numberOfDays);
    }

    private ZonedDateTime addWorkingDays(ZonedDateTime startDate, int numberOfDays) {
        if (numberOfDays == 0) {
            return startDate;
        }

        ZonedDateTime newDate = startDate.plusDays(1);
        if (isWeekend(newDate) || holidayService.isHoliday(newDate)) {
            return addWorkingDays(newDate, numberOfDays);
        } else {
            return addWorkingDays(newDate, numberOfDays - 1);
        }
    }

    private boolean isWeekend(ZonedDateTime date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
