package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationMetadata;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerTestingControllerTest extends SpringBootFunctionalBaseTest {

    private LocalDateTime eventTimeStamp;
    private LocalDateTime holdUntilTimeStamp;

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(1);
        holdUntilTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").plusDays(10);
    }

    @Test
    public void should_save_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = UUID.randomUUID().toString();
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask);

        EventInformationRequest createRequest = new EventInformationRequest(
            eventInformation,
            new EventInformationMetadata(
                Map.of(
                    "messageProperty1", "value1",
                    "messageProperty2", "value2"
                ),
                holdUntilTimeStamp
            ));

        postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", notNullValue())
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(eventTimeStamp.toString()))
            .body("FromDlq", equalTo(false))
            .body("State", equalTo(MessageState.NEW.name()))
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(holdUntilTimeStamp.toString()))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    @Test
    public void should_update_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = UUID.randomUUID().toString();
        String eventInstanceId = UUID.randomUUID().toString();
        LocalDateTime updatedEventTimestamp = eventTimeStamp.minusDays(10);

        EventInformationRequest createRequest = new EventInformationRequest(
            buildEventInformation(eventInstanceId, caseIdForTask), null);

        Integer sequence = postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("Sequence");

        EventInformationRequest updateRequest = new EventInformationRequest(
            EventInformation.builder()
                .eventInstanceId(eventInstanceId)
                .eventTimeStamp(updatedEventTimestamp)
                .caseId(caseIdForTask)
                .jurisdictionId("IA")
                .caseTypeId("Asylum")
                .eventId("sendDirection")
                .newStateId(null)
                .previousStateId(null)
                .userId("some user Id")
                .additionalData(additionalData())
                .build(),
            new EventInformationMetadata(
                Map.of(
                    "messageProperty1", "value1",
                    "messageProperty2", "value2"
                ),
                holdUntilTimeStamp
            )
        );

        putEventToRestEndpoint(messageId, s2sToken, updateRequest, true)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", equalTo(sequence))
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(updatedEventTimestamp.toString())) // updated
            .body("FromDlq", equalTo(true)) // updated
            .body("State", equalTo(MessageState.NEW.name()))
            .body("MessageContent", equalTo(asJsonString(updateRequest))) // updated
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(holdUntilTimeStamp.toString()))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    @Test
    public void should_get_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = UUID.randomUUID().toString();
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask);
        EventInformationRequest createRequest = new EventInformationRequest(
            eventInformation,
            new EventInformationMetadata(
                Map.of(
                    "messageProperty1", "value1",
                    "messageProperty2", "value2"
                ),
                holdUntilTimeStamp
            )
        );

        Integer sequence = postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("Sequence");

        getEventToRestEndpoint(messageId, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", equalTo(sequence))
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(eventTimeStamp.toString()))
            .body("FromDlq", equalTo(false))
            .body("State", equalTo(MessageState.NEW.name()))
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(holdUntilTimeStamp.toString()))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    private EventInformation buildEventInformation(String eventInstanceId, String caseIdForTask) {
        return EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .eventTimeStamp(eventTimeStamp)
            .caseId(caseIdForTask)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("sendDirection")
            .newStateId(null)
            .previousStateId(null)
            .userId("some user Id")
            .additionalData(additionalData())
            .build();
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

    private String randomMessageId() {
        return "" + ThreadLocalRandom.current().nextLong(1000000);
    }

    private Response getEventToRestEndpoint(String messageId,
                                            String s2sToken) {
        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .get("/messages/" + messageId);
    }

    private Response postEventToRestEndpoint(String messageId,
                                             String s2sToken,
                                             EventInformationRequest eventInformation) {
        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages/" + messageId);
    }

    private Response putEventToRestEndpoint(String messageId, String s2sToken,
                                            EventInformationRequest request,
                                            boolean fromDlq) {
        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(request))
            .when()
            .put("/messages/" + messageId + (fromDlq ? "?from_dlq=true" : ""));
    }
}
