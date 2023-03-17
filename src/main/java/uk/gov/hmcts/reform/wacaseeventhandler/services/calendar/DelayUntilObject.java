package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@NoArgsConstructor
public class DelayUntilObject {

    private String delayUntil;
    private String delayUntilTime;
    private String delayUntilOrigin;
    private String delayUntilIntervalDays;
    private String delayUntilNonWorkingCalendar;
    private String delayUntilNonWorkingDaysOfWeek;
    private String delayUntilSkipNonWorkingDays;
    private String delayUntilMustBeWorkingDay;

    @JsonCreator
    public DelayUntilObject(
        @JsonProperty("delayUntil")
        String delayUntil,
        @JsonProperty("delayUntilTime")
        String delayUntilTime,
        @JsonProperty("delayUntilOrigin")
        String delayUntilOrigin,
        @JsonProperty("delayUntilIntervalDays")
        String delayUntilIntervalDays,
        @JsonProperty("delayUntilNonWorkingCalendar")
        String delayUntilNonWorkingCalendar,
        @JsonProperty("delayUntilNonWorkingDaysOfWeek")
        String delayUntilNonWorkingDaysOfWeek,
        @JsonProperty("delayUntilSkipNonWorkingDays")
        String delayUntilSkipNonWorkingDays,
        @JsonProperty("delayUntilMustBeWorkingDay")
        String delayUntilMustBeWorkingDay) {
        this.delayUntil = delayUntil;
        this.delayUntilTime = delayUntilTime;
        this.delayUntilOrigin = delayUntilOrigin;
        this.delayUntilIntervalDays = delayUntilIntervalDays;
        this.delayUntilNonWorkingCalendar = delayUntilNonWorkingCalendar;
        this.delayUntilNonWorkingDaysOfWeek = delayUntilNonWorkingDaysOfWeek;
        this.delayUntilSkipNonWorkingDays = delayUntilSkipNonWorkingDays;
        this.delayUntilMustBeWorkingDay = delayUntilMustBeWorkingDay;
    }
}
