package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class EvaluateDmnResponseToInitiateTaskTest {

    @Autowired
    private JacksonTester<EvaluateDmnResponse<InitiateEvaluateResponse>> jacksonTester;

    @Test
    public void deserialize_as_expected() throws IOException {
        ObjectContent<EvaluateDmnResponse<InitiateEvaluateResponse>> evaluateDmnResponseObjectContent =
            jacksonTester.read("evaluate-dmn-response.json");

        evaluateDmnResponseObjectContent.assertThat().isEqualTo(InitiateTaskHelper.buildInitiateTaskDmnResponse());
    }

    @Test
    public void serialize_as_expected() throws IOException {
        JsonContent<EvaluateDmnResponse<InitiateEvaluateResponse>> evaluateDmnRequestAsJson =
            jacksonTester.write(InitiateTaskHelper.buildInitiateTaskDmnResponse());

        System.out.println(new Gson().toJson(evaluateDmnRequestAsJson.getJson()));
        assertThat(evaluateDmnRequestAsJson).isEqualToJson("evaluate-dmn-response.json");
    }

}
