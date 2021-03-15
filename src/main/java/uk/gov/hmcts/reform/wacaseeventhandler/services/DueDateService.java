package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Component
public class DueDateService {

    private final HolidayService holidayService;

    public DueDateService(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    public ZonedDateTime calculateDelayUntil(ZonedDateTime eventDateTime, int delayDuration) {
        final ZonedDateTime fourPmDateTime = resetTo4PmTime(eventDateTime);
        if (delayDuration == 0) {
            return fourPmDateTime;
        }

        return fourPmDateTime.plusDays(delayDuration);
    }

    public ZonedDateTime calculateDueDate(ZonedDateTime delayUntil, int workingDaysAllowed) {
        return addWorkingDays(delayUntil, workingDaysAllowed);
    }

    private ZonedDateTime addWorkingDays(ZonedDateTime dueDate, int numberOfDays) {
        if (numberOfDays == 0) {
            return dueDate;
        }

        ZonedDateTime newDate = dueDate.plusDays(1);
        if (isWeekend(newDate) || holidayService.isHoliday(newDate)) {
            return addWorkingDays(newDate, numberOfDays);
        } else {
            return addWorkingDays(newDate, numberOfDays - 1);
        }
    }

    private boolean isWeekend(ZonedDateTime date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private ZonedDateTime resetTo4PmTime(ZonedDateTime eventDateTime) {
        final LocalTime fourPmTime = LocalTime.of(16, 0, 0, 0);

        return eventDateTime.of(eventDateTime.toLocalDate(), fourPmTime, eventDateTime.getZone());
    }
}
