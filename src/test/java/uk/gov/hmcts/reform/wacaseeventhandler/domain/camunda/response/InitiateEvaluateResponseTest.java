package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;


@JsonTest
class InitiateEvaluateResponseTest {
    @Autowired
    private JacksonTester<EvaluateDmnResponse<InitiateEvaluateResponse>> jacksonTester;

    @Test
    void isWellImplemented() {

        final Class<?> classUnderTest = InitiateEvaluateResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }

    @Test
    void serialize_as_expected() throws IOException {
        JsonContent<EvaluateDmnResponse<InitiateEvaluateResponse>> evaluateDmnRequestAsJson =
            jacksonTester.write(InitiateTaskHelper.buildInitiateTaskDmnResponse());

        assertThat(evaluateDmnRequestAsJson).isEqualToJson("evaluate-dmn-response.json");
    }
}
