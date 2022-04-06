package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CaseEventHandlerControllerTest extends CaseEventHandlerControllerTestHelper {

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.now().minusDays(1);
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");

        taskIdStatusMap = new HashMap<>();
        caseId1Task1Id = "";
        caseId1Task2Id = "";
        caseId2Task1Id = "";
        caseId2Task2Id = "";
    }

    @After
    public void cleanUp() {
        taskIdStatusMap.forEach((key, value) -> completeTask(key, value));
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        common.cleanUpTask(caseworkerCredentials.getHeaders(), caseIds);
    }

    @Test
    public void should_succeed_and_create_a_task_with_no_categories() {

        String caseId = getCaseId();

        sendMessage(
            caseId,
            "changeDirectionDueDate",
            "",
            "",
            false,
            "IA",
            "Asylum"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", is("Asylum"))
            .body("jurisdiction.value", is("IA"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", is("unassigned"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseId))
            .body("name.value", is("Follow-up extended direction"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", is("followUpExtendedDirection"))
            .body("warningList.value", is("[]"));

    }

    @Test
    public void should_succeed_and_create_a_task_with_single_categories() {

        String caseId = getCaseId();

        sendMessageWithAdditionalData(
            caseId,
            "submitAppeal",
            "",
            "appealSubmitted",
            false,
            "revocationOfProtection"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", is("Asylum"))
            .body("idempotencyKey.value", notNullValue())
            .body("jurisdiction.value", is("IA"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", is("unassigned"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseId))
            .body("name.value", is("Review the appeal"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", is("reviewTheAppeal"))
            .body("caseId.value", is(caseId))
            .body("__processCategory__caseProgression.value", is(true))
            .body("hasWarnings.value", is(false))
            .body("warningList.value", is("[]"));

    }

    @Test
    @Ignore("non-existing requirement for IA")
    public void should_succeed_and_create_a_task_with_multiple_categories() {

        String caseId = getCaseId();

        sendMessage(
            caseId,
            "dummyEventForMultipleCategories",
            "",
            "",
            false, "IA", "Asylum"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", is("Asylum"))
            .body("idempotencyKey.value", notNullValue())
            .body("jurisdiction.value", is("IA"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", is("unassigned"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseId))
            .body("name.value", is("Test task to test multiple categories"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", is("testTaskIdForMultipleCategories"))
            .body("caseId.value", is(caseId))
            .body("__processCategory__caseProgression.value", is(true))
            .body("__processCategory__followUpOverdue.value", is(true))
            .body("hasWarnings.value", is(false))
            .body("warningList.value", is("[]"));

    }

    @Test
    @Ignore("non-existing requirement for IA")
    public void should_cancel_a_task_with_multiple_categories() {

        String caseId = getCaseId();

        sendMessage(
            caseId,
            "dummyEventForMultipleCategories",
            "",
            "",
            false, "IA", "Asylum"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        sendMessage(
            caseId,
            "dummyEventForMultipleCategoriesCancel",
            "",
            "",
            false, "IA", "Asylum"
        );

        // Assert the task was deleted
        assertTaskDoesNotExist(caseId, "testTaskIdForMultipleCategories");

    }

    @Test
    @Ignore("non-existing requirement for IA")
    public void should_warn_a_task_with_multiple_categories() {

        String caseId = getCaseId();

        sendMessage(
            caseId,
            "dummyEventForMultipleCategories",
            "",
            "",
            false, "IA", "Asylum"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        sendMessage(
            caseId,
            "dummyEventForMultipleCategoriesWarn",
            "",
            "",
            false, "IA", "Asylum"
        );

        // Assert the task warning was set
        assertTaskHasWarnings(caseId, caseId1Task1Id, true);

    }

    /**
     * This FT sends additionalData to DMN in json format to evaluate appealType.
     * Disabled as the checkFeeStatus task is created with delayUntil to 28 days and can't be
     * retrieved until the task is active.
     * When the DMN is deployed onto new wa jurisdiction, this test can be enabled
     * with delayUntil as 0.
     */
    @Test
    @Ignore("CheckFeeStatus task cannot be retrieved")
    public void given_event_submitAppeal_when_appealType_sent_as_json_then_initiate_task() {

        String caseId = getCaseId();

        sendMessageWithAdditionalData(
            caseId,
            "submitAppeal",
            "",
            "appealSubmitted",
            false,
            "revocationOfProtection"
        );

        Response taskFound = findTasksByCaseId(caseId, 2);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("taskId.value", is("checkFeeStatus"));

        caseId1Task2Id = taskFound
            .then().assertThat()
            .body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        taskIdStatusMap.put(caseId1Task2Id, "completed");

        response = findTaskDetailsForGivenTaskId(caseId1Task2Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("taskId.value", is("reviewTheAppeal"));

    }

    @Test
    public void given_initiate_tasks_with_time_extension_category_then_cancel_task() {
        // Given multiple existing tasks

        // DST (Day saving time) started on March 29th 2020 at 1:00am
        eventTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975");

        // create task1

        String caseIdForTask1 = getCaseId();

        String taskIdDmnColumn = "decideOnTimeExtension";
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "",
            false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // test for workingDaysAllowed  = 2
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // create task2
        String caseIdForTask2 = getCaseId();
        caseId1Task2Id = createTaskWithId(
            caseIdForTask2,
            "submitTimeExtension",
            "", "", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task2Id, "completed");

        // test for workingDaysAllowed  = 2
        responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task2Id);
        assertDelayDuration(responseTaskDetails);

        // Then cancel the task1
        String eventToCancelTask = "submitReasonsForAppeal";
        String previousStateToCancelTask = "awaitingReasonsForAppeal";
        sendMessage(caseIdForTask1, eventToCancelTask, previousStateToCancelTask,
            "", false, "IA", "Asylum"
        );

        // Assert the task1 is deleted
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_task() {
        // Given multiple existing tasks
        // create task1,
        // notice this creates only one task with the follow up category
        String caseIdForTask1 = getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // Then cancel the task1
        String eventToCancelTask = "uploadHomeOfficeBundle";
        String previousStateToCancelTask = "awaitingRespondentEvidence";
        sendMessage(caseIdForTask1, eventToCancelTask,
            previousStateToCancelTask, "", false, "IA", "Asylum");

        waitSeconds(5);
        // Assert the task1 is deleted
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_all_tasks() {

        eventTimeStamp = LocalDateTime.parse("2020-02-27T12:56:19.403975");

        // notice this creates one task with the follow up category
        String caseIdForTask1 = getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // task1
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask,
            "", "", false, "IA", "Asylum");

        waitSeconds(5);
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_different_categories_then_cancel_all_tasks() {

        String caseIdForTask1 = getCaseId();
        String task1IdDmnColumn = "reviewRespondentEvidence";

        // task1 with category Case progression
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence", false,
            task1IdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // task2 with category Time Extension
        String task2IdDmnColumn = "decideOnTimeExtension";
        caseId1Task2Id = createTaskWithId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "", false,
            task2IdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task2Id, "deleted");

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask,
            "", "", false, "IA", "Asylum");

        waitSeconds(5);
        assertTaskDoesNotExist(caseIdForTask1, task1IdDmnColumn);
        assertTaskDoesNotExist(caseIdForTask1, task2IdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");
        assertTaskDeleteReason(caseId1Task2Id, "deleted");

    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_with_followup_overdue_than_cancel_task() {

        String caseIdForTask1 = getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        caseId1Task1Id = createTaskWithId(caseIdForTask1, "requestRespondentEvidence",
            "", "awaitingRespondentEvidence",
            false, taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // Then cancel the task1
        sendMessage(caseIdForTask1, "uploadHomeOfficeBundle",
            "awaitingRespondentEvidence", "", false, "IA", "Asylum");

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_with_followup_overdue_than_cancel_task() {
        // create task1
        String caseIdForTask1 = getCaseId();
        String taskIdDmnColumn = "followUpOverdueCaseBuilding";
        caseId1Task1Id = createTaskWithId(caseIdForTask1, "requestCaseBuilding",
            "", "caseBuilding",
            true, taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // Then cancel the task1
        sendMessage(caseIdForTask1, "buildCase", "caseBuilding", "", false, "IA", "Asylum");

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_initiate_tasks_with_follow_up_overdue_category_then_warn_task_with_no_category() {
        // Given multiple existing tasks
        String caseIdForTask1 = getCaseId();
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestReasonsForAppeal",
            "", "awaitingReasonsForAppeal", false,
            "followUpOverdueReasonsForAppeal", "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );

        waitSeconds(5);

        Response taskFound = findTasksByCaseId(caseIdForTask1, 2);

        caseId1Task2Id = taskFound
            .then().assertThat()
            .body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        taskIdStatusMap.put(caseId1Task2Id, "completed");

        assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);

    }

    @Test
    public void given_caseId_with_multiple_tasks_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "submitReasonsForAppeal", null,
            "reasonsForAppealSubmitted", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // initiate task2, category (Case progression)
        sendMessage(caseIdForTask1, "draftHearingRequirements", null,
            "listing", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(caseIdForTask1, 2);

        caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        taskIdStatusMap.put(caseId1Task2Id, "completed");

        // send warning message
        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );

        waitSeconds(5);

        response = findTasksByCaseId(
            caseIdForTask1, 3);

        final String caseId1Task3Id = response
            .then()
            .body("size()", is(3))
            .assertThat().body("[2].id", notNullValue())
            .extract()
            .path("[2].id");

        taskIdStatusMap.put(caseId1Task3Id, "completed");

        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
        assertTaskHasWarnings(caseIdForTask1, caseId1Task2Id, true);

    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_caseId_and_multiple_tasks_and_different_ctg_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1 , category (caseProgression)
        sendMessage(caseIdForTask1, "requestRespondentEvidence", null,
            "awaitingRespondentEvidence", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        // initiate task2, category (followUpOverdue)
        sendMessage(caseIdForTask1, "sendDirectionWithQuestions", "",
            "awaitingClarifyingQuestionsAnswers", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        taskIdStatusMap.put(caseId1Task2Id, "completed");

        // send warning message
        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );

        waitSeconds(5);

        response = findTasksByCaseId(caseIdForTask1, 3);

        String caseId1Task3Id = response
            .then().assertThat()
            .body("[2].id", notNullValue())
            .extract()
            .path("[2].id");

        taskIdStatusMap.put(caseId1Task3Id, "completed");

        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
        assertTaskHasWarnings(caseIdForTask1, caseId1Task2Id, true);

    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_and_without_followup_overdue_then_complete_task() {

        String caseId = getCaseId();
        caseId1Task1Id = createTaskWithId(
            caseId,
            "uploadHomeOfficeBundle",
            "",
            "awaitingRespondentEvidence",
            true,
            "reviewRespondentEvidence", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_and_without_followup_overdue_then_complete_task() {

        String caseId = getCaseId();
        caseId1Task1Id = createTaskWithId(caseId, "generateDecisionAndReasons",
            "", "decision",
            false, "sendDecisionsAndReasons", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    @Test
    public void given_multiple_caseIDs_when_action_is_initiate_then_complete_all_tasks() {
        // DST (Day saving time) ended on October 25th 2020 at 2:00am.
        eventTimeStamp = LocalDateTime.parse("2020-10-23T12:56:19.403975");

        String caseIdForTask1 = getCaseId();
        caseId1Task1Id = createTaskWithId(caseIdForTask1, "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence",
            false, "reviewRespondentEvidence", "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        // test for workingDaysAllowed  = 2
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);


        String caseIdForTask2 = getCaseId();
        caseId1Task2Id = createTaskWithId(caseIdForTask2, "submitReasonsForAppeal",
            "", "reasonsForAppealSubmitted",
            false, "reviewReasonsForAppeal", "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task2Id, "completed");
    }

    @Test
    public void given_multiple_caseIDs_when_action_is_cancel_then_cancels_all_tasks() {

        String caseId1 = getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // caseId1 with category Followup overdue
        // task1
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // caseId2 with category Case progression
        String caseId2 = getCaseId();
        String taskId2DmnColumn = "reviewAppealSkeletonArgument";
        caseId2Task1Id = createTaskWithId(caseId2, "submitCase",
            "", "caseUnderReview",
            false, taskId2DmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId2Task1Id, "deleted");

        // Then cancel all tasks on both caseIDs
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseId1, eventToCancelTask,
            "", "", false, "IA", "Asylum");
        waitSeconds(5);
        sendMessage(caseId2, eventToCancelTask,
            "", "", false, "IA", "Asylum");
        waitSeconds(5);

        assertTaskDoesNotExist(caseId1, taskIdDmnColumn);
        assertTaskDoesNotExist(caseId2, taskId2DmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");
        assertTaskDeleteReason(caseId2Task1Id, "deleted");

    }

    @Test
    public void given_multiple_caseIDs_when_actions_is_warn_then_mark_all_tasks_with_warnings() {
        //caseId1 with category Case progression
        String caseId1 = getCaseId();
        String taskIdDmnColumn = "reviewRespondentEvidence";
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        //caseId1 with category Case progression
        String caseId2 = getCaseId();
        String taskId2DmnColumn = "reviewRespondentEvidence";
        caseId2Task1Id = createTaskWithId(caseId2, "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence",
            false, taskId2DmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId2Task1Id, "completed");

        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);

        Response taskFound = findTasksByCaseId(caseId1, 2);

        caseId1Task2Id = taskFound
            .then().assertThat()
            .body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        taskIdStatusMap.put(caseId1Task2Id, "completed");

        sendMessage(caseId2, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);

        taskFound = findTasksByCaseId(caseId2, 2);

        caseId2Task2Id = taskFound
            .then().assertThat()
            .body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        taskIdStatusMap.put(caseId2Task2Id, "completed");

        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseId1, caseId1Task1Id, true);
        assertTaskHasWarnings(caseId2, caseId2Task1Id, true);

    }

    @Test
    public void given_an_event_when_directionDueDate_is_empty_then_task_should_start_without_delay() {


        String caseIdForTask = getCaseId();

        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", ""),
            "appealType", "protection"
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(LocalDateTime.now().minusDays(1))
            .caseId(caseIdForTask)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("requestCaseBuilding")
            .newStateId("caseBuilding")
            .previousStateId(null)
            .userId("some user Id")
            .additionalData(additionalData)
            .build();

        callRestEndpoint(s2sToken, eventInformation);

        caseId1Task1Id = findTaskForGivenCaseId(
            caseIdForTask,
            "followUpOverdueCaseBuilding"
        );

        // add tasks to tear down.
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    @Test
    public void given_an_event_when_directionDueDate_is_not_set_then_task_should_start_without_delay() {

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("lastModifiedDirection", null);
        dataMap.put("appealType", "protection");


        final AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        String caseIdForTask = getCaseId();
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(LocalDateTime.now().minusDays(1))
            .caseId(caseIdForTask)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("sendDirection")
            .newStateId(null)
            .previousStateId(null)
            .userId("some user Id")
            .additionalData(additionalData)
            .build();

        callRestEndpoint(s2sToken, eventInformation);

        caseId1Task1Id = findTaskForGivenCaseId(
            caseIdForTask,
            "followUpNonStandardDirection"
        );

        // add tasks to tear down.
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    @Test
    public void given_initiation_task_with_followup_overdue_ctg_when_cancelled_with_noc_event_then_cancel_the_task() {

        String caseId1 = getCaseId();
        String taskIdDmnColumn = "followUpOverdueCaseBuilding";

        // caseId1 with category Followup overdue
        // task1
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "requestCaseBuilding",
            "", "caseBuilding", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        taskIdStatusMap.put(caseId1Task1Id, "deleted");

        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "applyNocDecision",
            "", "", false, "IA", "Asylum");

        assertTaskDoesNotExist(caseId1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_event_requestHearingRequirementsFeature_when_initiated_verify_task_creation() {

        String caseId1 = getCaseId();
        caseId1Task1Id = createTaskWithId(caseId1, "requestHearingRequirementsFeature",
            "", "submitHearingRequirements",
            false, "followUpOverdueHearingRequirements", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

}
