package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

import java.time.LocalDateTime;
import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    private final String caseId = UUID.randomUUID().toString();
    private String taskId;

    @Test
    public void given_validEventInformation_should_respond_with_200() {
        EventInformation validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(LocalDateTime.now().plusDays(2))
            .caseReference(caseId)
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .eventId("submitAppeal")
            .newStateId("")
            .userId("some user Id")
            .build();

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(asJsonString(validEventInformation))
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
            .prettyPeek()
            .then()
            .body("size()", is(1))
            .body("[0].name", is("Process Application"))
            .body("[0].formKey", is("processApplication"))
            .extract()
            .path("[0].id");

    }

    @After
    public void cleanUpTask() {

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .accept(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .when()
            .post(camundaUrl + "/task/{task-id}/complete", taskId);

        given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .accept(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .when()
            .get("/history/task?taskId=" + taskId)
            .then()
            .body("[0].deleteReason", is("completed"));
    }
}
