package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApi;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;

import static java.util.Objects.requireNonNull;

@Service
public class WorkflowService {

    private final WorkflowApi workflowApi;
    private final AuthTokenGenerator authTokenGenerator;

    @Autowired
    public WorkflowService(WorkflowApi workflowApi,
                           AuthTokenGenerator authTokenGenerator
    ) {
        this.workflowApi = workflowApi;
        this.authTokenGenerator = authTokenGenerator;
    }

    public EvaluateDmnResponse<? extends EvaluateResponse> evaluateDmn(String dmnTableKey,
                                                                      String tenantId,
                                                                      EvaluateDmnRequest<? extends EvaluateRequest> evaluateRequest
    ) {
        requireNonNull(dmnTableKey, "dmnTableKey cannot be null");
        requireNonNull(tenantId, "tenantId cannot be null");

        return workflowApi.evaluateDmnTable(
            dmnTableKey,
            tenantId,
            authTokenGenerator.generate(),
            evaluateRequest
        );
    }

    public void sendMessage(
        SendMessageRequest<? extends ProcessVariables, ? extends CorrelationKeys> sendMessageRequest
    ) {
        requireNonNull(sendMessageRequest, "sendMessageRequest cannot be null");

        workflowApi.sendMessage(
            authTokenGenerator.generate(),
            sendMessageRequest
        );
    }
}
