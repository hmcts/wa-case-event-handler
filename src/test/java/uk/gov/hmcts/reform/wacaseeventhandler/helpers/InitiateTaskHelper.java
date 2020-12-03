package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;

public final class InitiateTaskHelper {

    private  InitiateTaskHelper() {
        //not called
    }

    public static EvaluateDmnResponse<InitiateTaskDmnResponse> buildInitiateTaskDmnResponse() {
        DmnStringValue taskId = new DmnStringValue("some taskId value");
        DmnStringValue group = new DmnStringValue("some group value");
        DmnIntegerValue workingDaysAllowed = new DmnIntegerValue(1);
        DmnStringValue name = new DmnStringValue("some name value");
        InitiateTaskDmnResponse result = new InitiateTaskDmnResponse(taskId, group, workingDaysAllowed, name);

        return new EvaluateDmnResponse<>(result);
    }

    public static EvaluateDmnRequest<InitiateTaskDmnRequest> buildInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateTaskDmnRequest initiateTaskDmnRequestVariables = new InitiateTaskDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskDmnRequestVariables);
    }
}
