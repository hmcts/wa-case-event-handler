package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;


@JsonTest
class EvaluateDmnRequestTest {
    @Autowired
    private JacksonTester<EvaluateDmnRequest> jacksonTester;

    @Test
    void isWellImplemented() {

        final Class<?> classUnderTest = EvaluateDmnRequest.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

    @Test
    void serialize_as_expected() throws IOException {

        JsonContent<EvaluateDmnRequest> evaluateDmnRequestAsJson =
            jacksonTester.write(buildInitiateTaskDmnRequest("2020-04-12", "2020-03-29"));

        assertThat(evaluateDmnRequestAsJson).isEqualToJson("evaluate-dmn-request.json");
    }

    private EvaluateDmnRequest buildInitiateTaskDmnRequest(String now, String directionDueDate) {

        Map<String, DmnValue<?>> variables = Map.of(
            "eventId", dmnStringValue("submitAppeal"),
            "postEventState", dmnStringValue(""),
            "appealType", dmnStringValue("protection"),
            "now", dmnStringValue(now),
            "directionDueDate", dmnStringValue(directionDueDate)
        );

        return new EvaluateDmnRequest(variables);
    }
}
