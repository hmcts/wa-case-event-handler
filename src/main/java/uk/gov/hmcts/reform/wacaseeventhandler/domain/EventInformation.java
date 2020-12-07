package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import javax.validation.constraints.NotEmpty;

@Builder
@ToString
@EqualsAndHashCode
public class EventInformation {

    @NotEmpty
    private final String eventInstanceId;
    private final LocalDateTime dueTime;
    @NotEmpty
    private final String caseReference;
    @NotEmpty
    private final String jurisdictionId;
    @NotEmpty
    private final String caseTypeId;
    @NotEmpty
    private final String eventId;
    private final String previousStateId;
    @NotEmpty
    private final String newStateId;
    @NotEmpty
    private final String userId;

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getEventInstanceId() {
        return eventInstanceId;
    }

    public LocalDateTime getDueTime() {
        return dueTime;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPreviousStateId() {
        return previousStateId;
    }

    public String getNewStateId() {
        return newStateId;
    }

    public String getUserId() {
        return userId;
    }
}
