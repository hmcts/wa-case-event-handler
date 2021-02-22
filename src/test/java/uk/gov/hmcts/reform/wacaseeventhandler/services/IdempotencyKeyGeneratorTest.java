package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdempotencyKeyGeneratorTest {

    IdempotencyKeyGenerator idempotencyKeyGenerator = new IdempotencyKeyGenerator();

    @Test
    void should_generate_the_same_hashcode() {

        String messageId = "messageId";
        String eventId = "eventId";

        String result1 = idempotencyKeyGenerator.generateIdempotencyKey(messageId, eventId);
        String result2 = idempotencyKeyGenerator.generateIdempotencyKey(messageId, eventId);

        assertEquals(result1, result2);
    }

    @Test
    void should_generate_different_hashcodes_for_same_event_but_different_messageId() {


        String result1 = idempotencyKeyGenerator.generateIdempotencyKey("messageId1", "eventId");
        String result2 = idempotencyKeyGenerator.generateIdempotencyKey("messageId2", "eventId");

        assertNotEquals(result1, result2);
    }

    @Test
    void should_generate_different_hashcodes_for_same_message_but_different_events() {

        String result1 = idempotencyKeyGenerator.generateIdempotencyKey("messageId", "eventId1");
        String result2 = idempotencyKeyGenerator.generateIdempotencyKey("messageId", "eventId2");

        assertNotEquals(result1, result2);
    }
}
