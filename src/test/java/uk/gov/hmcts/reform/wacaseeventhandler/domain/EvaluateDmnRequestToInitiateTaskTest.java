package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class EvaluateDmnRequestToInitiateTaskTest {

    @Autowired
    private JacksonTester<EvaluateDmnRequest<InitiateEvaluateRequest>> jacksonTester;

    @Test
    public void serialize_as_expected() throws IOException {

        JsonContent<EvaluateDmnRequest<InitiateEvaluateRequest>> evaluateDmnRequestAsJson =
            jacksonTester.write(InitiateTaskHelper.buildInitiateTaskDmnRequest());

        assertThat(evaluateDmnRequestAsJson).isEqualToJson("evaluate-dmn-request.json");
    }

}
