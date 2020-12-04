package uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WaWorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandler;

@Service
@Order(3)
@Slf4j
public class InitiationTaskHandler implements CaseEventHandler {

    private final WaWorkflowApiClientToInitiateTask apiClientToInitiateTask;

    public InitiationTaskHandler(WaWorkflowApiClientToInitiateTask apiClientToInitiateTask) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
    }

    @Override
    public boolean canHandle() {
        EvaluateDmnResponse<InitiateTaskDmnResponse> response = apiClientToInitiateTask.evaluateDmn(
            "getTask_IA_Asylum",
            buildBodyWithInitiateTaskDmnRequest()
        );
        return !response.getResults().isEmpty();
    }

    private EvaluateDmnRequest<InitiateTaskDmnRequest> buildBodyWithInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateTaskDmnRequest initiateTaskDmnRequestVariables = new InitiateTaskDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskDmnRequestVariables);
    }


    @Override
    public void handle() {
        log.info("hey world!");
    }
}
