package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        CountryHolidayDates that = (CountryHolidayDates) object;
        return Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(events);
    }

    @Override
    public String toString() {
        return "CountryHolidayDates{"
               + "events=" + events
               + '}';
    }
}
