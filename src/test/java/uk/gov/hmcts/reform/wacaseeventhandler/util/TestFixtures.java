package uk.gov.hmcts.reform.wacaseeventhandler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TestFixtures {

    private static final String MESSAGE_ID = UUID.randomUUID().toString();
    public static final String USER_ID = "myUserId";

    private TestFixtures() {

    }

    public static CaseEventMessage createCaseEventMessage(EventInformation eventInformation)
            throws JsonProcessingException {

        final String messageContent = new ObjectMapper().writeValueAsString(eventInformation);

        return new CaseEventMessage(MESSAGE_ID, 0L, "caseId", LocalDateTime.now(), Boolean.FALSE, MessageState.READY,
                NullNode.getInstance(), messageContent, LocalDateTime.now(), 0, LocalDateTime.now(), 0);
    }

    public static CaseEventMessage createCaseEventMessage() {
        return createCaseEventMessage("messageContent", 0);
    }

    public static CaseEventMessage createCaseEventMessage(int retryCount) {
        return createCaseEventMessage("messageContent", retryCount);
    }

    private static CaseEventMessage createCaseEventMessage(String messageContent, int retryCount) {
        return new CaseEventMessage(MESSAGE_ID, 0L, "caseId", LocalDateTime.now(), Boolean.FALSE, MessageState.READY,
                NullNode.getInstance(), messageContent, LocalDateTime.now(), 0, LocalDateTime.now(), retryCount);
    }

    public static CaseEventMessageEntity createCaseEventMessageEntity() throws JsonProcessingException {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(UUID.randomUUID().toString());
        caseEventMessageEntity.setCaseId(UUID.randomUUID().toString());
        caseEventMessageEntity.setEventTimestamp(LocalDateTime.now());
        caseEventMessageEntity.setFromDlq(false);
        caseEventMessageEntity.setState(MessageState.NEW);
        caseEventMessageEntity.setReceived(LocalDateTime.now());
        caseEventMessageEntity.setDeliveryCount(0);
        caseEventMessageEntity.setRetryCount(0);
        caseEventMessageEntity.setMessageContent(String.format("{\"UserId\": \"%s\"}", USER_ID));
        caseEventMessageEntity.setMessageProperties(new ObjectMapper().readTree("{\"property1\":\"test1\"}"));
        caseEventMessageEntity.setHoldUntil(LocalDateTime.now());
        caseEventMessageEntity.setSequence(10L);
        return caseEventMessageEntity;
    }
}
