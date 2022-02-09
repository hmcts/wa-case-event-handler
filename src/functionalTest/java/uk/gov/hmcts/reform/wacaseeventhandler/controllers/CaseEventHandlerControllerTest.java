package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.azure.messaging.servicebus.ServiceBusMessage;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    private LocalDateTime eventTimeStamp;
    protected List<String> caseIds;
    protected List<String> taskIds;
    protected String taskCompletionStatus;
    protected TestAuthenticationCredentials caseworkerCredentials;


    private String caseId1Task1Id;
    private String caseId1Task2Id;
    private String caseId2Task1Id;
    private String caseId2Task2Id;


    @Autowired
    private DueDateService dueDateService;

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.now().minusDays(1);
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        caseIds = new ArrayList<>();
        taskIds = new ArrayList<>();
        caseId1Task1Id = "";
        caseId1Task2Id = "";
        caseId2Task1Id = "";
        caseId2Task2Id = "";
    }

    @After
    public void cleanUp() {
        taskIds.forEach(task -> completeTask(task, taskCompletionStatus));
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        common.cleanUpTask(caseworkerCredentials.getHeaders(), caseIds);
    }

    @Test
    public void should_succeed_and_create_a_task_with_no_categories() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();

        sendMessage(
            caseId,
            "makeAnApplication",
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
            .body("name.value", is("Process Application"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", is("processApplication"))
            .body("warningList.value", is("[]"));

        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    public void should_succeed_and_create_a_task_with_single_categories() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();

        sendMessage(
            caseId,
            "submitAppeal",
            "",
            "appealSubmitted",
            false, "IA", "Asylum"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

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

        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    @Ignore("non-existing requirement for IA")
    public void should_succeed_and_create_a_task_with_multiple_categories() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();

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

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", is("asylum"))
            .body("idempotencyKey.value", notNullValue())
            .body("jurisdiction.value", is("ia"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", is("unconfigured"))
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

        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    @Ignore("non-existing requirement for IA")
    public void should_cancel_a_task_with_multiple_categories() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();

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

        sendMessage(
            caseId,
            "dummyEventForMultipleCategoriesCancel",
            "",
            "",
            false, "IA", "Asylum"
        );

        // Assert the task was deleted
        assertTaskDoesNotExist(caseId, "testTaskIdForMultipleCategories");
        assertTaskDeleteReason(caseId1Task1Id, "deleted");
    }

    @Test
    @Ignore("non-existing requirement for IA")
    public void should_warn_a_task_with_multiple_categories() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();

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

        sendMessage(
            caseId,
            "dummyEventForMultipleCategoriesWarn",
            "",
            "",
            false, "IA", "Asylum"
        );

        // Assert the task warning was set
        assertTaskHasWarnings(caseId, caseId1Task1Id, true);

        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);

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
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();

        sendMessageWithAdditionalData(
            caseId,
            "submitAppeal",
            "",
            "appealSubmitted",
            false
        );

        Response taskFound = findTasksByCaseId(caseId, 2);

        caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

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

        response = findTaskDetailsForGivenTaskId(caseId1Task2Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("taskId.value", is("reviewTheAppeal"));

        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id);
    }

    @Test
    public void given_initiate_tasks_with_time_extension_category_then_cancel_task() {
        // Given multiple existing tasks

        // DST (Day saving time) started on March 29th 2020 at 1:00am
        eventTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975");

        // create task1
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();

        String taskIdDmnColumn = "decideOnTimeExtension";
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "",
            false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        // test for workingDaysAllowed  = 2
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // create task2
        taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask2 = taskVariables.getCaseId();
        caseId1Task2Id = createTaskWithId(
            caseIdForTask2,
            "submitTimeExtension",
            "", "", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

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

        // tear down task2
        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id);
    }

    @Test
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_task() {
        // Given multiple existing tasks

        // create task1,
        // notice this creates only one task with the follow up category
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        // Then cancel the task1
        String eventToCancelTask = "uploadHomeOfficeBundle";
        String previousStateToCancelTask = "awaitingRespondentEvidence";
        sendMessage(caseIdForTask1, eventToCancelTask,
            previousStateToCancelTask, "", false, "IA", "Asylum");

        // Assert the task1 is deleted
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");
        taskCompletionStatus = "deleted";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_all_tasks() {

        eventTimeStamp = LocalDateTime.parse("2020-02-27T12:56:19.403975");

        // notice this creates one task with the follow up category
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // task1
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask,
            "", "", false, "IA", "Asylum");

        waitSeconds(5);
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");

        // add tasks to tear down.
        taskCompletionStatus = "deleted";
        taskIds = List.of(caseId1Task1Id);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_different_categories_then_cancel_all_tasks() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        String task1IdDmnColumn = "reviewTheAppeal";

        // task1 with category Case progression
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "submitAppeal",
            "", "appealSubmitted", false,
            task1IdDmnColumn, "IA", "Asylum"
        );

        // task2 with category Time Extension
        String task2IdDmnColumn = "decideOnTimeExtension";
        caseId1Task2Id = createTaskWithId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "", false,
            task2IdDmnColumn, "IA", "Asylum"
        );

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask,
            "", "", false, "IA", "Asylum");

        waitSeconds(5);
        assertTaskDoesNotExist(caseIdForTask1, task1IdDmnColumn);
        assertTaskDoesNotExist(caseIdForTask1, task2IdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");
        assertTaskDeleteReason(caseId1Task2Id, "deleted");

        // add tasks to tear down.
        taskCompletionStatus = "deleted";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id);
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_with_followup_overdue_than_cancel_task() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        caseId1Task1Id = createTaskWithId(caseIdForTask1, "requestRespondentEvidence",
            "", "awaitingRespondentEvidence",
            false, taskIdDmnColumn, "IA", "Asylum"
        );

        // Then cancel the task1
        sendMessage(caseIdForTask1, "uploadHomeOfficeBundle",
            "awaitingRespondentEvidence", "", false, "IA", "Asylum");

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_with_followup_overdue_than_cancel_task() {
        // create task1
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "followUpOverdueCaseBuilding";
        caseId1Task1Id = createTaskWithId(caseIdForTask1, "requestCaseBuilding",
            "", "caseBuilding",
            true, taskIdDmnColumn, "IA", "Asylum"
        );

        // Then cancel the task1
        sendMessage(caseIdForTask1, "buildCase", "caseBuilding", "", false, "IA", "Asylum");

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");
    }

    @Test
    public void given_initiate_tasks_with_follow_up_overdue_category_then_warn_task_with_no_category() {
        // Given multiple existing tasks
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestCaseBuilding",
            "", "caseBuilding", false,
            "followUpOverdueCaseBuilding", "IA", "Asylum"
        );

        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );

        waitSeconds(5);

        Response taskFound = findTasksByCaseId(caseIdForTask1, 2);

        String taskId2 = taskFound
            .then().assertThat()
            .body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);

        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, taskId2);
    }

    @Test
    public void given_caseId_with_multiple_tasks_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
            "caseUnderReview", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // initiate task2, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
            "caseUnderReview", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(caseIdForTask1, 2);

        final String caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 3);

        final String caseId1Task3Id = response
            .then()
            .body("size()", is(3))
            .assertThat().body("[2].id", notNullValue())
            .extract()
            .path("[2].id");
        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
        assertTaskHasWarnings(caseIdForTask1, caseId1Task2Id, true);

        // tear down all tasks
        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id, caseId1Task3Id);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_caseId_and_multiple_tasks_and_different_ctg_when_warning_raised_then_mark_tasks_with_warnings() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();

        // Initiate task1 , category (Time extension)
        sendMessage(caseIdForTask1, "submitTimeExtension", "",
            null, false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // initiate task2, category (Case progression)
        sendMessage(caseIdForTask1, "requestCaseBuilding", null,
            "caseBuilding", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

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
        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
        assertTaskHasWarnings(caseIdForTask1, caseId1Task2Id, true);

        // tear down all tasks
        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id, caseId1Task3Id);
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_and_without_followup_overdue_then_complete_task() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();
        caseId1Task1Id = createTaskWithId(
            caseId,
            "makeAnApplication",
            "",
            "",
            true,
            "processApplication", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_and_without_followup_overdue_then_complete_task() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId = taskVariables.getCaseId();
        caseId1Task1Id = createTaskWithId(caseId, "submitAppeal",
            "", "appealSubmitted",
            false, "reviewTheAppeal", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    public void given_multiple_caseIDs_when_action_is_initiate_then_complete_all_tasks() {
        // DST (Day saving time) ended on October 25th 2020 at 2:00am.
        eventTimeStamp = LocalDateTime.parse("2020-10-23T12:56:19.403975");

        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask1 = taskVariables.getCaseId();
        caseId1Task1Id = createTaskWithId(caseIdForTask1, "submitAppeal",
            "", "appealSubmitted",
            false, "reviewTheAppeal", "IA", "Asylum"
        );

        // test for workingDaysAllowed  = 2
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask2 = taskVariables.getCaseId();
        caseId1Task2Id = createTaskWithId(caseIdForTask2, "submitAppeal",
            "", "appealSubmitted",
            false, "reviewTheAppeal", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id);
    }

    @Test
    public void given_multiple_caseIDs_when_action_is_cancel_then_cancels_all_tasks() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // caseId1 with category Followup overdue
        // task1
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        // caseId2 with category Case progression
        taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId2 = taskVariables.getCaseId();
        String taskId2DmnColumn = "reviewAppealSkeletonArgument";
        caseId2Task1Id = createTaskWithId(caseId2, "submitCase",
            "", "caseUnderReview",
            false, taskId2DmnColumn, "IA", "Asylum"
        );
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

        // add tasks to tear down.
        taskCompletionStatus = "deleted";
        taskIds = List.of(caseId1Task1Id, caseId2Task1Id);
    }

    @Test
    public void given_multiple_caseIDs_when_actions_is_warn_then_mark_all_tasks_with_warnings() {
        //caseId1 with category Case progression
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "attendCma";
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "listCma",
            "", "cmaListed", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        //caseId1 with category Case progression
        taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId2 = taskVariables.getCaseId();
        String taskId2DmnColumn = "reviewRespondentResponse";
        caseId2Task1Id = createTaskWithId(caseId2, "uploadHomeOfficeAppealResponse",
            "", "respondentReview",
            false, taskId2DmnColumn, "IA", "Asylum"
        );
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
        // check for warnings flag on both the tasks
        assertTaskHasWarnings(caseId1, caseId1Task1Id, true);
        assertTaskHasWarnings(caseId2, caseId2Task1Id, true);

        // tear down all tasks
        taskCompletionStatus = "completed";
        taskIds = List.of(caseId1Task1Id, caseId1Task2Id, caseId2Task1Id, caseId2Task2Id);
    }

    @Test
    public void given_an_event_when_directionDueDate_is_empty_then_task_should_start_without_delay() {

        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask = taskVariables.getCaseId();

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
        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    public void given_an_event_when_directionDueDate_is_not_set_then_task_should_start_without_delay() {

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("lastModifiedDirection", null);
        dataMap.put("appealType", "protection");


        final AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseIdForTask = taskVariables.getCaseId();
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
        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
    }

    @Test
    public void given_initiation_task_with_followup_overdue_ctg_when_cancelled_with_noc_event_then_cancel_the_task() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId1 = taskVariables.getCaseId();
        String taskIdDmnColumn = "followUpOverdueCaseBuilding";

        // caseId1 with category Followup overdue
        // task1
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "requestCaseBuilding",
            "", "caseBuilding", false,
            taskIdDmnColumn, "IA", "Asylum"
        );

        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "applyNocDecision",
            "", "", false, "IA", "Asylum");

        assertTaskDoesNotExist(caseId1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");
    }

    @Test
    public void given_event_requestHearingRequirementsFeature_when_initiated_verify_task_creation() {
        TestVariables taskVariables = common.createCase();
        assertNotNull(taskVariables);
        assertNotNull(taskVariables.getCaseId());
        caseIds.add(taskVariables.getCaseId());
        String caseId1 = taskVariables.getCaseId();
        caseId1Task1Id = createTaskWithId(caseId1, "requestHearingRequirementsFeature",
            "", "submitHearingRequirements",
            false, "followUpOverdueHearingRequirements", "IA", "Asylum"
        );

        // add tasks to tear down.
        taskCompletionStatus = "completed";
        taskIds.add(caseId1Task1Id);
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

    private void assertTaskDoesNotExist(String caseId, String taskId) {
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

    private void assertTaskHasWarnings(String caseId, String taskId, boolean hasWarningValue) {
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
                                                 String newStateId, boolean taskDelay) {

        if (taskDelay) {
            eventTimeStamp = LocalDateTime.now().plusSeconds(2);
        }
        EventInformation eventInformation = getEventInformationWithAdditionalData(
            caseId, event, previousStateId, newStateId, eventTimeStamp
        );

        if (publisher != null) {
            publishMessageToTopic(eventInformation);
        } else {
            callRestEndpoint(s2sToken, eventInformation);
        }
    }

    private EventInformation getEventInformation(String caseId,
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
            .userId("some user Id")
            .build();
    }

    private EventInformation getEventInformationWithAdditionalData(String caseId, String event, String previousStateId,
                                                                   String newStateId, LocalDateTime localDateTime) {
        return EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(localDateTime)
            .caseId(caseId)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .additionalData(setAdditionalData())
            .userId("some user Id")
            .build();
    }

    private void callRestEndpoint(String s2sToken, EventInformation eventInformation) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
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

        assertTaskDeleteReason(taskId, status);
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
