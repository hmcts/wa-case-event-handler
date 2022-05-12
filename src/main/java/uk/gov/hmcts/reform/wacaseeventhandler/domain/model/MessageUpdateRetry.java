package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageUpdateRetry {
    private String messageId;
    private MessageState state;
    private LocalDateTime holdUntil;
    private Integer retryCount;
}
