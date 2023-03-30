package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class MessagingTests extends SpringBootFunctionalBaseTest {
    // Messages go to Camunda that is locking the DB. This is to avoid OptimisticLockingException,
    // that we have seen, when multiple threads try to update the DB at the same time.
    private static final Integer SECONDS_TO_WAIT_FOR_THE_MESSAGE_TO_BE_PROCESSED = 3;

    protected String randomMessageId() {
        return "" + ThreadLocalRandom.current().nextLong(1000000);
    }

    @NotNull
    private Map<String, Object> dataAsMap() {
        return Map.of(
            "lastModifiedDirection", Map.of("dateDue", ""),
            "appealType", "protection"
        );
    }

    protected void deleteMessagesFromDatabase(List<CaseEventMessage> caseEventMessages) {

        AtomicInteger count = new AtomicInteger();
        caseEventMessages.forEach(caseEventMessage -> {
            log.info("Attempt to delete message from DB. CaseId:{} MessageId:{}",
                caseEventMessage.getCaseId(), caseEventMessage.getMessageId());
            count.set(0);
            await().ignoreException(AssertionError.class)
                .pollInterval(3, SECONDS)
                .atMost(120, SECONDS)
                .until(
                    () -> {
                        count.incrementAndGet();
                        if (!isMessageExist(caseEventMessage.getMessageId())) {
                            log.info("Message deleted from DB. CaseId:{} MessageId:{}",
                                caseEventMessage.getCaseId(), caseEventMessage.getMessageId());
                            return true;
                        } else {
                            log.info("Message found in DB trying  to delete. CaseId:{} MessageId:{} Attempt Count:{}",
                                caseEventMessage.getCaseId(), caseEventMessage.getMessageId(), count.get());

                            deleteMessage(caseEventMessage.getMessageId());
                            return false;
                        }
                    });
        });

        log.info("All test messages deleted from db");
    }

    protected void deleteMessagesFromDatabaseByMsgIds(List<String> messageIds) {
        messageIds.forEach(this::deleteMessage);
    }

    protected void deleteMessagesFromDatabaseByMsgIds(String caseId) {
        final List<CaseEventMessage> caseEventMessages = getMessagesFromDb(caseId).getCaseEventMessages();
        deleteMessagesFromDatabase(caseEventMessages);
    }

    private void deleteMessage(String msgId) {
        log.info("Deleting case event messages from DB with message Id " + msgId);
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .delete("/messages/" + msgId)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    protected void sendMessagesToDlq(Map<String, EventInformation> eventInformationMessages) {
        for (var entry : eventInformationMessages.entrySet()) {
            sendMessageToDlq(entry.getKey(), entry.getValue());
        }
    }

    protected void sendMessageToDlq(String messageId, EventInformation eventInformation) {
        sendMessage(messageId, eventInformation, true);
    }

    protected void sendMessagesToTopic(Map<String, EventInformation> eventInformationMessages) {
        for (var entry : eventInformationMessages.entrySet()) {
            sendMessageToTopic(entry.getKey(), entry.getValue());
        }
    }

    protected void sendMessageToTopic(String messageId, EventInformation eventInformation) {
        sendMessage(messageId, eventInformation, false);
    }

    protected void callRestEndpoint(String s2sToken,
                                  EventInformation eventInformation,
                                  boolean sendDirectlyToDlq,
                                  String messageId) {
        given()
            .log()
            .all()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(eventInformation))
            .when()
            .queryParam("from_dlq", sendDirectlyToDlq)
            .post("/messages/" + messageId)
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    private void sendMessage(String messageId,
                             EventInformation eventInformation,
                             boolean sendDirectlyToDlq) {
        if (publisher != null) {
            log.info("sendMessage to the topic, using publisher with message ID " + messageId + ","
                         + " caseId: " + eventInformation.getCaseId() + ", toDLQ: " + sendDirectlyToDlq);
            publishMessageToTopic(eventInformation, sendDirectlyToDlq);

            waitSeconds(SECONDS_TO_WAIT_FOR_THE_MESSAGE_TO_BE_PROCESSED);
        } else {
            log.info("sendMessage to the topic, using restEndpoint with message ID " + messageId + ","
                         + " caseId: " + eventInformation.getCaseId() + ", toDLQ: " + sendDirectlyToDlq);
            callRestEndpoint(s2sToken, eventInformation, sendDirectlyToDlq, messageId);
        }
    }

    private void publishMessageToTopic(EventInformation eventInformation, boolean sendDirectlyToDlq) {
        String jsonMessage = asJsonString(eventInformation);
        ServiceBusMessage message = new ServiceBusMessage(jsonMessage.getBytes());
        if (!sendDirectlyToDlq) {
            message.setSessionId(eventInformation.getCaseId());
        }

        publisher.sendMessage(message);
    }

    protected EventMessageQueryResponse getMessagesFromDb(String caseId, boolean fromDlq) {
        Map<String, Object> params = new HashMap<>();
        params.put("case_id", caseId);
        params.put("from_dlq", fromDlq);
        return getMessages(params);
    }

    protected EventMessageQueryResponse getMessagesFromDb(String caseId) {
        Map<String, Object> params = new HashMap<>();
        params.put("case_id", caseId);
        return getMessages(params);
    }

    protected EventMessageQueryResponse getMessagesFromDb(MessageState state) {
        Map<String, Object> params = new HashMap<>();
        params.put("states", state.name());
        return getMessages(params);
    }

    private EventMessageQueryResponse getMessages(Map<String, Object> queryParameters) {
        final Response response = given()
            .log()
            .all()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .queryParams(queryParameters)
            .get("/messages/query");

        return response.body().as(EventMessageQueryResponse.class);
    }

    private boolean isMessageExist(String messageId) {
        log.info("retrieving case event messages from DB with message Id " + messageId);
        final Response response = given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .get("/messages/" + messageId);

        return response.then().extract().statusCode() == HttpStatus.OK.value();
    }
}
