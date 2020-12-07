package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    @Test
    public void given_validEventInformation_should_respond_with_200() {

        EventInformation validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .caseReference("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        String body = asJsonString(validEventInformation);
        System.out.println("body: " + body);

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(body)
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.OK_200);

    }
}
