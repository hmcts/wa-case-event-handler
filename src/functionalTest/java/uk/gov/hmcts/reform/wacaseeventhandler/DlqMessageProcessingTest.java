package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.SubQueue;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class DlqMessageProcessingTest extends MessagingTests {

    private EventInformation buildEventInformation(String eventInstanceId, String caseIdForTask) {
        return EventInformation.builder()
                .eventInstanceId(eventInstanceId)
                .eventTimeStamp(LocalDateTime.now())
                .caseId(caseIdForTask)
                .jurisdictionId("IA")
                .caseTypeId("Asylum")
                .eventId("sendDirection")
                .userId("process_true")
                .build();
    }

    @Test
    public void should_update_message_in_db_from_new_to_ready_state_when_dlq_empty() {
        String messageId = randomMessageId();
        String messageId2 = randomMessageId();

        // TODO when running locally
        setDlqEmpty(true);

        storeMessagesInDatabase(List.of(messageId, messageId2));

        waitSeconds(45);

        checkMessagesInState(List.of(messageId, messageId2), MessageState.READY);

        deleteCaseEventMessagesFromDatabaseById(List.of(messageId, messageId2));
    }

    @Test
    public void should_not_update_message_in_db_when_dlq_not_empty() throws InterruptedException {
        String messageId = randomMessageId();
        String messageId2 = randomMessageId();

        // TODO when running locally
        setDlqEmpty(false);

        final EventInformation eventInformation = EventInformation.builder()
                .eventInstanceId("eventInstanceId")
                .eventTimeStamp(LocalDateTime.now())
                .caseId("1111333344445555")
                .jurisdictionId("IA")
                .caseTypeId("Asylum")
                .eventId("sendDirection")
                .userId("process_true")
                .build();

        //sendMessageToDlq(randomMessageId(), eventInformation);  // when running on pipeline

        storeMessagesInDatabase(List.of(messageId, messageId2));

        waitSeconds(30);

        checkMessagesInState(List.of(messageId, messageId2), MessageState.NEW);

        deleteCaseEventMessagesFromDatabaseById(List.of(messageId, messageId2));

        //        consumeDlqMessage(); // when running on pipeline
    }

    private void checkMessagesInState(List<String> messageIds, MessageState messageState) {
        messageIds.forEach(msgId -> {
            given()
                    .contentType(APPLICATION_JSON_VALUE)
                    .header(SERVICE_AUTHORIZATION, s2sToken)
                    .when()
                    .get("/messages/" + msgId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .assertThat()
                    .body("State", equalTo(messageState.name()));
        });
    }

    private void storeMessagesInDatabase(List<String> messageIds) {
        messageIds.forEach(msgId -> {
            EventInformation eventInformation
                    = buildEventInformation(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            given()
                    .contentType(APPLICATION_JSON_VALUE)
                    .header(SERVICE_AUTHORIZATION, s2sToken)
                    .body(asJsonString(eventInformation))
                    .queryParam("from_dlq", false)
                    .when()
                    .put("/messages/" + msgId)
                    .then()
                    .statusCode(HttpStatus.CREATED.value());
        });
    }

    private void setDlqEmpty(boolean dlqPeekMessagesExist) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .post("/messages/dlqPeekResponse/" + dlqPeekMessagesExist)
            .then()
            .statusCode(HttpStatus.OK.value());
    }


    private AdditionalData setAdditionalData() {
        Map<String, Object> dataMap = Map.of(
                "lastModifiedDirection", Map.of(
                        "dateDue", "",
                        "uniqueId", "",
                        "directionType", ""
                ),
                "appealType", "protection"
        );

        return AdditionalData.builder()
                .data(dataMap)
                .build();
    }

    private void callRestEndpoint(String s2sToken, EventInformation eventInformation) {
        given()
                .contentType(APPLICATION_JSON_VALUE)
                .header(SERVICE_AUTHORIZATION, s2sToken)
                .body(asJsonString(eventInformation))
                .when()
                .post("/messages")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }


    private void consumeDlqMessage() throws InterruptedException {
        log.info("Consuming DLQ message");
        String connectionString = "Endpoint=sb://ccd-servicebus-demo.servicebus.windows.net/;SharedAccessKeyName"
                + "=SendAndListenSharedAccessKey;SharedAccessKey=B4PezUTfHcKpRPZdIpWd9BUHp47l28NEClBnuIN2S9w=";
        //        try (ServiceBusReceiverClient receiver = serviceBusSessionReceiverClient.acceptNextSession()) {
        //            receiver.receiveMessages(1)
        //                    .forEach(
        //                            message -> {
        //                                try {
        //                                    receiver.complete(message);
        //                                } catch (Exception ex) {
        //                                    log.error("Error processing CCD Case Event message with id '{}' - "
        //                                            + "will continue to complete message", message.getMessageId());
        //                                    receiver.complete(message);
        //                                }
        //                            });
        //        } catch (IllegalStateException ex) {
        //            fail("Timeout: No CCD Case Event messages received waiting for next session.");
        //        } catch (Exception ex) {
        //            fail("Error occurred while closing the session" + ex);
        //        }
        ServiceBusReceiverClient receiver = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver() // Use this for session or non-session enabled queue or topic/subscriptions
                .topicName("wa-case-event-handler-topic-sessions-ft")
                .subscriptionName("gareth_dev_local")
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .buildClient();

        CountDownLatch countdownLatch = new CountDownLatch(1);

        // Create an instance of the processor through the ServiceBusClientBuilder
        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName("wa-case-event-handler-topic-sessions-ft")
                .subscriptionName("gareth_dev_local")
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .processMessage(DlqMessageProcessingTest::processMessage)
                .processError(context -> processError(context, countdownLatch))
                .buildProcessorClient();

        System.out.println("Starting the processor");
        processorClient.start();

        TimeUnit.SECONDS.sleep(10);
        System.out.println("Stopping and closing the processor");
        processorClient.close();
    }




    static void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        System.out.printf("Processing message. Session: %s, Sequence #: %s. Contents: %s%n", message.getMessageId(),
                message.getSequenceNumber(), message.getBody());
    }

    private static void processError(ServiceBusErrorContext context, CountDownLatch countdownLatch) {
        System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                context.getFullyQualifiedNamespace(), context.getEntityPath());

        if (!(context.getException() instanceof ServiceBusException)) {
            System.out.printf("Non-ServiceBusException occurred: %s%n", context.getException());
            return;
        }

        ServiceBusException exception = (ServiceBusException) context.getException();
        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
                || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
                || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            System.out.printf("An unrecoverable error occurred. Stopping processing with reason %s: %s%n",
                    reason, exception.getMessage());

            countdownLatch.countDown();
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            System.out.printf("Message lock lost for message: %s%n", context.getException());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            try {
                // Choosing an arbitrary amount of time to wait until trying again.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.err.println("Unable to sleep for period of time");
            }
        } else {
            System.out.printf("Error source %s, reason %s, message: %s%n", context.getErrorSource(),
                    reason, context.getException());
        }
    }
}
