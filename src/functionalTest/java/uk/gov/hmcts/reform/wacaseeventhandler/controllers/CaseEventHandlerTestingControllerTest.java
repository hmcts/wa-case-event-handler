package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
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

import java.net.URI;
import java.net.URISyntaxException;
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

    /**
     * TODO:
     * /messages/query?case_id=1234567890123456&states=NEW&from_dlq=true&event_timestamp=2020-12-07T17:39:22.932622
     * /messages/query?case_id=1234567890123456
     * /messages/query?states=NEW
     * /messages/query?from_dlq=true&event_timestamp=2020-12-07T17:39:22.932622
     * /messages/query?case_id=1234567890123456&states=NEW,READY
     *
     * AC 1
     * Given there are some messages in the case event messages database
     * When I call GET /messages/query **
     * And there are messages matching my query
     * Then the API will return all the messages matching my query
     *
     * AC 2
     * Given there are some messages in the case event messages database
     * When I call GET /messages/query **
     * And there are messages matching my query
     * Then the API will return as part of the answer payload the number of messages that match the query
     *
     * AC 3
     * Given there are some messages in the case event messages database
     * When I call GET /messages/query **
     * And there are no messages matching my query
     * Then the API will return an error message saying that no records matching the query
     *
     * AC 4
     * Given there are no messages in the case event messages database
     * When I call GET /messages/query **
     * Then the API will return an error message saying that there are no records in the database
     */

    @Test
    public void should_query_messages() {
        // TODO
    }

    @Test
    public void should_query_messages_when_there_is_no_messages_in_database() throws Exception {
        getMessagesToRestEndpoint("NEW,UNPROCESSABLE",
                                  "1111-2222-3333-4444",
                                  eventTimeStamp.toString(),
                                  "false",
                                  s2sToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat()
            .body("message", equalTo("There are no records in the database"))
            .body("totalNumberOfMessagesInTheDB", equalTo(0))
            .body("numberOfMessagesMatchingTheQuery", equalTo(0))
            .body("caseEventMessages.size()", equalTo(0));
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

    private Response getMessagesToRestEndpoint(String states,
                                               String caseId,
                                               String eventTimestamp,
                                               String fromDlq,
                                               String s2sToken) throws URISyntaxException {
        URI uri = new URIBuilder("")
            .addParameter("states", states)
            .addParameter("caseID", caseId)
            .addParameter("eventTimestamp", eventTimestamp)
            .addParameter("fromDlq", fromDlq)
            .build();

        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .get("/messages/query" + uri.toString());
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
