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
import java.util.Arrays;
import java.util.List;
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
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_all_tasks() {

        // notice this creates two tasks with the follow up category because the initiate dmn table
        // has multiple rules matching this event and state.
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // task1
        String task1Id = initiateTaskForGivenId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn
        );

        // task2
        String task2Id = findTaskForGivenCaseId(
            caseIdForTask1,
            "provideRespondentEvidence"
        );

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask, "", "", false);

        waitSeconds(5);
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDoesNotExist(task2Id, "provideRespondentEvidence");

        assertTaskDeleteReason(task1Id, "deleted");
        assertTaskDeleteReason(task2Id, "deleted");

        // add tasks to tear down.
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "deleted");
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_different_categories_then_cancel_all_tasks() {

        String caseIdForTask1 = UUID.randomUUID().toString();
        String task1IdDmnColumn = "reviewTheAppeal";

        // task1 with category Case progression
        String task1Id = initiateTaskForGivenId(
            caseIdForTask1,
            "submitAppeal",
            "", "appealSubmitted", false,
            task1IdDmnColumn
        );

        // task2 with category Time Extension
        String task2IdDmnColumn = "decideOnTimeExtension";
        String task2Id = initiateTaskForGivenId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "", false,
            task2IdDmnColumn
        );

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask, "", "", false);

        waitSeconds(5);
        assertTaskDoesNotExist(caseIdForTask1, task1IdDmnColumn);
        assertTaskDoesNotExist(caseIdForTask1, task2IdDmnColumn);

        assertTaskDeleteReason(task1Id, "deleted");
        assertTaskDeleteReason(task2Id, "deleted");

        // add tasks to tear down.
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "deleted");
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

        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "allocateFtpaToJudge";
        String task1Id = initiateTaskForGivenId(
            caseIdForTask1,
            "applyForFTPAAppellant",
            "", "", false,
            taskIdDmnColumn
        );

        sendMessage(caseIdForTask1, "makeAnApplication",
                    "", "", false);

        waitSeconds(5);

        assertTaskHasWarnings(caseIdForTask1,task1Id,true);

        taskToTearDown = task1Id;
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_caseId_with_multiple_tasks_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "allocateFtpaToJudge";

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "applyForFTPAAppellant", null,
                    null, false);

        waitSeconds(5);

        AtomicReference<Response> response = findTaskProcessVariables(
            caseIdForTask1, taskIdDmnColumn, 1);

        String task1Id = response.get()
            .then()
            .body("size()", is(1))
            .body("[0].formKey", is(taskIdDmnColumn))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // initiate task2, category (Case progression)
        sendMessage(caseIdForTask1, "applyForFTPARespondent", null,
                    null, false);
        waitSeconds(5);
        response = findTaskProcessVariables(
            caseIdForTask1, taskIdDmnColumn, 2);

        String task2Id = response.get()
            .then()
            .body("size()", is(2))
            .body("[1].formKey", is(taskIdDmnColumn))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessage(caseIdForTask1, "makeAnApplication",
                    "", "", false);

        waitSeconds(5);
        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseIdForTask1,task1Id,true);
        assertTaskHasWarnings(caseIdForTask1,task2Id,true);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "completed");
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_caseId_and_multiple_tasks_and_different_ctg_when_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "decideOnTimeExtension";

        // Initiate task1 , category (Time extension)
        sendMessage(caseIdForTask1, "submitTimeExtension", "",
                    null, false);

        waitSeconds(5);

        AtomicReference<Response> response = findTaskProcessVariables(
            caseIdForTask1, taskIdDmnColumn, 1);

        String task1Id = response.get()
            .then()
            .body("[0].formKey", is(taskIdDmnColumn))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // initiate task2, category (Case progression)
        taskIdDmnColumn = "allocateFtpaToJudge";
        sendMessage(caseIdForTask1, "applyForFTPARespondent", null,
                    null, false);
        waitSeconds(5);
        response = findTaskProcessVariables(
            caseIdForTask1, taskIdDmnColumn, 2);

        String task2Id = response.get()
            .then()
            .body("size()", is(2))
            .body("[1].formKey", is(taskIdDmnColumn))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false);

        waitSeconds(5);
        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseIdForTask1,task1Id,true);
        assertTaskHasWarnings(caseIdForTask1,task2Id,true);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "completed");
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_and_without_followup_overdue_then_complete_task() {
        String caseIdForTask2 = UUID.randomUUID().toString();
        final String taskId = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
            "", "appealSubmitted",
            true, "reviewTheAppeal");

        // add tasks to tear down.
        taskToTearDown = taskId;
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_and_without_followup_overdue_then_complete_task() {
        String caseIdForTask2 = UUID.randomUUID().toString();
        final String taskId = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
            "", "appealSubmitted",
            false, "reviewTheAppeal");

        // add tasks to tear down.
        taskToTearDown = taskId;
    }

    @Test
    public void given_multiple_caseIDs_when_action_is_initiate_then_complete_all_tasks() {
        String caseIdForTask1 = UUID.randomUUID().toString();
        final String taskId = initiateTaskForGivenId(caseIdForTask1, "submitAppeal",
                                                     "", "appealSubmitted",
                                                     false, "reviewTheAppeal"
        );

        String caseIdForTask2 = UUID.randomUUID().toString();
        final String task2Id = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
                                                      "", "appealSubmitted",
                                                      false, "reviewTheAppeal"
        );

        // add tasks to tear down.
        tearDownMultipleTasks(Arrays.asList(taskId, task2Id), "completed");
    }

    @Test
    public void given_multiple_caseIDs_when_action_is_cancel_then_cancels_all_tasks() {
        String caseId1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // caseId1 with category Followup overdue
        // task1
        final String caseId1Task1Id = initiateTaskForGivenId(
            caseId1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn
        );

        // task2
        final String caseId1Task2Id = findTaskForGivenCaseId(
            caseId1,
            "provideRespondentEvidence"
        );

        // caseId2 with category Case progression
        String taskId2DmnColumn = "allocateFtpaToJudge";
        String caseId2 = UUID.randomUUID().toString();
        final String caseId2Task1Id = initiateTaskForGivenId(caseId2, "applyForFTPAAppellant",
                                                             "", "",
                                                             false, taskId2DmnColumn);
        // Then cancel all tasks on both caseIDs
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseId1, eventToCancelTask, "", "", false);
        waitSeconds(5);
        sendMessage(caseId2, eventToCancelTask, "", "", false);
        waitSeconds(5);

        assertTaskDoesNotExist(caseId1, taskIdDmnColumn);
        assertTaskDoesNotExist(caseId1, "provideRespondentEvidence");
        assertTaskDoesNotExist(caseId2, taskId2DmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");
        assertTaskDeleteReason(caseId1Task2Id, "deleted");
        assertTaskDeleteReason(caseId2Task1Id, "deleted");

        // add tasks to tear down.
        tearDownMultipleTasks(Arrays.asList(caseId1Task1Id, caseId1Task1Id,
                                            caseId2Task1Id), "deleted");
    }

    @Test
    public void given_multiple_caseIDs_when_actions_is_warn_then_mark_all_tasks_with_warnings() {
        //caseId1 with category Case progression
        String caseId1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "attendCma";
        final String caseId1Task1Id = initiateTaskForGivenId(
            caseId1,
            "listCma",
            "", "cmaListed", false,
            taskIdDmnColumn
        );

        //caseId1 with category Case progression
        String taskId2DmnColumn = "reviewRespondentResponse";
        String caseId2 = UUID.randomUUID().toString();
        final String caseId2Task1Id = initiateTaskForGivenId(caseId2, "uploadHomeOfficeAppealResponse",
                                                             "", "respondentReview",
                                                             false, taskId2DmnColumn);
        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "makeAnApplication",
                    "", "", false);
        waitSeconds(5);
        sendMessage(caseId2, "makeAnApplication",
                    "", "", false);
        waitSeconds(5);

        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseId1,caseId1Task1Id,true);
        assertTaskHasWarnings(caseId2,caseId2Task1Id,true);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(caseId1Task1Id, caseId2Task1Id), "completed");
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
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
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
                        .body("hasWarnings.value", is(hasWarningValue));

                    return true;
                });
    }

    private void sendMessage(String caseId, String event, String previousStateId,
                             String newStateId, boolean taskDelay) {
        LocalDateTime delayTimer = LocalDateTime.now();

        if (taskDelay) {
            delayTimer = LocalDateTime.now().plusSeconds(2);
        }
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
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

    private AtomicReference<Response> findTaskProcessVariablesWithIdDmnColumn(
        String caseId, String taskIdDmnColumn, int tasks
    ) {

        log.info(String.format("Finding task for caseId : %s", caseId));
        AtomicReference<Response> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(1000, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    final Response result = given()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .basePath("/task")
                        .param("processVariables", "caseId_eq_" + caseId + ",taskId_eq_" + taskIdDmnColumn)
                        .when()
                        .get();

                    result
                        .then()
                        .body("size()", is(tasks));
                    response.set(result);
                    return true;
                });

        return response;
    }

    private AtomicReference<Response> findTaskProcessVariables(
        String caseId, String taskIdDmnColumn, int tasks
    ) {

        log.info(String.format("Finding task for caseId : %s", caseId));
        AtomicReference<Response> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(1000, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    final Response result = given()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .basePath("/task")
                        .param("processVariables", "caseId_eq_" + caseId)
                        .when()
                        .get();

                    result
                        .then()
                        .body("size()", is(tasks));
                    response.set(result);
                    return true;
                });

        return response;
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

    private void completeTask(String taskId, String status) {
        log.info(String.format("Completing task : %s", taskId));
        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .accept(APPLICATION_JSON_VALUE)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .post(camundaUrl + "/task/{task-id}/complete", taskId);

        assertTaskDeleteReason(taskId, status);
    }

    private void tearDownMultipleTasks(List<String> tasks, String status) {
        tasks.forEach(task -> completeTask(task, status));
    }


    @After
    public void cleanUpTask() {
        if (StringUtils.isNotEmpty(taskToTearDown)) {
            completeTask(taskToTearDown, "completed");
        }
    }

}
