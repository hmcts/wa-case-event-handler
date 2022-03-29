package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class WarningEventHandlerControllerTest extends CaseEventHandlerControllerTest {

    /**
     * Scenario: 1 event creates 2 different warnings on all tasks.
     */
    @Test
    public void given_caseId_with_multiple_tasks_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();
        // Initiate task1
        sendMessage(
            caseIdForTask1,
            "uploadHomeOfficeBundle",
            null,
            "awaitingRespondentEvidence",
            false,
            "WA",
            "WaCaseType"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
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

        caseId1Task2Id = response
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
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task1Id, new WarningValues(warningsAsJson));
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task2Id, new WarningValues(warningsAsJson));

        taskIdStatusMap.put(caseId1Task1Id, "completed");
        taskIdStatusMap.put(caseId1Task2Id, "completed");
    }

    /**
     * Scenario: 1 event creates 2 different warnings on single task.
     */
    @Test
    public void given_caseId_with_single_task_and_same_category_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

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

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // test for workingDaysAllowed
        Response responseTaskDetails = findTaskDetailsForGivenTaskId(caseId1Task1Id);
        assertDelayDuration(responseTaskDetails);

        // send warning message
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication",
            "",
            "",
            false,
            "Wa",
            "WaCaseType"
        );

        String warningsAsJson = "[{\"warningCode\":\"TA01\","
                                + "\"warningText\":\"There is an application task which "
                                + "might impact other active tasks\"},"
                                + "{\"warningCode\":\"TA02\","
                                + "\"warningText\":\"There is another task on this case that needs your attention\"}"
                                + "]";
        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task1Id, new WarningValues(warningsAsJson));

        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }


    /**
     * Scenario: 1 event creates 2 different warnings each for a different task type.
     */
    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_caseId_and_multiple_tasks_and_different_ctg_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1 , category (timeExtension)
        sendMessage(
            caseIdForTask1,
            "submitTimeExtension",
            "",
            null,
            false,
            "WA",
            "WaCaseType"
        );

        Response response = findTasksByCaseId(
            caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // initiate task2, category (followUpOverdue)
        sendMessage(
            caseIdForTask1,
            "requestCaseBuilding",
            null,
            "caseBuilding",
            false,
            "WA",
            "WaCaseType"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 2);

        caseId1Task2Id = response
            .then()
            .body("size()", is(2))
            .assertThat().body("[1].id", notNullValue())
            .extract()
            .path("[1].id");

        // initiate task3, category (caseProgression)
        sendMessage(
            caseIdForTask1,
            "listCma",
            null,
            "cmaListed",
            false,
            "WA",
            "WaCaseType"
        );

        response = findTasksByCaseId(
            caseIdForTask1, 3);

        String caseId1Task3Id = response
            .then()
            .body("size()", is(3))
            .assertThat().body("[2].id", notNullValue())
            .extract()
            .path("[2].id");

        // send warning message
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication101",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        waitSeconds(5);

        String timeExtensionWarning = "[{\"warningCode\":\"Code101\","
                                      + "\"warningText\":\"Warning Text 101\"}]";
        WarningValues timeExtensionWarningValues = new WarningValues(timeExtensionWarning);

        String followUpOverdueWarning = "[{\"warningCode\":\"Code102\","
                                        + "\"warningText\":\"Warning Text 102\"}]";
        WarningValues overdueWarningValues = new WarningValues(followUpOverdueWarning);

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task1Id, timeExtensionWarningValues);
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task2Id, overdueWarningValues);

        // task3Id should not contain any warnings because the category is not correlated
        assertTaskWithoutWarnings(caseIdForTask1, caseId1Task3Id, false);

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
        taskIdStatusMap.put(caseId1Task2Id, "completed");
        taskIdStatusMap.put(caseId1Task3Id, "completed");
    }

    /**
     * Scenario: 1 event on 2 different caseIds creates 2 different warnings each for all task types.
     */
    @Test
    public void given_multiple_caseIDs_when_actions_is_warn_then_mark_all_tasks_with_warnings() {
        //caseId1 with category Case progression

        String caseId1 = getCaseId();
        String taskIdDmnColumn = "attendCma";
        caseId1Task1Id = createTaskWithId(
            caseId1,
            "listCma",
            "",
            "cmaListed",
            false,
            taskIdDmnColumn,
            "WA",
            "WaCaseType"
        );

        //caseId1 with category Case progression

        String caseId2 = getCaseId();
        String taskId2DmnColumn = "reviewRespondentResponse";
        caseId1Task2Id = createTaskWithId(
            caseId2,
            "uploadHomeOfficeAppealResponse",
            "",
            "respondentReview",
            false,
            taskId2DmnColumn,
            "WA",
            "WaCaseType"
        );

        // Then cancel all tasks on both caseIDs
        sendMessage(
            caseId1,
            "_DUMMY_makeAnApplication",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );
        waitSeconds(5);
        sendMessage(
            caseId2,
            "_DUMMY_makeAnApplication",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
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
        assertTaskHasMultipleWarnings(caseId2, caseId1Task2Id, new WarningValues(warningsAsJson));

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
        taskIdStatusMap.put(caseId1Task2Id, "completed");
    }

    /**
     * Scenario: 1 event creates the same warning on all tasks twice?
     * (should only be able to add the same warning once - if warning code is already on task, then do not add).
     */
    @Test
    public void given_caseID_when_action_is_warn_with_same_warnings_then_add_the_warning_only_once() {

        String caseIdForTask1 = getCaseId();

        sendMessage(
            caseIdForTask1,
            "submitCase",
            null,
            "caseUnderReview",
            false,
            "WA",
            "WaCaseType"
        );

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication103",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );
        waitSeconds(5);

        String singleWarning = "[{\"warningCode\":\"Code103\","
                               + "\"warningText\":\"Warning Text 103\"}]";
        WarningValues warningValues = new WarningValues(singleWarning);

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task1Id, warningValues);

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    /**
     * Scenario: 1 event creates the same warning on tasks of a different category.
     */
    @Test
    public void given_caseId_with_different_category_when_same_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1, category (timeExtension)
        sendMessage(
            caseIdForTask1,
            "submitTimeExtension",
            null,
            "",
            false,
            "WA",
            "WaCaseType"
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

        // initiate task2, category (followUpOverdue)
        sendMessage(
            caseIdForTask1,
            "requestCaseBuilding",
            null,
            "caseBuilding",
            false,
            "WA",
            "WaCaseType"
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
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication104",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        String singleWarning = "[{\"warningCode\":\"Code104\","
                               + "\"warningText\":\"Warning Text 104\"}]";

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task1Id, new WarningValues(singleWarning));
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task2Id, new WarningValues(singleWarning));

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
        taskIdStatusMap.put(caseId1Task2Id, "completed");
    }

    /**
     * Scenario: 1 event creates warning with no ID or description on all tasks.
     */
    @Test
    public void given_caseId_when_warning_raised_without_warning_attributes_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1, category (Case progression)
        sendMessage(
            caseIdForTask1,
            "submitCase",
            null,
            "caseUnderReview",
            false,
            "WA",
            "WaCaseType"
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
        sendMessage(
            caseIdForTask1,
            "submitCase",
            null,
            "caseUnderReview",
            false,
            "WA",
            "WaCaseType"
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
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication102",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        // check for warnings flag on both the tasks
        assertTaskWithoutWarnings(caseIdForTask1, caseId1Task1Id, true);
        assertTaskWithoutWarnings(caseIdForTask1, caseId1Task2Id, true);

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
        taskIdStatusMap.put(caseId1Task2Id, "completed");
    }

    /**
     * Scenario: 1 event creates warning with no ID or description on tasks of a single category.
     */
    @Test
    public void given_caseId_with_category_when_warning_raised_without_warnings_then_mark_tasks_with_warning() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1, category (Case progression)
        sendMessage(
            caseIdForTask1,
            "submitCase",
            null,
            "caseUnderReview",
            false,
            "WA",
            "WaCaseType"
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

        // send warning message
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication102",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        // check for warnings flag on both the tasks
        assertTaskWithoutWarnings(caseIdForTask1, caseId1Task1Id, true);

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    /**
     * Scenario: 2 events creates same warnings on tasks of a single category.
     */
    @Test
    public void given_caseId_with_category_and_same_warnings_when_warnings_raised_then_mark_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1, category (timeExtension)
        sendMessage(
            caseIdForTask1,
            "submitTimeExtension",
            null,
            "",
            false,
            "WA",
            "WaCaseType"
        );

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // send warning message
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication105",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );
        waitSeconds(5);
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication106",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );
        waitSeconds(5);

        String singleWarning = "[{\"warningCode\":\"Code105\","
                               + "\"warningText\":\"Warning Text 105\"}]";

        // check for warnings flag on both the tasks
        assertTaskHasMultipleWarnings(caseIdForTask1, caseId1Task1Id, new WarningValues(singleWarning));

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
    }

    /**
     * Scenario: Single event with category and without warnings.
     */
    @Test
    public void given_caseId_with_without_warnings_when_warning_raised_then_mark_tasks_with_warnings() {

        String caseIdForTask1 = getCaseId();

        // Initiate task1, category (followUpOverdue)
        sendMessage(
            caseIdForTask1,
            "requestRespondentEvidence",
            null,
            "awaitingRespondentEvidence",
            false,
            "WA",
            "WaCaseType"
        );

        Response response = findTasksByCaseId(caseIdForTask1, 1);

        caseId1Task1Id = response
            .then()
            .body("size()", is(1))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

        // send warning message
        sendMessage(
            caseIdForTask1,
            "_DUMMY_makeAnApplication107",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        // check for warnings flag on both the tasks
        assertTaskWithoutWarnings(caseIdForTask1, caseId1Task1Id, true);

        // tear down all tasks
        taskIdStatusMap.put(caseId1Task1Id, "completed");
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
