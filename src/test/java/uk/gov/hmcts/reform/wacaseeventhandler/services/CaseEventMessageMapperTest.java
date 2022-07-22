package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.ProblemMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CaseEventMessageMapperTest {
    private static final LocalDateTime EVENT_TIME_STAMP = LocalDateTime.now();
    private static final LocalDateTime RECEIVED = LocalDateTime.now();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CaseEventMessageMapper mapper = new CaseEventMessageMapper();

    @Test
    void shouldMapEntity() throws Exception {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();

        entity.setMessageId("messageId_123");
        entity.setSequence(4L);
        entity.setCaseId("12345");
        entity.setEventTimestamp(EVENT_TIME_STAMP);
        entity.setFromDlq(true);
        entity.setState(MessageState.NEW);
        entity.setMessageProperties(objectMapper.readValue("{\"Property1\":\"Test\"}", JsonNode.class));
        entity.setMessageContent("{\"CaseId\":\"12345\",\"MessageProperties\":{\"Property1\":\"Test\"}}");
        entity.setReceived(RECEIVED);
        entity.setDeliveryCount(1);
        entity.setHoldUntil(RECEIVED.plusDays(2));
        entity.setRetryCount(2);

        CaseEventMessage message = mapper.mapToCaseEventMessage(entity);

        assertEquals(entity.getMessageId(), message.getMessageId());
        assertEquals(entity.getSequence(), message.getSequence());
        assertEquals(entity.getCaseId(), message.getCaseId());
        assertEquals(entity.getEventTimestamp(), message.getEventTimestamp());
        assertEquals(entity.getFromDlq(), message.getFromDlq());
        assertEquals(entity.getState(), message.getState());
        assertEquals(entity.getMessageProperties(), message.getMessageProperties());
        assertEquals(entity.getMessageContent(), message.getMessageContent());
        assertEquals(entity.getReceived(), message.getReceived());
        assertEquals(entity.getDeliveryCount(), message.getDeliveryCount());
        assertEquals(entity.getHoldUntil(), message.getHoldUntil());
        assertEquals(entity.getMessageId(), message.getMessageId());
        assertEquals(entity.getRetryCount(), message.getRetryCount());
    }

    @Test
    void shouldMapNullEntity() {
        assertNull(mapper.mapToCaseEventMessage(null));
    }

    @Test
    void shouldMapEntityToProblemMessage() throws Exception {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();

        entity.setMessageId("messageId_123");
        entity.setSequence(4L);
        entity.setCaseId("12345");
        entity.setEventTimestamp(EVENT_TIME_STAMP);
        entity.setFromDlq(true);
        entity.setState(MessageState.NEW);
        entity.setMessageProperties(objectMapper.readValue("{\"Property1\":\"Test\"}", JsonNode.class));
        entity.setMessageContent("{\"CaseId\":\"12345\",\"MessageProperties\":{\"Property1\":\"Test\"}}");
        entity.setReceived(RECEIVED);
        entity.setDeliveryCount(1);
        entity.setHoldUntil(RECEIVED.plusDays(2));
        entity.setRetryCount(2);

        ProblemMessage message = mapper.mapToProblemMessage(entity);

        assertEquals(entity.getMessageId(), message.getMessageId());
        assertEquals(entity.getCaseId(), message.getCaseId());
        assertEquals(entity.getEventTimestamp(), message.getEventTimestamp());
        assertEquals(entity.getFromDlq(), message.getFromDlq());
        assertEquals(entity.getState(), message.getState());
    }

    @Test
    void shouldMapNullEntityToProblemMessage() {
        assertNull(mapper.mapToProblemMessage(null));
    }
}
