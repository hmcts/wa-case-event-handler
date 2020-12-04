package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;

import java.util.List;

public final class InitiateTaskHelper {

    private  InitiateTaskHelper() {
        //not called
    }

    public static EvaluateDmnResponse<InitiateTaskDmnResponse> buildInitiateTaskDmnResponse() {
        DmnStringValue taskId = new DmnStringValue("processApplication");
        DmnStringValue group = new DmnStringValue("TCW");
        DmnIntegerValue workingDaysAllowed = new DmnIntegerValue(2);
        DmnStringValue name = new DmnStringValue("Process Application");
        InitiateTaskDmnResponse result = new InitiateTaskDmnResponse(taskId, group, workingDaysAllowed, name);

        return new EvaluateDmnResponse<>(List.of(result));
    }

    public static EvaluateDmnRequest<InitiateTaskDmnRequest> buildInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateTaskDmnRequest initiateTaskDmnRequestVariables = new InitiateTaskDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskDmnRequestVariables);
    }
}
