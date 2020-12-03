package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class EvaluateDmnRequestTest {

    @Autowired
    private JacksonTester<EvaluateDmnRequest> jacksonTester;

    @Test
    public void given_evaluate_dmn_request_then_serialize_as_expected() throws IOException {
        EvaluateDmnRequest evaluateDmnRequest = givenEvaluateDmnRequest();

        JsonContent<EvaluateDmnRequest> evaluateDmnRequestAsJson = jacksonTester.write(evaluateDmnRequest);

        assertThat(evaluateDmnRequestAsJson).isEqualToJson("evaluate-dmn-request.json");
    }

    private EvaluateDmnRequest givenEvaluateDmnRequest() {
        Map<String, DmnValue> variables = new ConcurrentHashMap<>();
        DmnValue eventIdValue = new DmnStringValue("submitAppeal");
        variables.put("eventId", eventIdValue);
        DmnValue postEventStateValue = new DmnStringValue("appealSubmitted");
        variables.put("postEventState", postEventStateValue);

        return new EvaluateDmnRequest(variables);
    }

}
