package uk.gov.hmcts.reform.wacaseeventhandler;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.Matchers.containsString;

public class WelcomeTest extends SpringBootFunctionalBaseTest {

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
