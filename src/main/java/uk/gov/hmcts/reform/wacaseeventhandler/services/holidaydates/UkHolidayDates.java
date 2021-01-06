package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class UkHolidayDates {
    @JsonProperty("england-and-wales")
    private CountryHolidayDates englandAndWales;

    private UkHolidayDates() {
    }

    public UkHolidayDates(CountryHolidayDates englandAndWales) {
        this.englandAndWales = englandAndWales;
    }

    public CountryHolidayDates getEnglandAndWales() {
        return englandAndWales;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        UkHolidayDates that = (UkHolidayDates) object;
        return Objects.equals(englandAndWales, that.englandAndWales);
    }

    @Override
    public int hashCode() {
        return Objects.hash(englandAndWales);
    }

    @Override
    public String toString() {
        return "UkHolidayDates{"
               + "englandAndWales=" + englandAndWales
               + '}';
    }
}
