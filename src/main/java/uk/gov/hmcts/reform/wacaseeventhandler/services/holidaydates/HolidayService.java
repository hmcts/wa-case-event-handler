package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class HolidayService {
    private final List<LocalDate> holidays;

    public HolidayService(List<LocalDate> holidays) {
        this.holidays = holidays;
    }

    public boolean isHoliday(ZonedDateTime zonedDateTime) {
        return holidays.contains(zonedDateTime.toLocalDate());
    }
}
