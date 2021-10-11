package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
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

}
