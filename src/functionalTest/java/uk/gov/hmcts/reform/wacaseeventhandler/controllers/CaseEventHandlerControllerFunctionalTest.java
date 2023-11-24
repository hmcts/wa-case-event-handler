package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wacaseeventhandler.MessagingTests;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerControllerFunctionalTest extends MessagingTests {
    private static final Duration AT_MOST_SECONDS = Duration.ofSeconds(120);
    private static final Duration AT_MOST_SECONDS_MULTIPLE_TASKS = Duration.ofSeconds(120);

    protected Map<String, String> taskIdStatusMap;
    protected TestAuthenticationCredentials caseworkerCredentials;
    private LocalDateTime eventTimeStamp;
    @Autowired
    private DueDateService dueDateService;

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

        sendMessageToTopic(randomMessageId(), eventInformation);
    }

    protected void sendMessageWithAdditionalData(String caseId, String event, String previousStateId,
                                                 String newStateId, boolean taskDelay) {

        if (taskDelay) {
            eventTimeStamp = LocalDateTime.now().plusSeconds(2);
        } else {
            eventTimeStamp = LocalDateTime.now().minusDays(1);
        }
        EventInformation eventInformation = getEventInformationWithAdditionalData(
            caseId, event, previousStateId, newStateId, eventTimeStamp
        );

        sendMessageToTopic(randomMessageId(), eventInformation);
    }

    protected void sendMessageWithAdditionalDataForWA(String caseId, String event, String previousStateId,
                                                 String newStateId, boolean taskDelay) {

        if (taskDelay) {
            eventTimeStamp = LocalDateTime.now().plusSeconds(2);
        } else {
            eventTimeStamp = LocalDateTime.now().minusDays(1);
        }
        EventInformation eventInformation = getEventInformationWithAdditionalDataForWA(
                caseId, event, previousStateId, newStateId, eventTimeStamp
        );

        sendMessageToTopic(randomMessageId(), eventInformation);
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
            waitSeconds(8);
        } else {
            waitSeconds(5);
        }

        return findTaskForGivenCaseId(caseId, outcomeTaskId);
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

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.now().minusDays(1);
        caseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2-");

        taskIdStatusMap = new HashMap<>();
    }

    @After
    public void cleanUp() {
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        common.cleanUpTask(caseworkerCredentials.getHeaders(), caseIds);
    }

    @Test
    public void should_succeed_and_create_a_task_with_no_categories() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "dummySubmitAppeal",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        String caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        taskIdStatusMap.put(caseId1Task1Id, "completed");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", equalToIgnoringCase("wacasetype"))
            .body("jurisdiction.value", equalToIgnoringCase("wa"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", equalToIgnoringCase("unconfigured"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseId))
            .body("name.value", equalToIgnoringCase("check fee status"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", equalToIgnoringCase("checkFeeStatus"))
            .body("warningList.value", is("[]"));

    }

    @Test
    public void should_succeed_and_create_a_task_with_single_categories() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "submitAppeal",
            "",
            "appealSubmitted",
            false,
            "WA",
            "WaCaseType"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        String caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", equalToIgnoringCase("wacasetype"))
            .body("idempotencyKey.value", notNullValue())
            .body("jurisdiction.value", equalToIgnoringCase("wa"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", is("unconfigured"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseId))
            .body("name.value", is("Inspect Appeal"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", is("inspectAppeal"))
            .body("caseId.value", is(caseId))
            .body("__processCategory__caseProgression.value", is(true))
            .body("hasWarnings.value", is(false))
            .body("warningList.value", is("[]"));

        completeTask(caseId1Task1Id, "completed");

    }

    @Test
    public void should_succeed_and_create_a_task_with_multiple_categories() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "dummyEventForMultipleCategories",
            "IN_PROGRESS",
            "DONE",
            false, "WA", "WaCaseType"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        String caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("caseTypeId.value", equalToIgnoringCase("wacasetype"))
            .body("idempotencyKey.value", notNullValue())
            .body("jurisdiction.value", equalToIgnoringCase("wa"))
            .body("dueDate.value", notNullValue())
            .body("taskState.value", equalToIgnoringCase("unconfigured"))
            .body("hasWarnings.value", is(false))
            .body("caseId.value", is(caseId))
            .body("name.value", equalToIgnoringCase("Dummy Activity"))
            .body("workingDaysAllowed.value", is(2))
            .body("isDuplicate.value", is(false))
            .body("delayUntil.value", notNullValue())
            .body("taskId.value", equalToIgnoringCase("dummyActivity"))
            .body("caseId.value", is(caseId))
            .body("__processCategory__caseProgression.value", is(true))
            .body("__processCategory__followUpOverdue.value", is(true))
            .body("hasWarnings.value", is(false))
            .body("warningList.value", is("[]"));

        completeTask(caseId1Task1Id, "completed");

    }

    @Test
    public void should_cancel_a_task_with_multiple_categories() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "dummyEventForMultipleCategories",
            "IN_PROGRESS",
            "DONE",
            false, "WA", "WaCaseType"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        String caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        sendMessage(
            caseId,
            "dummyEventForMultipleCategoriesCancel",
            "IN_PROGRESS",
            "DONE",
            false, "WA", "WaCaseType"
        );

        // Assert the task was deleted
        assertTaskDoesNotExist(caseId, "testTaskIdForMultipleCategories");

    }

    @Test
    public void should_warn_a_task_with_multiple_categories() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "dummyEventForMultipleCategories",
            "IN_PROGRESS",
            "DONE",
            false, "WA", "WaCaseType"
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        String caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        sendMessage(
            caseId,
            "makeAnApplication",
            "",
            "",
            false, "WA", "WaCaseType"
        );

        taskFound = findTasksByCaseId(caseId, 2);

        String caseId1Task2Id = taskFound
            .then().assertThat()
            .body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // Assert the task warning was set
        assertTaskHasWarnings(caseId, caseId1Task1Id, true);

        completeTask(caseId1Task1Id, "completed");
        completeTask(caseId1Task2Id, "completed");

    }

    /**
     * This FT sends additionalData to DMN in json format to evaluate appealType.
     * Disabled as the checkFeeStatus task is created with delayUntil to 28 days and can't be
     * retrieved until the task is active.
     * When the DMN is deployed onto new wa jurisdiction, this test can be enabled
     * with delayUntil as 0.
     */
    @Test
    public void given_event_submitAppeal_when_appealType_sent_as_json_then_initiate_task() {

        String caseId = getWaCaseId();

        sendMessageWithAdditionalDataForWA(
            caseId,
            "dummySubmitAppeal",
            "",
            "",
            false
        );

        Response taskFound = findTasksByCaseId(caseId, 1);

        String caseId1Task1Id = taskFound
            .then().assertThat()
            .body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        Response response = findTaskDetailsForGivenTaskId(caseId1Task1Id);

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("taskId.value", is("checkFeeStatus"));

        completeTask(caseId1Task1Id, "completed");

    }

    @Test
    public void given_initiate_tasks_with_time_extension_category_then_cancel_task() {
        // Given multiple existing tasks

        // DST (Day saving time) started on March 29th 2020 at 1:00am
        eventTimeStamp = LocalDateTime.parse("2020-03-27T12:56:10.403975");

        // create task1

        String caseIdForTask1 = getWaCaseId();

        String taskIdDmnColumn = "decideOnTimeExtension";
        String caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "",
            false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        // test for workingDaysAllowed  = 2
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // create task2
        String caseIdForTask2 = getWaCaseId();
        String caseId1Task2Id = createTaskWithId(
            caseIdForTask2,
            "submitTimeExtension",
            "", "", false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        // test for workingDaysAllowed  = 2
        responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task2Id);
        assertDelayDuration(responseTaskDetails);

        // Then cancel the task1
        String eventToCancelTask = "submitReasonsForAppeal";
        String previousStateToCancelTask = "awaitingReasonsForAppeal";
        sendMessage(caseIdForTask1, eventToCancelTask, previousStateToCancelTask,
            "", false, "WA", "WaCaseType"
        );

        // Assert the task1 is deleted
        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");

        completeTask(caseId1Task2Id, "completed");
    }

    @Test
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_task() {
        // Given multiple existing tasks
        // create task1,
        // notice this creates only one task with the follow up category
        String caseIdForTask1 = getWaCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        String caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        // Then cancel the task1
        String eventToCancelTask = "uploadHomeOfficeBundle";
        String previousStateToCancelTask = "awaitingRespondentEvidence";
        sendMessage(caseIdForTask1, eventToCancelTask,
            previousStateToCancelTask, "", false, "WA", "WaCaseType");

        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {

                    // Assert the task1 is deleted
                    assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
                    assertTaskDeleteReason(caseId1Task1Id, "deleted");
                    return true;
                });
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_follow_up_overdue_category_then_cancel_all_tasks() {

        eventTimeStamp = LocalDateTime.parse("2020-02-27T12:56:19.403975");

        // notice this creates one task with the follow up category
        String caseIdForTask1 = getWaCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // task1
        String caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask,
            "", "", false, "WA", "WaCaseType");

        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {
                    assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
                    assertTaskDeleteReason(caseId1Task1Id, "deleted");
                    return true;
                });

    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiate_tasks_with_different_categories_then_cancel_all_tasks() {

        String caseIdForTask1 = getWaCaseId();
        String task1IdDmnColumn = "reviewRespondentEvidence";

        // task1 with category Case progression
        String caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence", false,
            task1IdDmnColumn, "WA", "WaCaseType"
        );

        // task2 with category Time Extension
        String task2IdDmnColumn = "decideOnTimeExtension";
        String caseId1Task2Id = createTaskWithId(
            caseIdForTask1,
            "submitTimeExtension",
            "", "", false,
            task2IdDmnColumn, "WA", "WaCaseType"
        );

        // Then cancel all tasks
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseIdForTask1, eventToCancelTask,
            "", "", false, "WA", "WaCaseType");

        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {
                    assertTaskDoesNotExist(caseIdForTask1, task1IdDmnColumn);
                    assertTaskDoesNotExist(caseIdForTask1, task2IdDmnColumn);

                    assertTaskDeleteReason(caseId1Task1Id, "deleted");
                    assertTaskDeleteReason(caseId1Task2Id, "deleted");
                    return true;
                });

    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_with_followup_overdue_than_cancel_task() {

        String caseIdForTask1 = getWaCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";
        String caseId1Task1Id = createTaskWithId(caseIdForTask1, "requestRespondentEvidence",
            "", "awaitingRespondentEvidence",
            false, taskIdDmnColumn, "WA", "WaCaseType"
        );

        // Then cancel the task1
        sendMessage(caseIdForTask1, "uploadHomeOfficeBundle",
            "awaitingRespondentEvidence", "", false, "WA", "WaCaseType");

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_with_followup_overdue_than_cancel_task() {
        // create task1
        String caseIdForTask1 = getWaCaseId();
        String taskIdDmnColumn = "dummyActivity";
        String caseId1Task1Id = createTaskWithId(caseIdForTask1, "dummyEventForMultipleCategories",
            "", "DONE",
            true, taskIdDmnColumn, "WA", "WaCaseType"
        );

        // Then cancel the task1
        sendMessage(caseIdForTask1, "withdrawAppeal", "", "", false, "WA", "WaCaseType");

        assertTaskDoesNotExist(caseIdForTask1, taskIdDmnColumn);
        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_initiate_tasks_with_follow_up_overdue_category_then_warn_task_with_no_category() {
        // Given multiple existing tasks
        String caseIdForTask1 = getWaCaseId();
        String caseId1Task1Id = createTaskWithId(
            caseIdForTask1,
            "requestCaseBuilding",
            "", "caseBuilding", false,
            "followUpOverdueCaseBuilding", "WA", "WaCaseType"
        );

        sendMessage(caseIdForTask1, "makeAnApplication",
            "", "", false, "WA", "WaCaseType"
        );

        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {
                    Response taskFound = findTasksByCaseId(caseIdForTask1, 2);
                    String caseId1Task2Id = taskFound
                        .then().assertThat()
                        .body("[1].id", notNullValue())
                        .extract()
                        .path("[1].id");
                    taskIdStatusMap.put(caseId1Task2Id, "completed");

                    assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
                    return true;
                });

        completeTask(caseId1Task1Id, "completed");

    }

    @Test
    public void given_caseId_with_multiple_tasks_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getWaCaseId();

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
            "caseUnderReview", false, "WA", "WaCaseType"
        );

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        String caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // initiate task2, category (Case progression)
        sendMessage(caseIdForTask1, "listCma", null,
            "cmaListed", false, "WA", "WaCaseType"
        );

        response = findTasksByCaseId(caseIdForTask1, 2);

        String caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessageWithAdditionalData(
            caseIdForTask1,
            "makeAnApplication",
            "",
            "",
            false
        );
        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {

                    Response result = findTasksByCaseId(
                        caseIdForTask1, 3);

                    final String caseId1Task3Id = result
                        .then()
                        .body("size()", is(3))
                        .assertThat().body("[2].id", notNullValue())
                        .extract()
                        .path("[2].id");
                    taskIdStatusMap.put(caseId1Task3Id, "completed");
                    // check for warnings flag on both the tasks
                    assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
                    assertTaskHasWarnings(caseIdForTask1, caseId1Task2Id, true);
                    return true;
                });

        completeTask(caseId1Task1Id, "completed");
        completeTask(caseId1Task2Id, "completed");

    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_caseId_and_multiple_tasks_and_different_ctg_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getWaCaseId();

        // Initiate task1 , category (caseProgression)
        sendMessage(caseIdForTask1, "submitCase", null,
            "caseUnderReview", false, "WA", "WaCaseType"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String caseId1Task1Id = response
            .then()
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // initiate task2, category (followUpOverdue)
        sendMessage(caseIdForTask1, "requestCaseBuilding", "",
            "caseBuilding", false, "WA", "WaCaseType"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        String caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessageWithAdditionalData(
            caseIdForTask1,
            "makeAnApplication",
            "",
            "",
            false
        );
        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS_MULTIPLE_TASKS)
            .until(
                () -> {

                    Response result = findTasksByCaseId(caseIdForTask1, 3);

                    String caseId1Task3Id = result
                        .then().assertThat()
                        .body("[2].id", notNullValue())
                        .extract()
                        .path("[2].id");

                    // check for warnings flag on both the tasks
                    assertTaskHasWarnings(caseIdForTask1, caseId1Task1Id, true);
                    assertTaskHasWarnings(caseIdForTask1, caseId1Task2Id, true);

                    completeTask(caseId1Task3Id, "completed");
                    return true;
                });

        completeTask(caseId1Task1Id, "completed");
        completeTask(caseId1Task2Id, "completed");
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toFuture_and_without_followup_overdue_then_complete_task() {

        String caseId = getWaCaseId();
        String caseId1Task1Id = createTaskWithId(
            caseId,
            "uploadHomeOfficeBundle",
            "",
            "awaitingRespondentEvidence",
            true,
            "reviewRespondentEvidence", "WA", "WaCaseType"
        );

        // add tasks to tear down.
        completeTask(caseId1Task1Id, "completed");
    }

    @Test
    public void given_initiated_tasks_with_delayTimer_toCurrentTime_and_without_followup_overdue_then_complete_task() {

        String caseId = getWaCaseId();
        String caseId1Task1Id = createTaskWithId(caseId, "listCma",
            "", "cmaListed",
            false, "attendCma", "WA", "WaCaseType"
        );

        // add tasks to tear down.
        completeTask(caseId1Task1Id, "completed");

    }

    @Test
    public void given_multiple_caseIDs_when_action_is_initiate_then_complete_all_tasks() {
        // DST (Day saving time) ended on October 25th 2020 at 2:00am.
        eventTimeStamp = LocalDateTime.parse("2020-10-23T12:56:19.403975");

        String caseIdForTask1 = getWaCaseId();
        String caseId1Task1Id = createTaskWithId(caseIdForTask1, "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence",
            false, "reviewRespondentEvidence", "WA", "WaCaseType"
        );

        // test for workingDaysAllowed  = 2
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);


        String caseIdForTask2 = getWaCaseId();
        String caseId1Task2Id = createTaskWithId(caseIdForTask2, "submitAppeal",
            "", "appealSubmitted",
            false, "inspectAppeal", "WA", "WaCaseType"
        );

        completeTask(caseId1Task1Id, "completed");
        completeTask(caseId1Task2Id, "completed");

    }

    @Test
    public void given_multiple_caseIDs_when_action_is_cancel_then_cancels_all_tasks() {

        String caseId1 = getWaCaseId();
        String taskIdDmnColumn = "followUpOverdueRespondentEvidence";

        // caseId1 with category Followup overdue
        // task1
        final String caseId1Task1Id = createTaskWithId(
            caseId1,
            "requestRespondentEvidence",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        // caseId2 with category Case progression
        String caseId2 = getWaCaseId();
        String taskId2DmnColumn = "reviewAppealSkeletonArgument";
        final String caseId2Task1Id = createTaskWithId(caseId2, "submitCase",
            "", "caseUnderReview",
            false, taskId2DmnColumn, "WA", "WaCaseType"
        );

        // Then cancel all tasks on both caseIDs
        String eventToCancelTask = "removeAppealFromOnline";
        sendMessage(caseId1, eventToCancelTask,
            "", "", false, "WA", "WaCaseType");
        waitSeconds(5);
        sendMessage(caseId2, eventToCancelTask,
            "", "", false, "WA", "WaCaseType");
        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {

                    assertTaskDoesNotExist(caseId1, taskIdDmnColumn);
                    assertTaskDoesNotExist(caseId2, taskId2DmnColumn);

                    assertTaskDeleteReason(caseId1Task1Id, "deleted");
                    assertTaskDeleteReason(caseId2Task1Id, "deleted");
                    return true;
                });

    }

    @Test
    public void given_multiple_caseIDs_when_actions_is_warn_then_mark_all_tasks_with_warnings() {
        //caseId1 with category Case progression
        String caseId1 = getWaCaseId();
        String taskIdDmnColumn = "reviewRespondentEvidence";
        final String caseId1Task1Id = createTaskWithId(
            caseId1,
            "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence", false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        //caseId1 with category Case progression
        String caseId2 = getWaCaseId();
        String taskId2DmnColumn = "reviewRespondentEvidence";
        final String caseId2Task1Id = createTaskWithId(caseId2, "uploadHomeOfficeBundle",
            "", "awaitingRespondentEvidence",
            false, taskId2DmnColumn, "WA", "WaCaseType"
        );

        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "makeAnApplication",
            "", "", false, "WA", "WaCaseType"
        );
        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {
                    Response taskFound = findTasksByCaseId(caseId1, 2);

                    String caseId1Task2Id = taskFound
                        .then().assertThat()
                        .body("[1].id", notNullValue())
                        .extract()
                        .path("[1].id");
                    taskIdStatusMap.put(caseId1Task2Id, "completed");
                    return true;
                });


        sendMessage(caseId2, "makeAnApplication",
            "", "", false, "WA", "WaCaseType"
        );
        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {


                    Response taskFoundInDb = findTasksByCaseId(caseId2, 2);

                    String caseId2Task2Id = taskFoundInDb
                        .then().assertThat()
                        .body("[1].id", notNullValue())
                        .extract()
                        .path("[1].id");

                    // check for warnings flag on both the tasks
                    assertTaskHasWarnings(caseId1, caseId1Task1Id, true);
                    assertTaskHasWarnings(caseId2, caseId2Task1Id, true);

                    completeTask(caseId2Task2Id, "completed");
                    return true;
                });

        completeTask(caseId1Task1Id, "completed");
        completeTask(caseId2Task1Id, "completed");

    }

    @Test
    public void given_an_event_when_directionDueDate_is_empty_then_task_should_start_without_delay() {


        String caseIdForTask = getWaCaseId();

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
            .jurisdictionId("WA")
            .caseTypeId("WaCaseType")
            .eventId("requestCaseBuilding")
            .newStateId("caseBuilding")
            .previousStateId(null)
            .userId("some user Id")
            .additionalData(additionalData)
            .build();

        callRestEndpoint(s2sToken, eventInformation);

        String caseId1Task1Id = findTaskForGivenCaseId(
            caseIdForTask,
            "followUpOverdueCaseBuilding"
        );

        // add tasks to tear down.
        completeTask(caseId1Task1Id, "completed");
    }

    @Test
    public void given_an_event_when_directionDueDate_is_not_set_then_task_should_start_without_delay() {

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("lastModifiedDirection", null);
        dataMap.put("appealType", "protection");


        final AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        String caseIdForTask = getWaCaseId();
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(LocalDateTime.now().minusDays(1))
            .caseId(caseIdForTask)
            .jurisdictionId("WA")
            .caseTypeId("WaCaseType")
            .eventId("requestRespondentEvidence")
            .newStateId("awaitingRespondentEvidence")
            .previousStateId(null)
            .userId("some user Id")
            .additionalData(additionalData)
            .build();

        callRestEndpoint(s2sToken, eventInformation);

        String caseId1Task1Id = findTaskForGivenCaseId(
            caseIdForTask,
            "followUpOverdueRespondentEvidence"
        );

        // add tasks to tear down.
        completeTask(caseId1Task1Id, "completed");
    }

    @Test
    public void given_initiation_task_with_followup_overdue_ctg_when_cancelled_with_noc_event_then_cancel_the_task() {

        String caseId1 = getWaCaseId();
        String taskIdDmnColumn = "followUpOverdueCaseBuilding";

        // caseId1 with category Followup overdue
        // task1
        String caseId1Task1Id = createTaskWithId(
            caseId1,
            "requestCaseBuilding",
            "", "caseBuilding", false,
            taskIdDmnColumn, "WA", "WaCaseType"
        );

        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "applyNocDecision",
            "", "", false, "WA", "WaCaseType");

        assertTaskDoesNotExist(caseId1, taskIdDmnColumn);

        assertTaskDeleteReason(caseId1Task1Id, "deleted");

    }

    @Test
    public void given_initiate_tasks_then_reconfigure_task_to_mark_tasks_for_reconfiguration_for_WA() {

        TestVariables taskVariables = common.setupWaTaskAndRetrieveIds();
        String caseId1Task1Id = taskVariables.getTaskId();

        String caseIdForTask1 = taskVariables.getCaseId();
        common.setupCftOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        //initiate task
        initiateTask(caseworkerCredentials.getHeaders(), caseIdForTask1, caseId1Task1Id,
            "followUpOverdueCaseBuilding", "Follow-up overdue case building", "Follow-up overdue case building");

        //get task from CFT
        Response result = restApiActions.get(
            TASK_ENDPOINT,
            caseId1Task1Id,
            caseworkerCredentials.getHeaders()
        );
        //assert reconfigure request time
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(caseId1Task1Id))
            .body("task.reconfigure_request_time", nullValue());

        //send update event to trigger reconfigure action
        String jurisdiction = "WA";
        String caseType = "WaCaseType";
        sendMessage(caseIdForTask1, "UPDATE",
            "", "", false, jurisdiction, caseType
        );

        await().ignoreException(AssertionError.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS)
            .until(
                () -> {
                    //assert task in camunda
                    findTasksByCaseId(caseIdForTask1, 1);
                    return true;
                });
        await().ignoreException(Exception.class)
            .pollDelay(2, SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(AT_MOST_SECONDS_MULTIPLE_TASKS)
            .until(
                () -> {
                    //get task from CFT
                    Response response = restApiActions.get(
                        TASK_ENDPOINT,
                        caseId1Task1Id,
                        caseworkerCredentials.getHeaders()
                    );
                    //assert reconfigure request time
                    response.then().assertThat()
                        .statusCode(HttpStatus.OK.value());

                    String taskId = response.jsonPath().get("task.id");
                    String reconfigureRequestTime = response.jsonPath().get("task.reconfigure_request_time");
                    String lastReconfigurationTime = response.jsonPath().get("task.last_reconfiguration_time");
                    log.info("Task ID {}, reconfigureRequestTime {}, lastReconfigurationTime {}", taskId,
                             reconfigureRequestTime, lastReconfigurationTime);

                    assertEquals(caseId1Task1Id, taskId);
                    assertTrue(reconfigureRequestTime != null || lastReconfigurationTime != null);
                    //cleanup

                    completeTask(caseId1Task1Id, "completed");

                    return true;
                });
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders(), "WA");
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
            .pollDelay(500, MILLISECONDS)
            .pollInterval(2, SECONDS)
            .atMost(AT_MOST_SECONDS_MULTIPLE_TASKS)
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
            .pollDelay(500, MILLISECONDS)
            .pollInterval(2, SECONDS)
            .atMost(AT_MOST_SECONDS_MULTIPLE_TASKS)
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

    private EventInformation getEventInformation(String caseId,
                                                 String event,
                                                 String previousStateId,
                                                 String newStateId,
                                                 LocalDateTime localDateTime,
                                                 String jurisdictionId,
                                                 String caseTypeId) {
        String appealType = event.equals("submitAppeal") ? "deprivation" : "";

        return EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(localDateTime)
            .caseId(caseId)
            .jurisdictionId(jurisdictionId)
            .caseTypeId(caseTypeId)
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .additionalData(setAdditionalData(appealType, "Adjourn"))
            .userId("some user Id")
            .build();
    }

    private EventInformation getEventInformationWithAdditionalDataForWA(
            String caseId, String event, String previousStateId, String newStateId, LocalDateTime localDateTime) {
        return EventInformation.builder()
                .eventInstanceId(UUID.randomUUID().toString())
                .eventTimeStamp(localDateTime)
                .caseId(caseId)
                .jurisdictionId("WA")
                .caseTypeId("WaCaseType")
                .eventId(event)
                .newStateId(newStateId)
                .previousStateId(previousStateId)
                .additionalData(setAdditionalData("", "Adjourn"))
                .userId("some user Id")
                .build();
    }

    private EventInformation getEventInformationWithAdditionalData(String caseId, String event, String previousStateId,
                                                                   String newStateId, LocalDateTime localDateTime) {
        return EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(localDateTime)
            .caseId(caseId)
            .jurisdictionId("WA")
            .caseTypeId("WaCaseType")
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .additionalData(setAdditionalData("", "Adjourn"))
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

    private String findTaskForGivenCaseId(String caseId, String taskIdDmnColumn) {

        log.info("Attempting to retrieve task with caseId = {} and taskId = {}", caseId, taskIdDmnColumn);
        String filter = "?processVariables=caseId_eq_" + caseId + ",taskId_eq_" + taskIdDmnColumn;

        AtomicReference<String> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollDelay(500, MILLISECONDS)
            .pollInterval(2, SECONDS)
            .atMost(AT_MOST_SECONDS_MULTIPLE_TASKS)
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

    private AdditionalData setAdditionalData(String appealType, String lastModifiedApplicationType) {
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of(
                "dateDue", "",
                "uniqueId", "",
                "directionType", ""
            ),
            "appealType", appealType,
            "lastModifiedApplication", Map.of("type", lastModifiedApplicationType,
                                              "decision", "")

        );

        return AdditionalData.builder()
            .data(dataMap)
            .build();
    }

}
