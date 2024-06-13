package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;

@ToString
public class ProblemMessage {
    private final String messageId;
    private final String caseId;
    private final String caseTypeId;
    private final LocalDateTime eventTimestamp;
    private final Boolean fromDlq;
    private final MessageState state;

    public ProblemMessage(@JsonProperty("MessageId") String messageId,
                          @JsonProperty("CaseId") String caseId,
                          @JsonProperty("CaseTypeId") String caseTypeId,
                          @JsonProperty("EventTimestamp") LocalDateTime eventTimestamp,
                          @JsonProperty("FromDlq") Boolean fromDlq,
                          @JsonProperty("State")MessageState state) {
        this.messageId = messageId;
        this.caseId = caseId;
        this.caseTypeId = caseTypeId;
        this.eventTimestamp = eventTimestamp;
        this.fromDlq = fromDlq;
        this.state = state;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public Boolean getFromDlq() {
        return fromDlq;
    }

    public MessageState getState() {
        return state;
    }
}
