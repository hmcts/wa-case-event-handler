package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;

import java.time.LocalDateTime;
import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    private String taskToTearDown;

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiated_tasks_then_cancel_task() throws InterruptedException {
        // create task1
        String caseIdForTask1 = UUID.randomUUID().toString();
        String task1Id = initiateTaskForGivenId(caseIdForTask1, "uploadHomeOfficeBundle",
                                                "awaitingRespondentEvidence", "",
                                                false, "Review Respondent Evidence",
                                                "reviewRespondentEvidence");

        // create task2
        String caseIdForTask2 = UUID.randomUUID().toString();
        String task2Id = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
                                                "", "",
                                                false, "Process Application",
                                                "processApplication");

        // Then cancel the task1
        sendMessage(caseIdForTask1, "uploadHomeOfficeBundle", "", "awaitingRespondentEvidence", false);
        assertTaskDoesNotExist(caseIdForTask1);
        assertTaskDeleteReason(task1Id, "deleted");

        // add tasks to tear down.
        taskToTearDown = task2Id;
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void given_initiated_tasks_with_delayedTimer_then_cancel_task() throws InterruptedException {
        // create task1
        String caseIdForTask1 = UUID.randomUUID().toString();
        String task1Id = initiateTaskForGivenId(caseIdForTask1, "uploadHomeOfficeBundle",
                                      "awaitingRespondentEvidence", "",
                                      true, "Review Respondent Evidence",
                                      "reviewRespondentEvidence");

        // create task2
        String caseIdForTask2 = UUID.randomUUID().toString();
        String task2Id = initiateTaskForGivenId(caseIdForTask2, "submitAppeal",
                                                "caseUnderReview", "",
                                                true, "Process Application",
                                                "processApplication");

        // Then cancel the task1
        sendMessage(caseIdForTask1, "uploadHomeOfficeBundle", "", "awaitingRespondentEvidence", false);
        assertTaskDoesNotExist(caseIdForTask1);
        assertTaskDeleteReason(task1Id, "deleted");

        // add tasks to tear down.
        taskToTearDown = task2Id;

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

    private void assertTaskDoesNotExist(String caseIdForTask1) {
        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task")
            .param("processVariables", "caseId_eq_" + caseIdForTask1)
            .when()
            .get()
            .then()
            .body("size()", is(0));
    }

    private void sendMessage(String caseId, String event, String newState,
                             String previousState, boolean taskDelay) {

        LocalDateTime delayTimer = LocalDateTime.now();

        if (taskDelay) {
            delayTimer = LocalDateTime.now().plusSeconds(2);
        }

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(delayTimer)
            .caseReference(caseId)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId(event)
            .newStateId(newState)
            .previousStateId(previousState)
            .userId("some user Id")
            .build();

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private String initiateTaskForGivenId(String caseId, String eventId, String newStateId, String previousStateId,
                                          boolean taskDelay, String name, String formKey) throws InterruptedException {
        sendMessage(caseId, eventId, newStateId, previousStateId, taskDelay);

        if (taskDelay) {
            Thread.sleep(5000);
        }

        return findTaskForGivenCaseId(caseId, name, formKey);
    }

    private String findTaskForGivenCaseId(String caseId, String name, String formKey) {
        return given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task")
            .param("processVariables", "caseId_eq_" + caseId)
            .when()
            .get()
            .then()
            .body("size()", is(1))
            .body("[0].name", is(name))
            .body("[0].formKey", is(formKey))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");
    }

    @After
    public void cleanUpTask() {
        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .accept(APPLICATION_JSON_VALUE)
            .contentType(APPLICATION_JSON_VALUE)
            .when()
            .post(camundaUrl + "/task/{task-id}/complete", taskToTearDown);

        assertTaskDeleteReason(taskToTearDown, "completed");
    }

}
