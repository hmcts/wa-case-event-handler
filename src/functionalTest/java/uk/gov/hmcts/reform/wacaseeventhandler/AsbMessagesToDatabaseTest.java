package uk.gov.hmcts.reform.wacaseeventhandler;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class AsbMessagesToDatabaseTest extends MessagingTests {

    public static List<CaseEventMessage> caseEventMessages;

    @AfterEach
    public void tearDown() {
        deleteMessagesFromDatabase(caseEventMessages);
    }

    @BeforeEach
    public void setup() {
        caseEventMessages = new ArrayList<>();
    }

    @Test
    public void should_store_messages_in_database() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId(), randomMessageId());

        String caseId = getWaCaseId();

        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventTimeStamp(LocalDateTime.now())
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("insert_true")
            .caseTypeId("wacasetype")
            .build();

        Map<String, EventInformation> messages = new HashMap<>();
        messageIds.forEach(msgId -> messages.put(msgId, eventInformation));
        sendMessagesToTopic(messages);

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

                        Assertions.assertTrue(caseEventMessages.stream().map(CaseEventMessage::getMessageId)
                                                  .allMatch(messageIds::contains), "messageId mismatch");

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
        var caseId = getWaCaseId();

        //eventTimeStamp deliberately missing field
        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("insert_true")
            .caseTypeId("wacasetype")
            .build();

        sendMessageToTopic(randomMessageId(), eventInformation);

        await().ignoreException(AssertionError.class)
            .pollInterval(3, MILLISECONDS)
            .atMost(120, SECONDS)
            .until(() -> {
                final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, false);
                if (dlqMessagesFromDb != null) {
                    caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                    Assertions.assertEquals(1, caseEventMessages.size());
                    Assertions.assertEquals(MessageState.UNPROCESSABLE, caseEventMessages.get(0).getState());

                    return true;
                } else {
                    return false;
                }
            });
    }
}
