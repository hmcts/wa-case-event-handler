package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;

import java.util.List;

public final class InitiateTaskHelper {

    private InitiateTaskHelper() {
        //not called
    }

    public static EvaluateDmnResponse<InitiateEvaluateResponse> buildInitiateTaskDmnResponse() {
        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(new DmnStringValue("processApplication"))
            .group(new DmnStringValue("TCW"))
            .workingDaysAllowed(new DmnIntegerValue(2))
            .name(new DmnStringValue("Process Application"))
            .build();

        return new EvaluateDmnResponse<>(List.of(result));
    }

    public static EvaluateDmnRequest<InitiateEvaluateRequest> buildInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateEvaluateRequest initiateEvaluateRequestVariables =
            new InitiateEvaluateRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateEvaluateRequestVariables);
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.UPPER_CAMEL_CASE).writeValueAsString(obj);
    }
}
