package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.io.IOException;

@RunWith(SpringRunner.class)
@JsonTest
public class EvaluateDmnResponseTest {

    @Autowired
    private JacksonTester<EvaluateDmnResponse<InitiateTaskDmnResponse>> jacksonTester;

    @Test
    @Ignore
    public void given_a_initiate_task_dmn_response_from_evaluate_then_deserialize_as_expected() throws IOException {

        ObjectContent<EvaluateDmnResponse<InitiateTaskDmnResponse>> evaluateDmnResponseObjectContent =
            jacksonTester.read("evaluate-dmn-response.json");

        evaluateDmnResponseObjectContent.assertThat().isEqualTo(InitiateTaskHelper.buildInitiateTaskDmnResponse());

    }
}
