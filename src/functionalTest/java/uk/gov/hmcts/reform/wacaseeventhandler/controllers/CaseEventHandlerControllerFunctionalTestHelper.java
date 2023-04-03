package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.azure.messaging.servicebus.ServiceBusMessage;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public abstract class CaseEventHandlerControllerFunctionalTestHelper extends SpringBootFunctionalBaseTest {

    @Autowired
    private DueDateService dueDateService;

    protected LocalDateTime eventTimeStamp;
    protected Map<String, String> taskIdStatusMap;
    protected String caseId1Task1Id;
    protected String caseId1Task2Id;
    protected String caseId2Task1Id;
    protected String caseId2Task2Id;
    protected TestAuthenticationCredentials caseworkerCredentials;

    protected void assertTaskDeleteReason(String task1Id, String expectedDeletedReason) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .accept(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .baseUri(camundaUrl)
            .when()
            .get("/history/task?taskId=" + task1Id)
            .then()
            .body("[0].deleteReason", is(expectedDeletedReason));
    }

    protected void assertTaskDoesNotExist(String caseId, String taskId) {
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    given()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .basePath("/task")
                        .param(
                            "processVariables",
                            "caseId_eq_" + caseId + ",taskId_eq_" + taskId
                        )
                        .when()
                        .get()
                        .then()
                        .body("size()", is(0));
                    return true;
                });
    }

    protected void assertTaskHasWarnings(String caseId, String taskId, boolean hasWarningValue) {
        log.info("Finding warnings task for caseId = {} and taskId = {}", caseId, taskId);
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {

                    Response result = given()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .when()
                        .get("/task/{id}/variables", taskId);

                    result.then()
                        .body("caseId.value", is(caseId))
                        .body("hasWarnings.value", is(hasWarningValue))
                        .body("warningList.value",
                            is("[{\"warningCode\":\"TA01\","
                               + "\"warningText\":\"There is an application task which "
                               + "might impact other active tasks\"}]"));

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
        } else {
            eventTimeStamp = LocalDateTime.now().minusDays(1);
        }
        EventInformation eventInformation = getEventInformation(
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
            waitSeconds(2);
        } else {
            callRestEndpoint(s2sToken, eventInformation);
        }
    }

    protected void sendMessageWithAdditionalData(String caseId, String event, String previousStateId,
                                                 String newStateId, boolean taskDelay, String appealType) {

        if (taskDelay) {
            eventTimeStamp = LocalDateTime.now().plusSeconds(2);
        } else {
            eventTimeStamp = LocalDateTime.now().minusDays(1);
        }
        EventInformation eventInformation = getEventInformationWithAdditionalData(
            caseId, event, previousStateId, newStateId, eventTimeStamp, appealType
        );

        if (publisher != null) {
            publishMessageToTopic(eventInformation);
        } else {
            callRestEndpoint(s2sToken, eventInformation);
        }
    }

    protected EventInformation getEventInformation(String caseId,
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
            .additionalData(setAdditionalData(""))
            .userId("some user Id")
            .build();
    }

    protected EventInformation getEventInformationWithAdditionalData(String caseId, String event,
                                                                     String previousStateId,
                                                                     String newStateId, LocalDateTime localDateTime,
                                                                     String appealType) {
        return EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(localDateTime)
            .caseId(caseId)
            .jurisdictionId("WA")
            .caseTypeId("WaCaseType")
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .additionalData(setAdditionalData(appealType))
            .userId("some user Id")
            .build();
    }

    protected void callRestEndpoint(String s2sToken, EventInformation eventInformation) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    protected void publishMessageToTopic(EventInformation eventInformation) {
        String jsonMessage = asJsonString(eventInformation);
        ServiceBusMessage message = new ServiceBusMessage(jsonMessage.getBytes());
        message.setSessionId(eventInformation.getCaseId());

        publisher.sendMessage(message);
    }

    protected String createTaskWithId(String caseId,
                                      String eventId,
                                      String previousStateId,
                                      String newStateId,
                                      boolean delayUntil,
                                      String outcomeTaskId,
                                      String jurisdictionId,
                                      String caseTypeId) {

        sendMessage(caseId, eventId, previousStateId, newStateId, delayUntil, jurisdictionId, caseTypeId);

        // if the delayUntil is true, then the taskCreation process waits for delayUntil timer
        // to expire. The task is delayed for 2 seconds,
        // so manually waiting for 5 seconds for process to start
        if (delayUntil) {
            waitSeconds(10);
        } else {
            waitSeconds(5);
        }

        return findTaskForGivenCaseId(caseId, outcomeTaskId);
    }

    protected Response findTasksByCaseId(
        String caseId, int expectedTaskAmount
    ) {

        log.info("Finding task for caseId = {}", caseId);
        AtomicReference<Response> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(1000, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    Response result = given()
                        .relaxedHTTPSValidation()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .basePath("/task")
                        .param("processVariables", "caseId_eq_" + caseId)
                        .when()
                        .get();

                    result
                        .then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .body("size()", is(expectedTaskAmount));

                    response.set(result);
                    return true;
                });

        return response.get();
    }

    protected Response findTaskDetailsForGivenTaskId(String taskId) {
        log.info("Attempting to retrieve task details with taskId = {}", taskId);

        return given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task/" + taskId + "/variables")
            .when()
            .get();
    }

    protected String findTaskForGivenCaseId(String caseId, String taskIdDmnColumn) {

        log.info("Attempting to retrieve task with caseId = {} and taskId = {}", caseId, taskIdDmnColumn);
        String filter = "?processVariables=caseId_eq_" + caseId + ",taskId_eq_" + taskIdDmnColumn;

        AtomicReference<String> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {

                    Response result = given()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .when()
                        .get("/task" + filter);

                    result.then()
                        .body("size()", is(1))
                        .assertThat().body("[0].id", notNullValue());

                    response.set(
                        result.then()
                            .extract()
                            .path("[0].id")
                    );

                    return true;
                });

        return response.get();
    }

    public void completeTask(String taskId, String status) {
        log.info(String.format("Completing task : %s", taskId));
        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .accept(APPLICATION_JSON_VALUE)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .post(camundaUrl + "/task/{task-id}/complete", taskId);

    }

    protected AdditionalData setAdditionalData(String appealType) {
        String type = StringUtils.isNotBlank(appealType) ? appealType : "protection";
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of(
                "dateDue", "",
                "uniqueId", "",
                "directionType", ""
            ),
            "appealType", type,
            "lastModifiedApplication", Map.of("type", "Judge's review of application decision",
                "decision", "")
        );

        return AdditionalData.builder()
            .data(dataMap)
            .build();
    }

    protected void assertDelayDuration(Response result) {
        Map<String, Object> mapJson = result.jsonPath().get("dueDate");
        final String dueDateVal = (String) mapJson.get("value");
        final LocalDateTime dueDateTime = LocalDateTime.parse(dueDateVal);

        mapJson = result.jsonPath().get("delayUntil");
        final String delayUntil = (String) mapJson.get("value");
        final LocalDateTime delayUntilDateTime = LocalDateTime.parse(
            delayUntil,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        mapJson = result.jsonPath().get("workingDaysAllowed");
        int workingDaysLocal = (Integer) mapJson.get("value");

        ZoneId zoneId = ZoneId.of("Europe/London");
        ZonedDateTime zonedDateTimeStamp = eventTimeStamp.atZone(zoneId);
        final ZonedDateTime expectedDueDate = dueDateService.calculateDueDate(zonedDateTimeStamp, workingDaysLocal);

        assertAll(
            () -> assertEquals(expectedDueDate.getYear(), dueDateTime.getYear()),
            () -> assertEquals(expectedDueDate.getMonthValue(), dueDateTime.getMonthValue()),
            () -> assertEquals(expectedDueDate.getDayOfMonth(), dueDateTime.getDayOfMonth()),
            () -> assertEquals(16, dueDateTime.getHour()),
            () -> assertEquals(0, dueDateTime.getMinute()),
            () -> assertEquals(0, dueDateTime.getSecond()),
            () -> assertEquals(0, dueDateTime.getNano()),
            () -> assertEquals(eventTimeStamp.getYear(), delayUntilDateTime.getYear()),
            () -> assertEquals(eventTimeStamp.getMonthValue(), delayUntilDateTime.getMonthValue()),
            () -> assertEquals(eventTimeStamp.getDayOfMonth(), delayUntilDateTime.getDayOfMonth()),
            () -> assertEquals(eventTimeStamp.getHour(), delayUntilDateTime.getHour()),
            () -> assertEquals(eventTimeStamp.getMinute(), delayUntilDateTime.getMinute()),
            () -> assertEquals(eventTimeStamp.getSecond(), delayUntilDateTime.getSecond()),
            () -> assertEquals(eventTimeStamp.getNano(), delayUntilDateTime.getNano())
        );
    }
}
