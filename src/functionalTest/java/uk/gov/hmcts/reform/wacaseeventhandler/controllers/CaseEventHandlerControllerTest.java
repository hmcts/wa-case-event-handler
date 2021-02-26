package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.azure.messaging.servicebus.ServiceBusMessage;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    private String taskToTearDown;

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_time_extension_category_then_cancel_task() {
        // Given multiple existing tasks

        // create task1
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "decideOnTimeExtension";
        String task1Id = initiateTaskForGivenId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "",
            false,
            taskIdDmnColumn
        );

        // create task2
        String caseIdForTask2 = UUID.randomUUID().toString();
        String task2Id = initiateTaskForGivenId(
            caseIdForTask2,
            "submitTimeExtension",
            "", "", false,
            taskIdDmnColumn
        );

        // Then cancel the task1
        String eventToCancelTask = "submitReasonsForAppeal";
        String previousStateToCancelTask = "awaitingReasonsForAppeal";
        sendMessage(caseIdForTask1, eventToCancelTask, previousStateToCancelTask,
            "", false);

        // Assert the task1 is deleted
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(task1Id, "deleted");

        // tear down task2
        taskToTearDown = task2Id;
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_task() {
        // Given multiple existing tasks

        // create task1,
        // notice this creates two tasks with the follow up category because the initiate dmn table
        // has multiple rules matching this event and state.
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        String task1Id = initiateTaskForGivenId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn
        );

        // Then cancel the task1
        String eventToCancelTask = "uploadHomeOfficeBundle";
        String previousStateToCancelTask = "awaitingRespondentEvidence";
        sendMessage(caseIdForTask1, eventToCancelTask, previousStateToCancelTask, "", false);

        // Assert the task1 is deleted
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(task1Id, "deleted");

        // add tasks to tear down.
        String taskCreatedAsResultOfTheMultipleDmnRule = findTaskForGivenCaseId(
            caseIdForTask1,
            "provideRespondentEvidence"
        );
        taskToTearDown = taskCreatedAsResultOfTheMultipleDmnRule;
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_with_followup_overdue_than_cancel_task() {
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        final String task1Id = initiateTaskForGivenId(caseIdForTask1, "requestRespondentEvidence",
            "", "awaitingRespondentEvidence",
            false, taskIdDmnColumn);

        // Then cancel the task1
        sendMessage(caseIdForTask1, "uploadHomeOfficeBundle", "awaitingRespondentEvidence", "", false);

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);

        assertTaskDeleteReason(task1Id, "deleted");
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_with_followup_overdue_than_cancel_task() {
        // create task1
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "followUpOverdueCaseBuilding";
        final String task1Id = initiateTaskForGivenId(caseIdForTask1, "requestCaseBuilding",
            "", "caseBuilding",
            true, taskIdDmnColumn);
        // Then cancel the task1
        sendMessage(caseIdForTask1, "submitCase", "caseBuilding", "", false);

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(task1Id, "deleted");
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_follow_up_overdue_category_then_warn_task_with_no() {
        // Given multiple existing tasks

        // create task1,
        // notice this creates two tasks with the follow up category because the initiate dmn table
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "allocateFtpaToJudge";
        String task1Id = initiateTaskForGivenId(
            caseIdForTask1,
            "applyForFTPAAppellant",
            "", "", true,
            taskIdDmnColumn
        );

        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .body("{\"value\" : \"true\", \"type\" : \"boolean\"}")
            .baseUri(camundaUrl)
            .basePath("/task")
            .when()
            .put("/{id}/localVariables/hasWarnings", task1Id)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        sendMessage(caseIdForTask1, "makeAnApplication",
                    "", "", false);

        waitSeconds(17);
        assertTaskHasWarnings(caseIdForTask1,task1Id,true);
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_and_without_followup_overdue_then_complete_task() {
        String caseIdForTask2 = UUID.randomUUID().toString();
        final String taskId = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
            "", "",
            true, "processApplication");

        // add tasks to tear down.
        taskToTearDown = taskId;
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_and_without_followup_overdue_then_complete_task() {
        String caseIdForTask2 = UUID.randomUUID().toString();
        final String taskId = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
            "", "",
            false, "processApplication");

        // add tasks to tear down.
        taskToTearDown = taskId;
    }

    private void assertTaskDeleteReason(String task1Id, String expectedDeletedReason) {
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

    private void assertTaskDoesNotExist(String caseId, String taskIdDmnColumn) {
        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task")
            .param(
                "processVariables",
                "caseId_eq_" + caseId + ",taskId_eq_" + taskIdDmnColumn
            )
            .when()
            .get()
            .then()
            .body("size()", is(0));
    }


    private void assertTaskHasWarnings(String caseId, String taskId, boolean hasWarningValue) {
        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task")
            .when()
            .get("/{id}/variables", taskId)
            .prettyPeek()
            .then()
            .body("hasWarnings.value", is(hasWarningValue));
    }

    private void sendMessage(String caseId, String event, String previousStateId,
                             String newStateId, boolean taskDelay) {
        LocalDateTime delayTimer = LocalDateTime.now();

        if (taskDelay) {
            delayTimer = LocalDateTime.now().plusSeconds(2);
        }
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("eventInstanceId")
            .eventTimeStamp(delayTimer)
            .caseId(caseId)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .userId("some user Id")
            .build();

        if (publisher != null) {
            publishMessageToTopic(eventInformation);

            waitSeconds(2);
        } else {
            callRestEndpoint(eventInformation);
        }
    }

    private void callRestEndpoint(EventInformation eventInformation) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private void publishMessageToTopic(EventInformation eventInformation) {
        String jsonMessage = asJsonString(eventInformation);
        ServiceBusMessage message = new ServiceBusMessage(jsonMessage.getBytes());
        message.setSessionId(eventInformation.getCaseId());

        publisher.sendMessage(message);
    }

    private String initiateTaskForGivenId(String caseId, String eventId,
                                          String previousStateId, String newStateId,
                                          boolean delayUntil, String taskIdDmnColumn) {

        sendMessage(caseId, eventId, previousStateId, newStateId, delayUntil);

        // if the delayUntil is true, then the taskCreation process waits for delayUntil timer
        // to expire. The task is delayed for 2 seconds,
        // so manually waiting for 5 seconds for process to start
        if (delayUntil) {
            waitSeconds(10);
        } else {
            waitSeconds(5);
        }

        return findTaskForGivenCaseId(caseId, taskIdDmnColumn);
    }

    private String findTaskForGivenCaseId(String caseId, String taskIdDmnColumn) {

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
                        .body("[0].formKey", is(taskIdDmnColumn))
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


    @After
    public void cleanUpTask() {
        if (StringUtils.isNotEmpty(taskToTearDown)) {
            given()
                .header(SERVICE_AUTHORIZATION, s2sToken)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .when()
                .post(camundaUrl + "/task/{task-id}/complete", taskToTearDown);

            assertTaskDeleteReason(taskToTearDown, "completed");
        }
    }

}
