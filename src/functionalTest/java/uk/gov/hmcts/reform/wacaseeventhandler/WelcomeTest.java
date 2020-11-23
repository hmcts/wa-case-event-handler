package uk.gov.hmcts.reform.wacaseeventhandler;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.Matchers.containsString;

public class WelcomeTest extends SpringBootFunctionalBaseTest {

    @Before
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.with().contentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void should_welcome_with_200_response_code_and_welcome_message() {

        Response result = given()
            .get("/");

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body(containsString("Welcome to wa-case-event-handler"));
    }
}
