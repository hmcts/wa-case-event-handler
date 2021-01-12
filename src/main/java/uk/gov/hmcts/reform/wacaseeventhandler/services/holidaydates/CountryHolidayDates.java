package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@ToString
public class CountryHolidayDates {
    private List<HolidayDate> events;

    private CountryHolidayDates() {
    }

    public CountryHolidayDates(List<HolidayDate> events) {
        this.events = events;
    }

    public List<HolidayDate> getEvents() {
        return events;
    }

}
