package uk.gov.hmcts.reform.wacaseeventhandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;


public class AsbMessagesToDatabaseTest extends MessagingTests {

    public static List<CaseEventMessage> caseEventMessages = new ArrayList<>();

    @After
    public void tearDown() {
        deleteMessagesFromDatabase(caseEventMessages);
    }

    @Before
    public void setup() {
        caseEventMessages = new ArrayList<>();
    }

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

                        caseEventMessages.forEach(msg ->
                                                    Assertions.assertTrue(messageIds.contains(msg.getMessageId()),
                                                                          "messageId mismatch"));

                        Assertions.assertTrue(caseEventMessages.stream().noneMatch(CaseEventMessage::getFromDlq),
                                              "None of the messages stored in DB should be in DLQ state");

                        return true;
                    } else {
                        return false;
                    }
                });
    }

    @Test
    public void should_store_messages_missing_mandatory_fields_in_database_as_unprocessable() {
        var caseId = randomCaseId();

        //eventTimeStamp deliberately missing field
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
            .pollInterval(3, MILLISECONDS)
            .atMost(120, SECONDS)
            .until(() -> {
                final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, false);
                if (dlqMessagesFromDb != null) {
                    final List<CaseEventMessage> caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                    Assertions.assertEquals(1, caseEventMessages.size());
                    Assertions.assertEquals(MessageState.UNPROCESSABLE, caseEventMessages.get(0).getState());

                    return true;
                } else {
                    return false;
                }
            });
    }
}
