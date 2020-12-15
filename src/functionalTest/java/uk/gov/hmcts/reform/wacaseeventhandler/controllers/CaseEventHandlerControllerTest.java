package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;

import java.time.LocalDateTime;
import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    private final String caseId = UUID.randomUUID().toString();
    private String taskId;

    @Test
    public void given_eventInformation_from_ccd_should_initiate_task_and_respond_with_204() {
        String eventToInitiateTask = "submitAppeal";

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(LocalDateTime.now().plusDays(2))
            .caseReference(caseId)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId(eventToInitiateTask)
            .newStateId("")
            .previousStateId("")
            .userId("some user Id")
            .build();

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());


        taskId = given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task")
            .param("processVariables", "caseId_eq_" + caseId)
            .when()
            .get()
            .then()
            .body("size()", is(1))
            .body("[0].name", is("Process Application"))
            .body("[0].formKey", is("processApplication"))
            .assertThat().body("[0].id", notNullValue())
            .extract()
            .path("[0].id");

    }

    @Test
    public void given_eventInformation_from_ccd_should_cancel_task_and_respond_with_204() {
        String eventToCancelTask = "submitReasonsForAppeal";
        String previousStateToCancelTask = "awaitingReasonsForAppeal";

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(LocalDateTime.now().plusDays(2))
            .caseReference(caseId)
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId(eventToCancelTask)
            .newStateId("")
            .previousStateId(previousStateToCancelTask)
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

    @After
    public void cleanUpTask() {
        if (StringUtils.isNotEmpty(taskId)) {
            given()
                .header(SERVICE_AUTHORIZATION, s2sToken)
                .accept(APPLICATION_JSON_VALUE)
                .contentType(APPLICATION_JSON_VALUE)
                .when()
                .post(camundaUrl + "/task/{task-id}/complete", taskId);

            given()
                .contentType(APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON_VALUE)
                .header(SERVICE_AUTHORIZATION, s2sToken)
                .baseUri(camundaUrl)
                .when()
                .get("/history/task?taskId=" + taskId)
                .then()
                .body("[0].deleteReason", is("completed"));
            log.info("cleanUpTask done successfully");
        } else {
            log.info("cleanUpTask not needed, this test could be a cancel task test for instance.");
        }
    }
}
