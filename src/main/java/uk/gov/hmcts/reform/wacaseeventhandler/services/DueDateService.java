package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.LocalTime;
import java.time.ZonedDateTime;

@Component
public class DueDateService {

    private final HolidayService holidayService;

    public DueDateService(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    public ZonedDateTime calculateDelayUntil(ZonedDateTime eventDateTime, int delayDuration) {
        if (delayDuration <= 0) {
            return eventDateTime;
        }

        final ZonedDateTime zonedDateTime = addWorkingDaysForDelayDuration(eventDateTime, delayDuration);

        return resetTo4PmTime(zonedDateTime);
    }

    public ZonedDateTime calculateDueDate(ZonedDateTime delayUntil, int workingDaysAllowed) {
        final ZonedDateTime zonedDateTime = addWorkingDays(delayUntil, workingDaysAllowed);

        return resetTo4PmTime(zonedDateTime);
    }

    private ZonedDateTime addWorkingDays(ZonedDateTime dueDate, int numberOfDays) {
        if (numberOfDays == 0) {
            return dueDate;
        }

        ZonedDateTime newDate = dueDate.plusDays(1);
        if (holidayService.isWeekend(newDate) || holidayService.isHoliday(newDate)) {
            return addWorkingDays(newDate, numberOfDays);
        } else {
            return addWorkingDays(newDate, numberOfDays - 1);
        }
    }

    private ZonedDateTime addWorkingDaysForDelayDuration(ZonedDateTime eventDate, int delayDuration) {
        if (delayDuration == 0) {
            return eventDate;
        }

        ZonedDateTime newDate = eventDate;
        for (int remaining = delayDuration; remaining > 0; ) {
            newDate = newDate.plusDays(1);
            if (!(holidayService.isWeekend(newDate) || holidayService.isHoliday(newDate))) {
                remaining--;
            }
        }

        return newDate;
    }

    private ZonedDateTime resetTo4PmTime(ZonedDateTime eventDateTime) {
        final LocalTime fourPmTime = LocalTime.of(16, 0, 0, 0);

        return ZonedDateTime.of(eventDateTime.toLocalDate(), fourPmTime, eventDateTime.getZone());
    }
}
