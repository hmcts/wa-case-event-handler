package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class EventMessageQueryResponse implements Serializable {
    private static final long serialVersionUID = -4503912338669550168L;

    @JsonProperty("message")
    private String message;
    @JsonProperty("totalNumberOfMessagesInTheDB")
    private long totalNumberOfMessages;
    @JsonProperty("numberOfMessagesMatchingTheQuery")
    private long numberOfMessagesFound;
    @JsonProperty("caseEventMessages")
    private List<CaseEventMessage> caseEventMessages;

    public EventMessageQueryResponse() {
        // This constructor is intentionally empty for Json deserialization
    }

    public EventMessageQueryResponse(String message,
                                     long totalNumberOfMessages,
                                     long numberOfMessagesFound,
                                     List<CaseEventMessage> caseEventMessages) {
        this.message = message;
        this.totalNumberOfMessages = totalNumberOfMessages;
        this.numberOfMessagesFound = numberOfMessagesFound;
        this.caseEventMessages = caseEventMessages;
    }

    public String getMessage() {
        return message;
    }

    public long getTotalNumberOfMessages() {
        return totalNumberOfMessages;
    }

    public long getNumberOfMessagesFound() {
        return numberOfMessagesFound;
    }

    public List<CaseEventMessage> getCaseEventMessages() {
        return caseEventMessages;
    }
}
