package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import lombok.Builder;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;

@Builder
public class MessageUpdateRetry {
    private String messageId;
    private MessageState state;
    private LocalDateTime holdUntil;
    private Integer retryCount;

    public String getMessageId() {
        return messageId;
    }

    public MessageState getState() {
        return state;
    }

    public LocalDateTime getHoldUntil() {
        return holdUntil;
    }

    public Integer getRetryCount() {
        return retryCount;
    }
}
