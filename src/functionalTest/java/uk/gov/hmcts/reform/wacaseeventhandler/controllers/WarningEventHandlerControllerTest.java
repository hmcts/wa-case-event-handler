package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;

import java.util.Arrays;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Functional tests for non-IAC requirements. These tests are executed against dummy events
 * defined in ia-task-configuration in src/main/resources/nonprod folder.
 *
 */
@Slf4j
public class WarningEventHandlerControllerTest extends CaseEventHandlerControllerTest {

    /**
     * Scenario: 1 event creates 2 different warnings on all tasks.
     */
    @Test
    public void given_caseId_with_multiple_tasks_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1
        sendMessage(
            caseIdForTask1,
            "submitCase",
            null,
            "caseUnderReview",
            false,
            "WA",
            "WaCaseType"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        Response responseTaskDetails = findTaskDetailsForGivenTaskId(task1Id);
        assertDelayDuration(responseTaskDetails);

        // initiate task2 with same caseId
        sendMessage(
            caseIdForTask1,
            "submitCase",
            null,
            "caseUnderReview",
            false,
            "WA",
            "WaCaseType"
        );

        response = findTasksByCaseId(caseIdForTask1, 2);

        final String task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        String warningsAsJson = "[{\"warningCode\":\"TA01\","
            + "\"warningText\":\"There is an application task which "
            + "might impact other active tasks\"},"
            + "{\"warningCode\":\"TA02\","
            + "\"warningText\":\"There is another task on this case that needs your attention\"}"
            + "]";
        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, task1Id, new WarningValues(warningsAsJson));
        assertTaskHasMultipleWarnings(caseIdForTask1, task2Id, new WarningValues(warningsAsJson));

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "completed");
    }

    /**
     * Scenario: 1 event creates 2 different warnings on single task.
     */
    @Test
    @Ignore
    public void given_caseId_with_single_task_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
                    "caseUnderReview", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(task1Id);
        assertDelayDuration(responseTaskDetails);

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication",
                    "", "", false, "IA", "Asylum"
        );

        String warningsAsJson = "[{\"warningCode\":\"TA01\","
            + "\"warningText\":\"There is an application task which "
            + "might impact other active tasks\"},"
            + "{\"warningCode\":\"TA02\","
            + "\"warningText\":\"There is another task on this case that needs your attention\"}"
            + "]";
        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, task1Id, new WarningValues(warningsAsJson));

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id), "completed");
    }


    /**
     * Scenario: 1 event creates 2 different warnings each for a different task type.
     */
    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Ignore
    public void given_caseId_and_multiple_tasks_and_different_ctg_when_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1 , category (timeExtension)
        sendMessage(caseIdForTask1, "submitTimeExtension", "",
                    null, false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // initiate task2, category (followUpOverdue)
        sendMessage(caseIdForTask1, "requestCaseBuilding", null,
                    "caseBuilding", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        String task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // initiate task3, category (caseProgression)
        sendMessage(caseIdForTask1, "listCma", null,
                    "cmaListed", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 3);

        String task3Id = response
            .then()
            .body("size()", is(3))
            .assertThat().body("[2].id", notNullValue())
            .extract()
            .path("[2].id");

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication101",
                    "", "", false, "IA", "Asylum"
        );

        waitSeconds(5);

        String timeExtensionWarning = "[{\"warningCode\":\"Code101\","
            + "\"warningText\":\"Warning Text 101\"}]";
        WarningValues timeExtensionWarningValues = new WarningValues(timeExtensionWarning);

        String followUpOverdueWarning = "[{\"warningCode\":\"Code102\","
            + "\"warningText\":\"Warning Text 102\"}]";
        WarningValues overdueWarningValues = new WarningValues(followUpOverdueWarning);

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, task1Id, timeExtensionWarningValues);
        assertTaskHasMultipleWarnings(caseIdForTask1, task2Id, overdueWarningValues);

        // task3Id should not contain any warnings because the category is not correlated
        assertTaskWithoutWarnings(caseIdForTask1, task3Id, false);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id, task3Id), "completed");
    }

    /**
     * Scenario: 1 event on 2 different caseIds creates 2 different warnings each for all task types.
     */
    @Test
    @Ignore
    public void given_multiple_caseIDs_when_actions_is_warn_then_mark_all_tasks_with_warnings() {
        //caseId1 with category Case progression
        String caseId1 = UUID.randomUUID().toString();
        String taskIdDmnColumn = "attendCma";
        final String caseId1Task1Id = createTaskWithId(
            caseId1,
            "listCma",
            "", "cmaListed", false,
            taskIdDmnColumn
        );

        //caseId1 with category Case progression
        String taskId2DmnColumn = "reviewRespondentResponse";
        String caseId2 = UUID.randomUUID().toString();
        final String caseId2Task1Id = createTaskWithId(caseId2, "uploadHomeOfficeAppealResponse",
                                                       "", "respondentReview",
                                                       false, taskId2DmnColumn);
        // Then cancel all tasks on both caseIDs
        sendMessage(caseId1, "_DUMMY_makeAnApplication",
                    "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);
        sendMessage(caseId2, "_DUMMY_makeAnApplication",
                    "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);

        String warningsAsJson = "[{\"warningCode\":\"TA01\","
            + "\"warningText\":\"There is an application task which "
            + "might impact other active tasks\"},"
            + "{\"warningCode\":\"TA02\","
            + "\"warningText\":\"There is another task on this case that needs your attention\"}"
            + "]";
        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseId1, caseId1Task1Id, new WarningValues(warningsAsJson));
        assertTaskHasMultipleWarnings(caseId2, caseId2Task1Id, new WarningValues(warningsAsJson));

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(caseId1Task1Id, caseId2Task1Id), "completed");
    }

    /**
     * Scenario: 1 event creates the same warning on all tasks twice?
     * (should only be able to add the same warning once - if warning code is already on task, then do not add).
     */
    @Test
    @Ignore
    public void given_caseID_when_action_is_warn_with_same_warnings_then_add_the_warning_only_once() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        sendMessage(caseIdForTask1, "submitCase", null,
                    "caseUnderReview", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication103",
                    "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);

        String singleWarning = "[{\"warningCode\":\"Code103\","
            + "\"warningText\":\"Warning Text 103\"}]";
        WarningValues warningValues = new WarningValues(singleWarning);

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, task1Id, warningValues);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id), "completed");
    }

    /**
     * Scenario: 1 event creates the same warning on tasks of a different category.
     */
    @Test
    @Ignore
    public void given_caseId_with_different_category_when_same_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1, category (timeExtension)
        sendMessage(caseIdForTask1, "submitTimeExtension", null,
                    "", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(task1Id);
        assertDelayDuration(responseTaskDetails);

        // initiate task2, category (followUpOverdue)
        sendMessage(caseIdForTask1, "requestCaseBuilding", null,
                    "caseBuilding", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        final String task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication104",
                    "", "", false, "IA", "Asylum"
        );

        String singleWarning = "[{\"warningCode\":\"Code104\","
            + "\"warningText\":\"Warning Text 104\"}]";

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, task1Id, new WarningValues(singleWarning));
        assertTaskHasMultipleWarnings(caseIdForTask1, task2Id, new WarningValues(singleWarning));

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "completed");
    }

    /**
     * Scenario: 1 event creates warning with no ID or description on all tasks.
     */
    @Test
    @Ignore
    public void given_caseId_when_warning_raised_without_warning_attributes_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
                    "caseUnderReview", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(task1Id);
        assertDelayDuration(responseTaskDetails);

        // initiate task2, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
                    "caseUnderReview", false, "IA", "Asylum"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        final String task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication102",
                    "", "", false, "IA", "Asylum"
        );

        // check for warnings flag on both the tasks
        assertTaskWithoutWarnings(caseIdForTask1, task1Id, true);
        assertTaskWithoutWarnings(caseIdForTask1, task2Id, true);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id, task2Id), "completed");
    }

    /**
     * Scenario: 1 event creates warning with no ID or description on tasks of a single category.
     */
    @Test
    @Ignore
    public void given_caseId_with_category_when_warning_raised_without_warnings_then_mark_tasks_with_warning() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1, category (Case progression)
        sendMessage(caseIdForTask1, "submitCase", null,
                    "caseUnderReview", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed  = 5
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(task1Id);
        assertDelayDuration(responseTaskDetails);

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication102",
                    "", "", false, "IA", "Asylum"
        );

        // check for warnings flag on both the tasks
        assertTaskWithoutWarnings(caseIdForTask1, task1Id, true);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id), "completed");
    }

    /**
     * Scenario: 2 events creates same warnings on tasks of a single category.
     */
    @Test
    @Ignore
    public void given_caseId_with_category_and_same_warnings_when_warnings_raised_then_mark_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1, category (timeExtension)
        sendMessage(caseIdForTask1, "submitTimeExtension", null,
                    "", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        final String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication105",
                    "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication106",
                    "", "", false, "IA", "Asylum"
        );
        waitSeconds(5);

        String singleWarning = "[{\"warningCode\":\"Code105\","
            + "\"warningText\":\"Warning Text 105\"}]";

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, task1Id, new WarningValues(singleWarning));

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id), "completed");
    }

    /**
     * Scenario: Single event with category and without warnings.
     */
    @Test
    @Ignore
    public void given_caseId_with_without_warnings_when_warning_raised_then_mark_tasks_with_warnings() {
        String caseIdForTask1 = UUID.randomUUID().toString();

        // Initiate task1, category (followUpOverdue)
        sendMessage(caseIdForTask1, "requestRespondentEvidence", null,
                    "awaitingRespondentEvidence", false, "IA", "Asylum"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        String task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // send warning message
        sendMessage(caseIdForTask1, "_DUMMY_makeAnApplication107",
                    "", "", false, "IA", "Asylum"
        );

        // check for warnings flag on both the tasks
        assertTaskWithoutWarnings(caseIdForTask1, task1Id, true);

        // tear down all tasks
        tearDownMultipleTasks(Arrays.asList(task1Id), "completed");
    }

    public void assertTaskHasMultipleWarnings(String caseId, String taskId,
                                              WarningValues expectedWarningValues) {
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
                        .body("hasWarnings.value", is(true));

                    final String warningList = result.jsonPath().getString("warningList.value");
                    WarningValues actualWarningValues = new WarningValues(warningList);

                    assertEquals(expectedWarningValues, actualWarningValues);

                    return true;
                });
    }

    private void assertTaskWithoutWarnings(String caseId, String taskId, boolean hasWarnings) {
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
                        .body("hasWarnings.value", is(hasWarnings))
                        .body("warningList.value", is("[]"));

                    return true;
                });
    }
}
