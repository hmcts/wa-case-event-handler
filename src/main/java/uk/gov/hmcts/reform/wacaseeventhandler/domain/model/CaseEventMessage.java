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
    private String messageId;
    @JsonProperty("Sequence")
    private Long sequence;
    @JsonProperty("CaseId")
    private String caseId;
    @JsonProperty("EventTimestamp")
    private LocalDateTime eventTimestamp;
    @JsonProperty("FromDlq")
    private Boolean fromDlq;
    @JsonProperty("State")
    private MessageState state;
    @JsonProperty("MessageProperties")
    private JsonNode messageProperties;
    @JsonProperty("MessageContent")
    private String messageContent;
    @JsonProperty("Received")
    private LocalDateTime received;
    @JsonProperty("DeliveryCount")
    private Integer deliveryCount;
    @JsonProperty("HoldUntil")
    private LocalDateTime holdUntil;
    @JsonProperty("RetryCount")
    private Integer retryCount;

    public CaseEventMessage() {
    }

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
