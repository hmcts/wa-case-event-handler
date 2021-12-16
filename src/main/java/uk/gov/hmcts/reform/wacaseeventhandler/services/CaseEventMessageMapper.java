package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;

@Component
public class CaseEventMessageMapper {

    public CaseEventMessage mapToCaseEventMessage(CaseEventMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CaseEventMessage(
            entity.getMessageId(),
            entity.getSequence(),
            entity.getCaseId(),
            entity.getEventTimestamp(),
            entity.getFromDlq(),
            entity.getState(),
            entity.getMessageProperties(),
            entity.getMessageContent(),
            entity.getReceived(),
            entity.getDeliveryCount(),
            entity.getHoldUntil(),
            entity.getRetryCount());
    }
}
