package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class MessagingTests extends SpringBootFunctionalBaseTest {

    private LocalDateTime eventTimeStamp;
    private LocalDateTime holdUntilTimeStamp;

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(1);
        holdUntilTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").plusDays(10);
    }

    protected String randomMessageId() {
        return "" + ThreadLocalRandom.current().nextLong(1000000);
    }

    protected String randomCaseId() {
        return RandomStringUtils.randomNumeric(16);
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

    protected void deleteCaseEventMessagesFromDatabase(List<CaseEventMessage> caseEventMessages) {
        final List<String> msgIds = caseEventMessages.stream()
                .map(CaseEventMessage::getMessageId)
                .collect(Collectors.toList());
        deleteCaseEventMessagesFromDatabaseById(msgIds);
    }

    protected void deleteCaseEventMessagesFromDatabaseById(List<String> messageIds) {
        messageIds.stream()
                .forEach(msgId -> given()
                        .contentType(APPLICATION_JSON_VALUE)
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .when()
                        .delete("/messages/" + msgId)
                        .then()
                        .statusCode(HttpStatus.OK.value()));
    }

    protected void sendMessageToDlq(String messageId, EventInformation eventInformation) {
        sendMessage(messageId,eventInformation, true);
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
            waitSeconds(2);
        } else {
            callRestEndpoint(s2sToken, eventInformation, sendDirectlyToDlq, messageId);
        }
    }

    private void publishMessageToTopic(EventInformation eventInformation, boolean sendDirectlyToDlq) {
        log.info("Publishing message to Topic ");
        String jsonMessage = asJsonString(eventInformation);
        ServiceBusMessage message = new ServiceBusMessage(jsonMessage.getBytes());
        if (!sendDirectlyToDlq) {
            message.setSessionId(eventInformation.getCaseId());
        }

        publisher.sendMessage(message);

        waitSeconds(10);
    }
}
