package uk.gov.hmcts.reform.wacaseeventhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@ActiveProfiles(profiles = {"local", "functional"})
public class MessageProcessorFunctionalTest extends MessagingTests {

    private List<String> caseIdToDelete = new ArrayList<>();
    private Integer testExecution = 0;
    private Integer isReadyExecution = 0;


    public static List<CaseEventMessage> caseEventMessages;
    public static List<CaseEventMessage> caseEventMessagesToBeDeleted;

    @Before
    public void setup() {
        caseEventMessages = new ArrayList<>();
        caseEventMessagesToBeDeleted = new ArrayList<>();
        caseIdToDelete = new ArrayList<>();
    }

    @After
    public void teardown() {

        if (caseIdToDelete != null) {
            caseIdToDelete.forEach(this::deleteMessagesFromDatabaseByMsgIds);
            caseIdToDelete = new ArrayList<>();
        }

        deleteMessagesFromDatabase(caseEventMessagesToBeDeleted);

    }

    @Test
    public void should_process_multiple_messages_for_that_case() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId(), randomMessageId());

        String caseId = getWaCaseId();
        caseIdToDelete.add(caseId);

        final EventInformation.EventInformationBuilder eventInformationBuilder = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("wa-dlq-user@fake.hmcts.net")
            .newStateId(null)
            .caseTypeId("WaCaseType");

        Map<String, EventInformation> messages = new HashMap<>();
        messageIds.forEach(msgId -> {
            final EventInformation eventInformation =
                    eventInformationBuilder
                            .eventTimeStamp(LocalDateTime.now())
                            .build();
            log.info("should_process_message_with_the_lowest_event_timestamp_for_that_case, using message ID " + msgId);
            messages.put(msgId, eventInformation);
        });
        sendMessagesToTopic(messages);

        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(120, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, false);
                    if (dlqMessagesFromDb != null) {
                        caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                        assertEquals(messageIds.size(), caseEventMessages.size());
                        assertTrue(caseEventMessages.stream()
                                .allMatch(x -> x.getState() == MessageState.PROCESSED && x.getCaseId().equals(caseId)));
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    /**
     * CaseEventMessageRepository.LOCK_AND_GET_NEXT_MESSAGE_SQL
     * or exists (select 1 from wa_case_event_messages d "
     *     where d.event_timestamp > msg.event_timestamp + interval '30 minutes' "
     *     and not d.from_dlq "
     *     and d.state in ('READY', 'PROCESSED'))))) "
     */
    @Test
    public void should_process_dlq_msg_if_processed_or_ready_messages_with_timestamp_later_than_thirty_mins_exist() {
        final EventInformation.EventInformationBuilder eventInformationBuilder = EventInformation.builder()
                .eventInstanceId(UUID.randomUUID().toString())
                .jurisdictionId("WA")
                .eventId("makeAnApplication")
                .userId("wa-dlq-user@fake.hmcts.net")
                .newStateId(null)
                .caseTypeId("WaCaseType");

        String dlqMessageIdFromHourAgo = randomMessageId();
        log.info("should_process_dlq_msg_if_processed_or_ready_messages_with_timestamp_later_than_thirty_mins_exist, "
                + "using message ID for DLQ message " + dlqMessageIdFromHourAgo);
        String messageId =  randomMessageId();
        log.info("should_process_dlq_msg_if_processed_or_ready_messages_with_timestamp_later_than_thirty_mins_exist, "
                + "using event timestamp from hour ago "
                + messageId);

        String dlqCaseId = getWaCaseId();
        caseIdToDelete.add(dlqCaseId);

        sendMessageToDlq(dlqMessageIdFromHourAgo, eventInformationBuilder
            .caseId(dlqCaseId)
            .eventTimeStamp(LocalDateTime.now().minusHours(1))
            .build());

        String caseId = getWaCaseId();
        caseIdToDelete.add(caseId);
        sendMessageToTopic(messageId,
                eventInformationBuilder.caseId(caseId).eventTimeStamp(LocalDateTime.now()).build());

        await().ignoreException(AssertionError.class)
                .pollInterval(3, SECONDS)
                .atMost(120, SECONDS)
                .until(
                    () -> {
                        final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(dlqCaseId, true);
                        if (dlqMessagesFromDb != null) {
                            logMessageQueryResults(dlqMessagesFromDb);
                            caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                            assertTrue(format("no message with caseId: %s in PROCESSED state", dlqCaseId),
                                       caseEventMessages.stream().anyMatch(x -> x.getCaseId().equals(dlqCaseId)
                                            && x.getState() == MessageState.PROCESSED));
                            return true;
                        } else {
                            return false;
                        }
                    });
    }

    /**
     * CaseEventMessageRepository.LOCK_AND_GET_NEXT_MESSAGE_SQL
     * exists (select 1 from wa_case_event_messages d "
     *     where d.case_id = msg.case_id "
     *     and d.event_timestamp > msg.event_timestamp "
     *     and not d.from_dlq "
     *     and d.state = 'READY') "
     */
    @Test
    public void should_process_dlq_msg_if_processed_or_ready_messages_with_same_case_id_exist() {
        String caseId = getWaCaseId();

        final EventInformation.EventInformationBuilder eventInformationBuilder = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("wa-dlq-user@fake.hmcts.net")
            .newStateId(null)
            .caseTypeId("WaCaseType");

        String dlqMessageId = randomMessageId();
        log.info("should_process_dlq_msg_if_processed_or_ready_messages_with_same_case_id_exist, "
                     + "using message ID for DLQ message " + dlqMessageId);
        String messageIdFromFiveMinutesFromNow =  randomMessageId();
        log.info("should_process_dlq_msg_if_processed_or_ready_messages_with_same_case_id_exist, "
                     + "using event timestamp from hour ago "
                     + messageIdFromFiveMinutesFromNow);

        caseIdToDelete.add(caseId);

        sendMessageToDlq(dlqMessageId, eventInformationBuilder.eventTimeStamp(LocalDateTime.now()).build());

        sendMessageToTopic(messageIdFromFiveMinutesFromNow,
                           eventInformationBuilder.eventTimeStamp(LocalDateTime.now().plusMinutes(5)).build());

        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(120, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse dlqMessagesFromDb = getMessagesFromDb(caseId, true);
                    if (dlqMessagesFromDb != null) {
                        caseEventMessages = dlqMessagesFromDb.getCaseEventMessages();

                        assertTrue(caseEventMessages.stream()
                                       .anyMatch(x -> x.getCaseId().equals(caseId)
                                           && x.getState() == MessageState.PROCESSED));
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    @Test
    public void should_not_process_message_unless_in_ready_state() {
        List<String> messageIds = List.of(randomMessageId(), randomMessageId());

        // Sending message without case id will cause validation to fail and message will be stored with
        // UNPROCESSABLE state
        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventId("makeAnApplication")
            .userId("wa-dlq-user@fake.hmcts.net")
            .newStateId(null)
            .caseTypeId("WaCaseType")
            .eventTimeStamp(LocalDateTime.now())
            .additionalData(AdditionalData.builder()
                .data(Map.of("testName", "should_not_process_message_unless_in_ready_state"))
                .build())
            .build();

        Map<String, EventInformation> messages = new HashMap<>();
        messageIds.forEach(msgId -> {
            log.info("should_not_process_message_unless_in_ready_state using message ID " + msgId);
            messages.put(msgId, eventInformation);
        });
        sendMessagesToTopic(messages);

        AtomicReference<List<CaseEventMessage>> collect = new AtomicReference<>(new ArrayList<>());

        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    caseEventMessagesToBeDeleted = new ArrayList<>();

                    final EventMessageQueryResponse messagesInUnprocessableState
                        = getMessagesFromDb(MessageState.UNPROCESSABLE);

                    List<CaseEventMessage> unprocessableCaseEventMessage =
                        messagesInUnprocessableState.getCaseEventMessages()
                            .stream()
                            .filter(caseEventMessage -> hasAdditionalData(caseEventMessage.getMessageContent()))
                            .collect(Collectors.toList());

                    caseEventMessagesToBeDeleted.addAll(unprocessableCaseEventMessage);

                    collect.set(unprocessableCaseEventMessage);

                    assertEquals(messageIds.size(), collect.get().size());
                    return true;

                });

    }

    @Test
    public void should_not_process_any_message_after_unprocessable_message_for_same_case_id() {
        String caseId = getWaCaseId();
        String caseId2 = getWaCaseId();
        caseIdToDelete.add(caseId);
        caseIdToDelete.add(caseId2);
        String unprocessableMsgId = randomMessageId();

        // Sending message without time stamp will cause validation to fail and message will be stored with
        // UNPROCESSABLE state
        final EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventId("makeAnApplication")
            .caseId(caseId)
            .userId("wa-dlq-user@fake.hmcts.net")
            .newStateId(null)
            .eventTimeStamp(null)
            .caseTypeId("WaCaseType").build();


        log.info("should_not_process_any_message_after_unprocessable_message_for_same_case_id "
                     + "unprocessable message ID " + unprocessableMsgId);
        sendMessageToTopic(unprocessableMsgId, eventInformation);

        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(120, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse unprocessableMsg = getMessagesFromDb(caseId, false);

                    if (unprocessableMsg != null) {
                        caseEventMessages = unprocessableMsg.getCaseEventMessages();

                        assertTrue(caseEventMessages.stream()
                                       .anyMatch(x -> x.getCaseId().equals(caseId)
                                           && x.getMessageId().equals(unprocessableMsgId)
                                           && x.getState() == MessageState.UNPROCESSABLE));
                        return true;
                    } else {
                        return false;
                    }
                });

        String msgId = randomMessageId();
        String msgId2 = randomMessageId();

        final EventInformation.EventInformationBuilder eventInformationBuilder = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .jurisdictionId("WA")
            .eventId("makeAnApplication")
            .userId("wa-dlq-user@fake.hmcts.net")
            .newStateId(null)
            .eventTimeStamp(LocalDateTime.now())
            .caseTypeId("WaCaseType");

        log.info("should_not_process_any_message_after_unprocessable_message_for_same_case_id "
                     + "unprocessable message ID " + unprocessableMsgId);
        sendMessageToTopic(msgId, eventInformationBuilder.caseId(caseId).build());
        sendMessageToTopic(msgId2, eventInformationBuilder.caseId(caseId2).build());

        //Wait for message processor run and process the second message
        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse messagesFromDb = getMessagesFromDb(caseId2, false);
                    if (messagesFromDb != null) {
                        caseEventMessages = messagesFromDb.getCaseEventMessages();

                        assertTrue(caseEventMessages.stream()
                                       .anyMatch(caseEventMessage -> caseEventMessage.getCaseId().equals(caseId2)
                                           && caseEventMessage.getMessageId().equals(msgId2)
                                           && caseEventMessage.getState() == MessageState.PROCESSED));
                        return true;
                    } else {
                        return false;
                    }
                });

        //Assert that message for the case with unprocessable message is not processed
        await().ignoreException(AssertionError.class)
            .pollInterval(3, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    final EventMessageQueryResponse messagesFromDb = getMessagesFromDb(caseId, false);
                    if (messagesFromDb != null) {
                        caseEventMessages = messagesFromDb.getCaseEventMessages();

                        assertTrue(caseEventMessages.stream()
                                       .anyMatch(caseEventMessage -> caseEventMessage.getCaseId().equals(caseId)
                                           && caseEventMessage.getMessageId().equals(msgId)
                                           && caseEventMessage.getState() != MessageState.PROCESSED));
                        return true;
                    } else {
                        return false;
                    }
                });


    }


    private boolean hasAdditionalData(String msg) {
        try {
            JsonNode messageAsJson = new ObjectMapper().readTree(msg);
            final JsonNode additionalDataNode = messageAsJson
                    .findPath("AdditionalData")
                    .findPath("Data")
                    .findPath("testName");
            return "should_not_process_message_unless_in_ready_state".equals(additionalDataNode.textValue());
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    @Test
    public void should_not_process_dlq_message_if_no_processed_or_ready_messages_with_same_case_id_exist() {
        String msgId = randomMessageId();
        String caseId = getWaCaseId();

        caseIdToDelete.add(caseId);

        final EventInformation eventInformation = EventInformation.builder()
                .eventInstanceId(UUID.randomUUID().toString())
                .jurisdictionId("WA")
                .eventId("makeAnApplication")
                .userId("wa-dlq-user@fake.hmcts.net")
                .newStateId("appealSubmitted")
                .caseId(caseId)
                .caseTypeId("WaCaseType")
                .eventTimeStamp(LocalDateTime.now())
                .build();

        log.info("should_not_process_dlq_message_if_no_processed_or_ready_messages_with_same_case_id_exist using dlq "
                     + "message id " + msgId);
        sendMessageToDlq(msgId, eventInformation);

        testExecution = 0;
        isReadyExecution = 0;
        await().ignoreException(AssertionError.class)
                .pollInterval(3, SECONDS)
                .atMost(120, SECONDS)
                .until(
                    () -> {
                        final EventMessageQueryResponse messagesFromDb = getMessagesFromDb(caseId, true);
                        logMessageQueryResults(messagesFromDb);
                        if (messagesFromDb != null) {
                            testExecution++;

                            // store the execution number when message gets to a READY state
                            if (isReadyExecution == 0 && messagesFromDb.getCaseEventMessages().stream()
                                .filter(c -> c.getState().equals(MessageState.READY)).count() == 1) {
                                isReadyExecution = testExecution;
                            }
                            // give it few seconds and check it hasn't been processed using few pollIntervals
                            assertTrue("", messagesFromDb.getCaseEventMessages().stream()
                                .filter(c -> c.getState().equals(MessageState.READY)).count() == 1
                                && testExecution > isReadyExecution + 3);

                            return true;
                        } else {
                            return false;
                        }
                    });
    }

    private void logMessageQueryResults(EventMessageQueryResponse queryResponse) {
        String lineSeparator = System.getProperty("line.separator");
        String data = queryResponse == null ? "" : queryResponse.getCaseEventMessages().stream()
            .map(e -> "caseId: " + e.getCaseId()
                + " msgId: " + e.getMessageId()
                + " state: " + e.getState()
                + " dlq: " + e.getFromDlq())
            .collect(Collectors.joining(lineSeparator));
        log.info("messages from db:" + lineSeparator + data);
    }

}
