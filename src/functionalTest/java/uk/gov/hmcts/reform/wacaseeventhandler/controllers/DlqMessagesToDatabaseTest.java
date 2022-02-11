package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wacaseeventhandler.MessagingTests;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@ActiveProfiles(profiles = {"local", "functional"})
public class DlqMessagesToDatabaseTest extends MessagingTests {

    @Test
    public void should_store_dlq_messages_in_database() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId(), randomMessageId());

        var caseId = randomCaseId();

        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .eventTimeStamp(LocalDateTime.now())
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("insert_true")
            .caseTypeId("caseTypeId")
            .build();

        messageIds.forEach(msgId ->
            sendMessageToDlq(msgId, eventInformation)
        );

        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, true);
                    if (dlqMessagesFromDb != null) {
                        final List<CaseEventMessage> caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                        assertEquals(messageIds.size(), caseEventMessages.size());
                        assertTrue(caseEventMessages.stream().allMatch(CaseEventMessage::getFromDlq));

                        deleteMessagesFromDatabase(caseEventMessages);
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    @Test
    public void should_store_dlq_messages_missing_mandatory_fields_in_database_as_unprocessable() {

        var caseId = randomCaseId();

        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("insert_true")
            .caseTypeId("caseTypeId")
            .build();

        sendMessageToDlq(randomMessageId(), eventInformation);

        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
            .until(() -> {
                final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, true);
                if (dlqMessagesFromDb != null) {
                    final List<CaseEventMessage> caseEventMessages
                        = dlqMessagesFromDb.getCaseEventMessages();

                    assertEquals(1, caseEventMessages.size());
                    assertEquals(MessageState.UNPROCESSABLE, caseEventMessages.get(0).getState());
                    deleteMessagesFromDatabase(caseEventMessages);
                    return true;
                } else {
                    return false;
                }
            });
    }

}