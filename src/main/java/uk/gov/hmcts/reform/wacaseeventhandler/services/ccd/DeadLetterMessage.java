package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DeadLetterMessage {

    private final String originalMessage;

    private final String errorDescription;

    @JsonCreator
    public DeadLetterMessage(@JsonProperty("OriginalMessage") String originalMessage,
                             @JsonProperty("ErrorDescription") String errorDescription) {
        this.originalMessage = originalMessage;
        this.errorDescription = errorDescription;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
