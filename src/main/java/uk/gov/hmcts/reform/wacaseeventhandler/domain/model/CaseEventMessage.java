package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Boolean getFromDlq() {
        return fromDlq;
    }

    public void setFromDlq(Boolean fromDlq) {
        this.fromDlq = fromDlq;
    }

    public MessageState getState() {
        return state;
    }

    public void setState(MessageState state) {
        this.state = state;
    }

    public JsonNode getMessageProperties() {
        return messageProperties;
    }

    public void setMessageProperties(JsonNode messageProperties) {
        this.messageProperties = messageProperties;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public LocalDateTime getReceived() {
        return received;
    }

    public void setReceived(LocalDateTime received) {
        this.received = received;
    }

    public Integer getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(Integer deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public LocalDateTime getHoldUntil() {
        return holdUntil;
    }

    public void setHoldUntil(LocalDateTime holdUntil) {
        this.holdUntil = holdUntil;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
}
