package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.MessagingTests;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationMetadata;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class ProblemMessageControllerTest extends MessagingTests {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());

    private LocalDateTime eventTimestamp1;
    private LocalDateTime holdUntilTimestamp;

    @Before
    public void setup() {
        eventTimestamp1 = LocalDateTime.parse("2020-03-27T12:56:10.403975").minusDays(1);
        holdUntilTimestamp = LocalDateTime.parse("2020-03-27T12:56:10.403975").plusDays(10);
    }

    @Test
    public void should_check_for_unprocessable_messages_using_job_request_endpoint() throws Exception {
        String messageId = randomMessageId();
        String caseIdForTask = null;
        String eventInstanceId = UUID.randomUUID().toString();

        EventInformation eventInformation = buildEventInformation(eventInstanceId, caseIdForTask);

        EventInformationRequest createRequest = createRequestWithAdditionalMetadata(eventInformation);

        postEventToRestEndpoint(messageId, s2sToken, createRequest)
            .then()
            .statusCode(HttpStatus.CREATED.value());


        Response result = postJobMessageEndpoint(s2sToken,JobName.FIND_PROBLEM_MESSAGES.name())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        List<CaseEventMessage> caseEventMessages = OBJECT_MAPPER.readValue(result.body().asString(),
                                                        new TypeReference<>() {});
        List<String> messageIds = caseEventMessages
            .stream()
            .map(caseEventMessage -> caseEventMessage.getMessageId())
            .collect(Collectors.toList());

        assertThat(messageIds).asList()
            .containsSubsequence(messageId);

        deleteMessagesFromDatabaseByMsgIds(messageId);
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
            .userId("insert_true")
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

    private Response postJobMessageEndpoint(String s2sToken,
                                            String jobName) {
        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .post("/messages/jobs/" + jobName);
    }
}
