package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.config.TestUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationMetadata;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerTestingControllerFunctionalTest extends SpringBootFunctionalBaseTest {

    private static final Boolean FROM_DLQ = Boolean.TRUE;
    private static final Boolean NOT_FROM_DLQ = Boolean.FALSE;

    private LocalDateTime eventTimestamp1;
    private LocalDateTime eventTimestamp2;
    private LocalDateTime holdUntilTimestamp;

    //should match valid values from MessageState Enum
    Matcher<String> stateMatcher = Matchers.oneOf("NEW", "READY", "PROCESSED");

    @Before
    public void setup() {
        eventTimestamp1 = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(1);
        eventTimestamp2 = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(2);
        holdUntilTimestamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").plusDays(10);
    }



    @Test
    public void given_post_event_using_test_rest_endpoints_should_create_task() {
        String caseIdForTask = getCaseId();

        String messageId = randomMessageId();
        String eventInstanceId = UUID.randomUUID().toString();

        LocalDateTime timeStamp = LocalDateTime.now();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask,
                                                                  "wa-dlq-user@fake.hmcts.net", timeStamp);
        EventInformationRequest createRequest = createRequestWithAdditionalMetadata(eventInformation, null);

        postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", notNullValue())
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(TestUtils.removeTrailingZeroes(timeStamp)))
            .body("FromDlq", equalTo(false))
            .body("State", stateMatcher)
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));

        Response taskFound = findTasksByCaseId(caseIdForTask, 1);

        String taskId = taskFound
            .then().assertThat()
            .body("[0].id", CoreMatchers.notNullValue())
            .extract()
            .path("[0].id");

        Response response = findTaskDetailsForGivenTaskId(taskId);
        String idempotencyKey = idempotencyKeyGenerator.generateIdempotencyKey(eventInformation.getEventInstanceId(),
                                                                               "followUpNonStandardDirection");

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", equalToIgnoringCase("asylum"))
            .body("jurisdiction.value", equalToIgnoringCase("ia"))
            .body("idempotencyKey.value", is(idempotencyKey))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", equalToIgnoringCase("unassigned"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseIdForTask))
            .body("name.value", equalToIgnoringCase("Follow-up non-standard direction"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", CoreMatchers.notNullValue())
            .body("taskId.value", equalToIgnoringCase("followUpNonStandardDirection"))
            .body("warningList.value", is("[]"));
    }

    @Test
    public void should_save_ccd_event_using_test_rest_endpoints() {
        String messageId = randomMessageId();
        String caseIdForTask = RandomStringUtils.randomNumeric(16);
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask);

        EventInformationRequest createRequest = createRequestWithAdditionalMetadata(eventInformation);

        postEventToRestEndpoint(messageId, s2sToken, createRequest).then()
            .statusCode(HttpStatus.CREATED.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", notNullValue())
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(TestUtils.removeTrailingZeroes(eventTimestamp1)))
            .body("FromDlq", equalTo(false))
            .body("State", stateMatcher)
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(TestUtils.removeTrailingZeroes(holdUntilTimestamp)))
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
            .body("EventTimestamp", equalTo(TestUtils.removeTrailingZeroes(updatedEventTimestamp))) // updated
            .body("FromDlq", equalTo(true)) // updated
            .body("State", stateMatcher)
            .body("MessageContent", equalTo(asJsonString(updateRequest))) // updated
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(TestUtils.removeTrailingZeroes(holdUntilTimestamp)))
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

        getMessageFromRestEndpoint(messageId, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("MessageId", equalTo(messageId))
            .body("Sequence", equalTo(sequence))
            .body("CaseId", equalTo(caseIdForTask))
            .body("EventTimestamp", equalTo(TestUtils.removeTrailingZeroes(eventTimestamp1)))
            .body("FromDlq", equalTo(false))
            .body("State", stateMatcher)
            .body("MessageContent", equalTo(asJsonString(createRequest)))
            .body("Received", notNullValue())
            .body("DeliveryCount", equalTo(0))
            .body("HoldUntil", equalTo(TestUtils.removeTrailingZeroes(holdUntilTimestamp)))
            .body("RetryCount", equalTo(0))

            .rootPath("MessageProperties")
            .body("messageProperty1", equalTo("value1"))
            .body("messageProperty2", equalTo("value2"));
    }

    @Test
    public void messages_should_be_created() {
        String caseId1 = RandomStringUtils.randomNumeric(16);
        String caseId2 = RandomStringUtils.randomNumeric(16);
        String messageId1 = createMessage(eventTimestamp1, caseId1, FROM_DLQ);
        String messageId2 = createMessage(eventTimestamp2, caseId1, FROM_DLQ);
        createMessage(eventTimestamp2, caseId2, NOT_FROM_DLQ);

        getMessagesFromRestEndpoint("NEW,READY,PROCESSED,UNPROCESSABLE", caseId1, null, "true", s2sToken)
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
    public void should_return_error_when_no_query_parameters_specified() {
        createMessage(eventTimestamp1, RandomStringUtils.randomNumeric(16), NOT_FROM_DLQ);

        getMessagesFromRestEndpoint(null, null, null, null, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", equalTo("No query parameters specified"))
            .body("totalNumberOfMessagesInTheDB", greaterThan(0))
            .body("numberOfMessagesMatchingTheQuery", equalTo(0))
            .body("caseEventMessages.size()", equalTo(0));
    }

    @Test
    public void should_query_messages_when_there_are_no_messages_matching_my_query() {
        createMessage(eventTimestamp1, RandomStringUtils.randomNumeric(16), NOT_FROM_DLQ);

        getMessagesFromRestEndpoint(null, RandomStringUtils.randomNumeric(16), null, null, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", equalTo("No records matching the query"))
            .body("totalNumberOfMessagesInTheDB", greaterThan(0))
            .body("numberOfMessagesMatchingTheQuery", equalTo(0))
            .body("caseEventMessages.size()", equalTo(0));
    }

    @Test
    public void should_delete_message() {
        String caseId1 = RandomStringUtils.randomNumeric(16);
        String messageToDelete = createMessage(eventTimestamp1, caseId1, FROM_DLQ);
        String messageToKeep = createMessage(eventTimestamp2, caseId1, FROM_DLQ);

        deleteEventToRestEndpoint(messageToDelete, s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value());

        getMessagesFromRestEndpoint("NEW,READY,PROCESSED,UNPROCESSABLE", caseId1, null, "true", s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", containsString("Found"))
            .body("message", containsString("messages"))
            .body("numberOfMessagesMatchingTheQuery", equalTo(1))
            .body("caseEventMessages.size()", equalTo(1))
            .body("caseEventMessages.MessageId", hasItem(equalTo(messageToKeep)));
    }

    @Test
    public void should_delete_message_and_get_404_if_not_found() {
        String messageId1 = RandomStringUtils.randomNumeric(16);

        deleteEventToRestEndpoint(messageId1, s2sToken)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private String createMessage(LocalDateTime eventTimestamp, String caseId, Boolean fromDlq) {
        String messageId = randomMessageId();
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformationRequest createRequest = new EventInformationRequest(
            buildEventInformation(eventInstanceId, caseId), null);

        // create message
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
        return createRequestWithAdditionalMetadata(eventInformation, holdUntilTimestamp);
    }

    private EventInformationRequest createRequestWithAdditionalMetadata(EventInformation eventInformation,
                                                                        LocalDateTime holdUntil) {
        return new EventInformationRequest(
            eventInformation,
            new EventInformationMetadata(
                Map.of(
                    "messageProperty1", "value1",
                    "messageProperty2", "value2"
                ),
                holdUntil
            )
        );
    }

    private EventInformation buildEventInformation(String eventInstanceId, String caseIdForTask) {
        return buildEventInformation(eventInstanceId, caseIdForTask, "insert_true", eventTimestamp1);
    }

    private EventInformation buildEventInformation(String eventInstanceId, String caseIdForTask, String userId,
                                                   LocalDateTime eventTimeStamp) {
        return EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .eventTimeStamp(eventTimeStamp)
            .caseId(caseIdForTask)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("sendDirection")
            .newStateId(null)
            .previousStateId(null)
            .userId(userId)
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

    private Response getMessagesFromRestEndpoint(String states,
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

    private Response getMessageFromRestEndpoint(String messageId,
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
