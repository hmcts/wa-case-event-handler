package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.io.Serializable;
import java.time.LocalDateTime;

@SuppressWarnings("PMD.ExcessiveParameterList")
public class CaseEventMessage implements Serializable {

    private static final long serialVersionUID = 3213665975741833471L;

    @JsonProperty("MessageId")
    private final String messageId;
    @JsonProperty("Sequence")
    private final Long sequence;
    @JsonProperty("CaseId")
    private final String caseId;
    @JsonProperty("EventTimestamp")
    private final LocalDateTime eventTimestamp;
    @JsonProperty("FromDlq")
    private final Boolean fromDlq;
    @JsonProperty("State")
    private final MessageState state;
    @JsonProperty("MessageProperties")
    private final JsonNode messageProperties;
    @JsonProperty("MessageContent")
    private final String messageContent;
    @JsonProperty("Received")
    private final LocalDateTime received;
    @JsonProperty("DeliveryCount")
    private final Integer deliveryCount;
    @JsonProperty("HoldUntil")
    private final LocalDateTime holdUntil;
    @JsonProperty("RetryCount")
    private final Integer retryCount;

    public CaseEventMessage(String messageId, Long sequence, String caseId, LocalDateTime eventTimestamp,
                            Boolean fromDlq, MessageState state, JsonNode messageProperties, String messageContent,
                            LocalDateTime received, Integer deliveryCount, LocalDateTime holdUntil,
                            Integer retryCount) {
        this.messageId = messageId;
        this.sequence = sequence;
        this.caseId = caseId;
        this.eventTimestamp = eventTimestamp;
        this.fromDlq = fromDlq;
        this.state = state;
        this.messageProperties = messageProperties;
        this.messageContent = messageContent;
        this.received = received;
        this.deliveryCount = deliveryCount;
        this.holdUntil = holdUntil;
        this.retryCount = retryCount;
    }

    public String getMessageId() {
        return messageId;
    }

    public Long getSequence() {
        return sequence;
    }

    public String getCaseId() {
        return caseId;
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

    public JsonNode getMessageProperties() {
        return messageProperties;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public LocalDateTime getReceived() {
        return received;
    }

    public Integer getDeliveryCount() {
        return deliveryCount;
    }

    public LocalDateTime getHoldUntil() {
        return holdUntil;
    }

    public Integer getRetryCount() {
        return retryCount;
    }
}
