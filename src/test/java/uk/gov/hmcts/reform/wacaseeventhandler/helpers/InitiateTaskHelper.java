package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskEvaluateDmnResponse;

import java.util.List;

public final class InitiateTaskHelper {

    private InitiateTaskHelper() {
        //not called
    }

    public static EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse> buildInitiateTaskDmnResponse() {
        InitiateTaskEvaluateDmnResponse result = InitiateTaskEvaluateDmnResponse.builder()
            .taskId(new DmnStringValue("processApplication"))
            .group(new DmnStringValue("TCW"))
            .workingDaysAllowed(new DmnIntegerValue(2))
            .name(new DmnStringValue("Process Application"))
            .build();

        return new EvaluateDmnResponse<>(List.of(result));
    }

    public static EvaluateDmnRequest<InitiateTaskEvaluateDmnRequest> buildInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateTaskEvaluateDmnRequest initiateTaskEvaluateDmnRequestVariables =
            new InitiateTaskEvaluateDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskEvaluateDmnRequestVariables);
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }
}
