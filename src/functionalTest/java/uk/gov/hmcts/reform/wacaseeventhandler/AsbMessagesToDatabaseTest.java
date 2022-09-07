package uk.gov.hmcts.reform.wacaseeventhandler;


import org.junit.Test;
import org.junit.jupiter.api.Assertions;
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


public class AsbMessagesToDatabaseTest extends MessagingTests {

    public static List<CaseEventMessage> caseEventMessages = null;


    @Test
    public void should_store_messages_in_database() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId(), randomMessageId());

        String caseId = randomCaseId();

        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .eventTimeStamp(LocalDateTime.now())
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("insert_true")
            .caseTypeId("caseTypeId")
            .build();

        messageIds.forEach(msgId -> sendMessageToTopic(msgId, eventInformation));

        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(120, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, false);
                    if (dlqMessagesFromDb != null) {
                        caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                        Assertions.assertEquals(messageIds.size(), caseEventMessages.size(),
                                                "Number of messages stored in database does not match");

                        //TODO: check messages IDs match those created
                        /*  Assertions.assertTrue(caseEventMessages.stream().map(CaseEventMessage::getState)
                                       .allMatch(e -> MessageState.READY == e),
                                              "Not all messages were in READY state: " + caseEventMessages);*/

                        Assertions.assertTrue(caseEventMessages.stream().noneMatch(CaseEventMessage::getFromDlq),
                                              "None of the messages stored in DB should be in DLQ state");


                        return true;
                    } else {
                        return false;
                    }
                });

        deleteMessagesFromDatabase(caseEventMessages);
    }

    @Test
    public void should_store_messages_missing_mandatory_fields_in_database_as_unprocessable() {
        var caseId = randomCaseId();

        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("insert_true")
            .caseTypeId("caseTypeId")
            .build();

        sendMessageToTopic(randomMessageId(), eventInformation);

        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
            .until(() -> {
                final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, false);
                if (dlqMessagesFromDb != null) {
                    final List<CaseEventMessage> caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                    Assertions.assertEquals(1, caseEventMessages.size());
                    Assertions.assertEquals(MessageState.UNPROCESSABLE, caseEventMessages.get(0).getState());
                    deleteMessagesFromDatabase(caseEventMessages);
                    return true;
                } else {
                    return false;
                }
            });
    }
}
