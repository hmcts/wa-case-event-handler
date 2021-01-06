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
    public void given_initiated_tasks_then_cancel_task() {
        // Given multiple existing tasks

        // create task1
        String caseIdForTask1 = UUID.randomUUID().toString();
        String task1Id = initiateTaskForGivenId(caseIdForTask1);

        // create task2
        String caseIdForTask2 = UUID.randomUUID().toString();
        String task2Id = initiateTaskForGivenId(caseIdForTask2);

        // Then cancel the task1
        String eventToCancelTask = "submitReasonsForAppeal";
        String previousStateToCancelTask = "awaitingReasonsForAppeal";
        sendMessage(caseIdForTask1, eventToCancelTask, previousStateToCancelTask, "");

        // Assert the task1 is deleted
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

    private void sendMessage(String caseId, String event, String previousState, String newStateId) {
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(LocalDateTime.now().plusDays(2))
            .caseReference(caseId)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId(event)
            .newStateId(newStateId)
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

    private String initiateTaskForGivenId(String caseId) {
        String eventToInitiateTask = "submitTimeExtension";

        sendMessage(caseId, eventToInitiateTask, "", "");

        return findTaskForGivenCaseId(caseId);
    }

    private String findTaskForGivenCaseId(String caseId) {
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
            .body("[0].name", is("Decide On Time Extension"))
            .body("[0].formKey", is("decideOnTimeExtension"))
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
