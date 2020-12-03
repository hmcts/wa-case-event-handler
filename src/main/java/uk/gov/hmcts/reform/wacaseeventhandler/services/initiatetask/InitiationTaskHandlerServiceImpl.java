package uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WaWorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandlerService;

import java.util.List;

@Service
@Order(3)
public class InitiationTaskHandlerServiceImpl implements CaseEventHandlerService {

    private final WaWorkflowApiClient<InitiateTaskDmnRequest, InitiateTaskDmnResponse> waWorkflowApiClient;
    private final AuthTokenGenerator authTokenGenerator;

    public InitiationTaskHandlerServiceImpl(
        WaWorkflowApiClient<InitiateTaskDmnRequest, InitiateTaskDmnResponse> waWorkflowApiClient,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.waWorkflowApiClient = waWorkflowApiClient;
        this.authTokenGenerator = authTokenGenerator;
    }

    @Override
    public boolean canHandle() {

        List<EvaluateDmnResponse<InitiateTaskDmnResponse>> response = waWorkflowApiClient.evaluateDmn(
            authTokenGenerator.generate(),
            "getTask_IA_Asylum",
            buildInitiateTaskDmnRequest()
        );
        return !response.isEmpty();
    }

    private EvaluateDmnRequest<InitiateTaskDmnRequest> buildInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateTaskDmnRequest initiateTaskDmnRequestVariables = new InitiateTaskDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskDmnRequestVariables);
    }

    @Override
    public void handle() {

    }
}
