package uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Getter
public class DelayUntilIntervalData {

    private Long intervalDays;
    private LocalDateTime referenceDate;
    private List<String> nonWorkingCalendars;
    private List<String> nonWorkingDaysOfWeek;
    private boolean skipNonWorkingDays;
    private String mustBeWorkingDay;
    private String delayUntilTime;
    public static final String MUST_BE_WORKING_DAY_NEXT = "Next";
    public static final String MUST_BE_WORKING_DAY_PREVIOUS = "Previous";
    public static final String MUST_BE_WORKING_DAY_NO = "No";
}
