package uk.gov.hmcts.reform.wacaseeventhandler.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "wa_case_event_messages")
@Entity
@EqualsAndHashCode
@ToString
@TypeDef(name = "json", typeClass = JsonType.class)
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
public class CaseEventMessageEntity {
    private static final String MESSAGE_ID = "message_id";
    private static final String CASE_ID = "case_id";
    private static final String EVENT_TIMESTAMP = "event_timestamp";
    private static final String FROM_DLQ = "from_dlq";
    private static final String MESSAGE_PROPERTIES = "message_properties";
    private static final String MESSAGE_CONTENT = "message_content";
    private static final String DELIVERY_COUNT = "delivery_count";
    private static final String HOLD_UNTIL = "hold_until";
    private static final String RETRY_COUNT = "retry_count";

    @Column(name = MESSAGE_ID, nullable = false)
    private String messageId;

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequence;

    @Column(name = CASE_ID, nullable = false)
    private String caseId;

    @Column(name = EVENT_TIMESTAMP)
    private LocalDateTime eventTimestamp;

    @Column(name = FROM_DLQ, nullable = false)
    private Boolean fromDlq;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "message_state_enum")
    @Type(type = "pgsql_enum")
    private MessageState state;

    @Column(name = MESSAGE_PROPERTIES, columnDefinition = "jsonb")
    @Convert(disableConversion = true)
    @Type(type = "json")
    private JsonNode messageProperties;

    @Column(name = MESSAGE_CONTENT)
    private String messageContent;

    @Column(nullable = false)
    private LocalDateTime received;

    @Column(name = DELIVERY_COUNT, nullable = false)
    private Integer deliveryCount;

    @Column(name = HOLD_UNTIL)
    private LocalDateTime holdUntil;

    @Column(name = RETRY_COUNT, nullable = false)
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
