package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
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

}
