package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.CcdEventMessage;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

public class CaseEventHandlerControllerTest extends SpringBootFunctionalBaseTest {

    @Test
    public void given_ccdEventMessage_should_respond_with_200() {

        CcdEventMessage ccdEventMessage = CcdEventMessage.builder()
            .id("some id")
            .name("some name")
            .build();

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .body(asJsonString(ccdEventMessage))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.OK_200);

    }
}
