package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerTestingControllerTest extends SpringBootFunctionalBaseTest {

    private static final Boolean FROM_DLQ = Boolean.TRUE;
    private static final Boolean NOT_FROM_DLQ = Boolean.FALSE;

    private LocalDateTime eventTimestamp1;
    private LocalDateTime eventTimestamp2;
    private LocalDateTime holdUntilTimestamp;

    @Before
    public void setup() {
        eventTimestamp1 = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(1);
        eventTimestamp2 = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(2);
        holdUntilTimestamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").plusDays(10);
    }

    @Test
    public void should_save_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = RandomStringUtils.randomNumeric(16);
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask);

        EventInformationRequest createRequest = createRequestWithAdditionalMetadata(eventInformation);

        postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", notNullValue())
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(eventTimestamp1.toString()))
            .body("FromDlq", equalTo(false))
            .body("State", equalTo(MessageState.NEW.name()))
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(holdUntilTimestamp.toString()))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    @Test
    public void should_update_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = RandomStringUtils.randomNumeric(16);
        String eventInstanceId = UUID.randomUUID().toString();
        LocalDateTime updatedEventTimestamp = eventTimestamp1.minusDays(10);

        EventInformationRequest createRequest = new EventInformationRequest(
            buildEventInformation(eventInstanceId, caseIdForTask), null);

        Integer sequence = postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("Sequence");

        EventInformationRequest updateRequest = createEventInformationRequest(
            caseIdForTask,
            eventInstanceId,
            updatedEventTimestamp
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
            .body("HoldUntil", equalTo(holdUntilTimestamp.toString()))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    @Test
    public void should_update_ccd_event_using_test_rest_endpoints_when_from_dlq_not_specified() {
        String messageId = randomMessageId();
        String caseIdForTask = RandomStringUtils.randomNumeric(16);
        String eventInstanceId = UUID.randomUUID().toString();
        LocalDateTime updatedEventTimestamp = eventTimestamp1.minusDays(10);

        EventInformationRequest updateRequest = createEventInformationRequest(
            caseIdForTask,
            eventInstanceId,
            updatedEventTimestamp
        );

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(updateRequest))
            .when()
            .put("/messages/" + messageId)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value()); // defaults
    }

    @Test
    public void should_get_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = RandomStringUtils.randomNumeric(16);
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask);
        EventInformationRequest createRequest = createRequestWithAdditionalMetadata(eventInformation);

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
            .body("EventTimestamp", equalTo(eventTimestamp1.toString()))
            .body("FromDlq", equalTo(false))
            .body("State", equalTo(MessageState.NEW.name()))
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(holdUntilTimestamp.toString()))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    @Test
    public void should_query_messages() throws Exception {
        String caseId1 = RandomStringUtils.randomNumeric(16);
        String caseId2 = RandomStringUtils.randomNumeric(16);
        String messageId1 = createMessage(eventTimestamp1, caseId1, FROM_DLQ);
        String messageId2 = createMessage(eventTimestamp2, caseId1, FROM_DLQ);
        createMessage(eventTimestamp2, caseId2, NOT_FROM_DLQ);

        getMessagesToRestEndpoint("NEW,UNPROCESSABLE", caseId1, null, "true", s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", containsString("Found"))
            .body("message", containsString("messages"))
            .body("totalNumberOfMessagesInTheDB", greaterThan(2))
            .body("numberOfMessagesMatchingTheQuery", equalTo(2))
            .body("caseEventMessages.size()", equalTo(2))
            .body("caseEventMessages.MessageId", hasItem(equalTo(messageId1)))
            .body("caseEventMessages.MessageId", hasItem(equalTo(messageId2)));
    }

    @Test
    public void should_return_error_when_no_query_parameters_specified() throws Exception {
        createMessage(eventTimestamp1, RandomStringUtils.randomNumeric(16), NOT_FROM_DLQ);

        getMessagesToRestEndpoint(null, null, null, null, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", equalTo("No query parameters specified"))
            .body("totalNumberOfMessagesInTheDB", greaterThan(0))
            .body("numberOfMessagesMatchingTheQuery", equalTo(0))
            .body("caseEventMessages.size()", equalTo(0));
    }

    @Test
    public void should_query_messages_when_there_are_no_messages_matching_my_query() throws Exception {
        createMessage(eventTimestamp1, RandomStringUtils.randomNumeric(16), NOT_FROM_DLQ);

        getMessagesToRestEndpoint(null, RandomStringUtils.randomNumeric(16), null, null, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", equalTo("No records matching the query"))
            .body("totalNumberOfMessagesInTheDB", greaterThan(0))
            .body("numberOfMessagesMatchingTheQuery", equalTo(0))
            .body("caseEventMessages.size()", equalTo(0));
    }

    @Test
    public void should_delete_message() throws Exception {
        String caseId1 = RandomStringUtils.randomNumeric(16);
        String messageId1 = createMessage(eventTimestamp1, caseId1, FROM_DLQ);
        String messageId2 = createMessage(eventTimestamp2, caseId1, FROM_DLQ);

        deleteEventToRestEndpoint(messageId1,  s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value());

        getMessagesToRestEndpoint("NEW,UNPROCESSABLE", caseId1, null, "true", s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", containsString("Found"))
            .body("message", containsString("messages"))
            .body("numberOfMessagesMatchingTheQuery", equalTo(1))
            .body("caseEventMessages.size()", equalTo(1))
            .body("caseEventMessages.MessageId", hasItem(equalTo(messageId2)));
    }

    @Test
    public void should_delete_message_and_get_404_if_not_found() throws Exception {
        String messageId1 = RandomStringUtils.randomNumeric(16);

        deleteEventToRestEndpoint(messageId1,  s2sToken)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private String createMessage(LocalDateTime eventTimestamp, String caseId, Boolean fromDlq) {
        String messageId = randomMessageId();
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformationRequest createRequest = new EventInformationRequest(
            buildEventInformation(eventInstanceId, caseId), null);

        // crete message
        postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        EventInformationRequest updateRequest = createEventInformationRequest(caseId, eventInstanceId, eventTimestamp);

        // update message with eventTimestamp, caseId and fromDlq
        putEventToRestEndpoint(messageId, s2sToken, updateRequest, fromDlq)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        return messageId;
    }

    private EventInformationRequest createEventInformationRequest(String caseIdForTask, String eventInstanceId,
                                                                  LocalDateTime updatedEventTimestamp) {
        return createRequestWithAdditionalMetadata(
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
                .build());
    }

    private EventInformationRequest createRequestWithAdditionalMetadata(EventInformation eventInformation) {
        return new EventInformationRequest(
            eventInformation,
            new EventInformationMetadata(
                Map.of(
                    "messageProperty1", "value1",
                    "messageProperty2", "value2"
                ),
                holdUntilTimestamp
            )
        );
    }

    private EventInformation buildEventInformation(String eventInstanceId, String caseIdForTask) {
        return EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .eventTimeStamp(eventTimestamp1)
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

    private Map<String, Object> dataAsMap() {
        return Map.of(
                "lastModifiedDirection", Map.of("dateDue", ""),
                "appealType", "protection"
            );
    }

    private String randomMessageId() {
        return "" + ThreadLocalRandom.current().nextLong(1000000);
    }

    private Response getMessagesToRestEndpoint(String states,
                                               String caseId,
                                               String eventTimestamp,
                                               String fromDlq,
                                               String s2sToken) {
        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .param("states", states).and()
            .param("case_id", caseId).and()
            .param("event_timestamp", eventTimestamp).and()
            .param("from_dlq", fromDlq)
            .when()
            .get("/messages/query");
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
            .put("/messages/" + messageId + (fromDlq ? "?from_dlq=true" : "?from_dlq=false"));
    }

    private Response deleteEventToRestEndpoint(String messageId, String s2sToken) {
        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .delete("/messages/" + messageId);
    }
}
