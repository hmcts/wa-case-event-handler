package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
public class DelayUntilObject {

    private String delayUntil;
    private String delayUntilTime;
    private String  delayUntilOrigin;
    private Integer delayUntilIntervalDays;
    private String delayUntilNonWorkingCalendar;
    private String delayUntilNonWorkingDaysOfWeek;
    private Boolean delayUntilSkipNonWorkingDays;
    private String delayUntilMustBeWorkingDay;
}
