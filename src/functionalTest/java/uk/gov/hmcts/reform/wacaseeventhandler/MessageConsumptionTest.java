package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusMessage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

public class MessageConsumptionTest extends SpringBootFunctionalBaseTest {
    private LocalDateTime eventTimeStamp;
    private String userId;

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.now().minusDays(1);
        userId = "some insert_true,process_false user id";
    }

    @Test
    public void should_succeed_to_create_a_task_and_store_it_in_db() {
        String caseIdForTask = RandomStringUtils.randomNumeric(16);

        sendMessage(
            caseIdForTask,
            "makeAnApplication",
            "",
            "",
            false,
            "IA",
            "Asylum"
        );

        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    given()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .basePath("/messages/query")
                        .param("case_id", caseIdForTask)
                        .when()
                        .get()
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .assertThat()
                        .body("numberOfMessagesMatchingTheQuery", equalTo(1))
                        .body("caseEventMessages.size()", equalTo(1))
                        .body("caseEventMessages.CaseId", hasItem(equalTo(caseIdForTask)))
                        .body("caseEventMessages.State", hasItem(equalTo("NEW")));
                    return true;
                });
    }

    protected void sendMessage(String caseId,
                               String event,
                               String previousStateId,
                               String newStateId,
                               boolean taskDelay,
                               String jurisdictionId,
                               String caseTypeId) {

        if (taskDelay) {
            eventTimeStamp = LocalDateTime.now().plusSeconds(2);
        }
        EventInformation eventInformation = createEventInformation(
            caseId,
            event,
            previousStateId,
            newStateId,
            eventTimeStamp,
            jurisdictionId,
            caseTypeId
        );

        if (publisher != null) {
            publishMessageToTopic(eventInformation);
        } else {
            postMessageUsingRestEndpoint(s2sToken, eventInformation);
        }
    }

    private void publishMessageToTopic(EventInformation eventInformation) {
        String jsonMessage = asJsonString(eventInformation);
        ServiceBusMessage message = new ServiceBusMessage(jsonMessage.getBytes());
        message.setSessionId(eventInformation.getCaseId());

        publisher.sendMessage(message);
    }

    private void postMessageUsingRestEndpoint(String s2sToken, EventInformation eventInformation) {
        String randomMessageId = UUID.randomUUID().toString();
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages/" + randomMessageId)
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    private EventInformation createEventInformation(String caseId,
                                                 String event,
                                                 String previousStateId,
                                                 String newStateId,
                                                 LocalDateTime localDateTime,
                                                 String jurisdictionId,
                                                 String caseTypeId) {
        return EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(localDateTime)
            .caseId(caseId)
            .jurisdictionId(jurisdictionId)
            .caseTypeId(caseTypeId)
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .additionalData(setAdditionalData())
            .userId(userId)
            .build();
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
}
