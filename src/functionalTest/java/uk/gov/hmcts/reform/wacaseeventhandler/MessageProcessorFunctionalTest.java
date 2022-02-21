package uk.gov.hmcts.reform.wacaseeventhandler;

import org.junit.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageProcessorFunctionalTest extends MessagingTests {

    @Test
    public void should_process_message_with_the_lowest_event_timestamp_for_that_case() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId(), randomMessageId());

        String caseId = randomCaseId();

        final EventInformation.EventInformationBuilder eventInformationBuilder = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("wa-dlq-user@fake.hmcts.net")
            .newStateId("appealSubmitted")
            .caseTypeId("Asylum");

        messageIds.forEach(msgId -> {
            final EventInformation eventInformation =
                    eventInformationBuilder
                            .eventTimeStamp(LocalDateTime.now())
                            .build();
            sendMessageToTopic(msgId, eventInformation);
            waitSeconds(3);
        });

        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(120, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, false);
                    if (dlqMessagesFromDb != null) {
                        final List<CaseEventMessage> caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                        assertEquals(messageIds.size(), caseEventMessages.size());
                        assertTrue(caseEventMessages.stream()
                                .anyMatch(x -> x.getMessageId().equals(messageIds.get(0))
                                        && x.getState() == MessageState.PROCESSED));
                        deleteMessagesFromDatabase(caseEventMessages);
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    @Test
    public void should_process_dlq_msg_if_processed_or_ready_messages_with_timestamp_later_than_thirty_mins_exist() {
        String caseId = randomCaseId();

        final EventInformation.EventInformationBuilder eventInformationBuilder = EventInformation.builder()
                .eventInstanceId(UUID.randomUUID().toString())
                .jurisdictionId("IA")
                .eventId("makeAnApplication")
                .caseId(caseId)
                .userId("wa-dlq-user@fake.hmcts.net")
                .newStateId("appealSubmitted")
                .caseTypeId("Asylum");

        String dlqMessageId = randomMessageId();
        String messageIdFromHourAgo =  randomMessageId();

        sendMessageToDlq(dlqMessageId, eventInformationBuilder.eventTimeStamp(LocalDateTime.now()).build());
        waitSeconds(3);

        sendMessageToTopic(messageIdFromHourAgo,
                eventInformationBuilder.eventTimeStamp(LocalDateTime.now().plusHours(1)).build());
        waitSeconds(3);

        await().ignoreException(AssertionError.class)
                .pollInterval(3, SECONDS)
                .atMost(120, SECONDS)
                .until(
                        () -> {
                            final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, true);
                            if (dlqMessagesFromDb != null) {
                                final List<CaseEventMessage> caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                                assertTrue(caseEventMessages.stream()
                                        .anyMatch(x -> x.getMessageId().equals(dlqMessageId)
                                                && x.getState() == MessageState.PROCESSED));
                                deleteMessagesFromDatabase(caseEventMessages);
                                return true;
                            } else {
                                return false;
                            }
                        });
    }


    @Test
    public void should_not_process_message_unless_in_ready_state() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId());

        final EventInformation eventInformation = EventInformation.builder()
                .eventInstanceId(UUID.randomUUID().toString())
                .jurisdictionId("IA")
                .eventId("makeAnApplication")
                .userId("wa-dlq-user@fake.hmcts.net")
                .newStateId("appealSubmitted")
                .caseTypeId("Asylum")
                .eventTimeStamp(LocalDateTime.now())
                .build();

        messageIds.forEach(msgId -> {
            sendMessageToTopic(msgId, eventInformation);
            waitSeconds(3);
        });

        await().ignoreException(AssertionError.class)
                .pollInterval(3, SECONDS)
                .atMost(120, SECONDS)
                .until(
                        () -> {
                            final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(MessageState.UNPROCESSABLE);
                            if (dlqMessagesFromDb != null) {
                                assertEquals(messageIds.size(), dlqMessagesFromDb.getNumberOfMessagesFound());
                                deleteMessagesFromDatabase(dlqMessagesFromDb.getCaseEventMessages());
                                return true;
                            } else {
                                return false;
                            }
                        });
    }

    @Test
    public void should_not_process_dlq_message_unless_other_messages_exist_with_same_case_id() {
        String msgId = randomMessageId();
        String caseId = randomCaseId();

        final EventInformation eventInformation = EventInformation.builder()
                .eventInstanceId(UUID.randomUUID().toString())
                .jurisdictionId("IA")
                .eventId("makeAnApplication")
                .userId("wa-dlq-user@fake.hmcts.net")
                .newStateId("appealSubmitted")
                .caseId(caseId)
                .caseTypeId("Asylum")
                .eventTimeStamp(LocalDateTime.now())
                .build();


        sendMessageToDlq(msgId, eventInformation);
        waitSeconds(3);

        await().ignoreException(AssertionError.class)
                .pollInterval(3, SECONDS)
                .atMost(45, SECONDS)
                .until(
                        () -> {
                            final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(MessageState.READY);
                            if (dlqMessagesFromDb != null) {
                                assertEquals(1, dlqMessagesFromDb.getNumberOfMessagesFound());
                                assertEquals(msgId, dlqMessagesFromDb.getCaseEventMessages().get(0).getMessageId());
                                deleteMessagesFromDatabase(dlqMessagesFromDb.getCaseEventMessages());
                                return true;
                            } else {
                                return false;
                            }
                        });
    }

}
