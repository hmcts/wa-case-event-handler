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
    public void given_ccdEventMessage_should_respond_with_200() {

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance id")
            .caseReference("some case ref")
            .eventId("some event id")
            .newStateId("some new statie id")
            .userId("some user id")
            .build();

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.OK_200);

    }
}
