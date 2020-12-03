package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class EvaluateDmnRequestToInitiateTaskTest {

    @Autowired
    private JacksonTester<EvaluateDmnRequest<InitiateTaskDmnRequest>> jacksonTester;

    @Test
    public void given_a_initiate_task_dmn_request_to_evaluate_then_serialize_as_expected() throws IOException {
        EvaluateDmnRequest<InitiateTaskDmnRequest> evaluateDmnRequest = givenEvaluateDmnRequest();

        JsonContent<EvaluateDmnRequest<InitiateTaskDmnRequest>> evaluateDmnRequestAsJson =
            jacksonTester.write(evaluateDmnRequest);

        assertThat(evaluateDmnRequestAsJson).isEqualToJson("evaluate-dmn-request.json");
    }

    private EvaluateDmnRequest<InitiateTaskDmnRequest> givenEvaluateDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("appealSubmitted");
        InitiateTaskDmnRequest initiateTaskDmnRequestVariables = new InitiateTaskDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskDmnRequestVariables);
    }

}
