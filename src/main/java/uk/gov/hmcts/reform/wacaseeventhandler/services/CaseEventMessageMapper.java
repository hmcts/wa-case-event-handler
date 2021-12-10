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

        CaseEventMessage caseEventMessage = new CaseEventMessage();
        caseEventMessage.setMessageId(entity.getMessageId());
        caseEventMessage.setSequence(entity.getSequence());
        caseEventMessage.setCaseId(entity.getCaseId());
        caseEventMessage.setEventTimestamp(entity.getEventTimestamp());
        caseEventMessage.setFromDlq(entity.getFromDlq());
        caseEventMessage.setState(entity.getState());
        caseEventMessage.setMessageProperties(entity.getMessageProperties());
        caseEventMessage.setMessageContent(entity.getMessageContent());
        caseEventMessage.setReceived(entity.getReceived());
        caseEventMessage.setDeliveryCount(entity.getDeliveryCount());
        caseEventMessage.setHoldUntil(entity.getHoldUntil());
        caseEventMessage.setRetryCount(entity.getRetryCount());

        return caseEventMessage;
    }
}
