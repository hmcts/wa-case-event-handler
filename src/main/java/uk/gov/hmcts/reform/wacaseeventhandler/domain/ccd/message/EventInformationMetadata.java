package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.Map;

@ToString
@EqualsAndHashCode
public final class EventInformationMetadata {

    private final Map<String, String> messageProperties;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private final LocalDateTime holdUntil;

    @JsonCreator
    public EventInformationMetadata(@JsonProperty("MessageProperties") Map<String, String> messageProperties,
                                    @JsonProperty("HoldUntil") LocalDateTime holdUntil) {
        this.messageProperties = messageProperties;
        this.holdUntil = holdUntil;
    }

    public Map<String, String> getMessageProperties() {
        return messageProperties;
    }

    public LocalDateTime getHoldUntil() {
        return holdUntil;
    }
}
