package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Locale;
import javax.validation.constraints.NotEmpty;

@ToString
@EqualsAndHashCode
@Builder
public final class EventInformation {

    @NotEmpty
    private final String eventInstanceId;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private final LocalDateTime dateTime;
    @NotEmpty
    private final String caseReference;
    @NotEmpty
    private final String jurisdictionId;
    @NotEmpty
    private final String caseTypeId;
    @NotEmpty
    private final String eventId;
    private final String previousStateId;
    private final String newStateId;
    @NotEmpty
    private final String userId;

    @JsonCreator
    public EventInformation(@JsonProperty("eventInstanceId") String eventInstanceId,
                            @JsonProperty("dateTime")LocalDateTime dateTime,
                            @JsonProperty("caseReference") String caseReference,
                            @JsonProperty("jurisdictionId") String jurisdictionId,
                            @JsonProperty("caseTypeId") String caseTypeId,
                            @JsonProperty("eventId") String eventId,
                            @JsonProperty("previousStateId") String previousStateId,
                            @JsonProperty("newStateId") String newStateId,
                            @JsonProperty("userId") String userId) {
        this.eventInstanceId = eventInstanceId;
        this.dateTime = dateTime;
        this.caseReference = caseReference;
        this.jurisdictionId = jurisdictionId.toLowerCase(Locale.ENGLISH);
        this.caseTypeId = caseTypeId.toLowerCase(Locale.ENGLISH);
        this.eventId = eventId;
        this.previousStateId = previousStateId;
        this.newStateId = newStateId;
        this.userId = userId;
    }

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getEventInstanceId() {
        return eventInstanceId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
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
