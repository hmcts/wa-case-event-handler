package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import java.time.LocalDate;
import java.util.Objects;

public class HolidayDate {
    private LocalDate date;

    private HolidayDate() {
    }

    public HolidayDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        HolidayDate that = (HolidayDate) object;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return "HolidayDate{"
               + "date='" + date + '\''
               + '}';
    }
}
