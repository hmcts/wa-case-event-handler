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
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandlerService;

import java.util.List;

@Service
@Order(3)
@Slf4j
public class InitiationTaskHandlerServiceImpl implements CaseEventHandlerService {

    private final WaWorkflowApiClientToInitiateTask apiClientToInitiateTask;

    public InitiationTaskHandlerServiceImpl(WaWorkflowApiClientToInitiateTask apiClientToInitiateTask) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
    }

    @Override
    public boolean canHandle() {
        List<EvaluateDmnResponse<InitiateTaskDmnResponse>> response = apiClientToInitiateTask.evaluateDmn(
            "getTask_IA_Asylum",
            buildBodyWithInitiateTaskDmnRequest()
        );
        return !response.isEmpty();
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
