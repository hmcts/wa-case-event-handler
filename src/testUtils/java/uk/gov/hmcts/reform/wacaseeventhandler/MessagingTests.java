package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class MessagingTests extends SpringBootFunctionalBaseTest {

    protected String randomMessageId() {
        return "" + ThreadLocalRandom.current().nextLong(1000000);
    }

    private AdditionalData additionalData() {
        return AdditionalData.builder()
            .data(dataAsMap())
            .build();
    }

    @NotNull
    private Map<String, Object> dataAsMap() {
        return Map.of(
            "lastModifiedDirection", Map.of("dateDue", ""),
            "appealType", "protection"
        );
    }

    protected void deleteMessagesFromDatabase(List<CaseEventMessage> caseEventMessages) {
        caseEventMessages.stream()
            .map(CaseEventMessage::getMessageId)
            .forEach(this::deleteMessage);
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

    protected void sendMessageToDlq(String messageId, EventInformation eventInformation) {
        sendMessage(messageId, eventInformation, true);
    }

    protected void sendMessageToTopic(String messageId, EventInformation eventInformation) {
        sendMessage(messageId, eventInformation, false);
    }

    private void callRestEndpoint(String s2sToken,
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

    protected void sendMessage(String messageId,
                               EventInformation eventInformation,
                               boolean sendDirectlyToDlq) {
        if (publisher != null) {
            publishMessageToTopic(eventInformation, sendDirectlyToDlq);
            waitSeconds(3);
        } else {
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

        waitSeconds(3);
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
}
