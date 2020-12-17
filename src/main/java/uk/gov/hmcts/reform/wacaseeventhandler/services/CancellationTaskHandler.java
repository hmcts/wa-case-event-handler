package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToCancelTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationCorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;

import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.HandlerConstants.TASK_CANCELLATION;

@Service
@Order(1)
public class CancellationTaskHandler implements CaseEventHandler {

    private final WorkflowApiClientToCancelTask workflowApiClientToCancelTask;

    public CancellationTaskHandler(WorkflowApiClientToCancelTask workflowApiClientToCancelTask) {
        this.workflowApiClientToCancelTask = workflowApiClientToCancelTask;
    }

    @Override
    public List<CancellationEvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_CANCELLATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        EvaluateDmnRequest<CancellationEvaluateRequest> requestParameters = getParameterRequest(
            eventInformation.getPreviousStateId(),
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        return workflowApiClientToCancelTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private EvaluateDmnRequest<CancellationEvaluateRequest> getParameterRequest(
        String previousStateId,
        String eventId,
        String newStateId
    ) {
        CancellationEvaluateRequest variables =
            new CancellationEvaluateRequest(
                new DmnStringValue(eventId),
                new DmnStringValue(newStateId),
                new DmnStringValue(previousStateId)
            );

        return new EvaluateDmnRequest<>(variables);
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {

        SendMessageRequest<ProcessVariables, CancellationCorrelationKeys> sendMessageRequest =
            SendMessageRequest.<ProcessVariables, CancellationCorrelationKeys>builder()
                .messageName(TASK_CANCELLATION.getMessageName())
                .correlationKeys(getCorrelationKeys(eventInformation))
                .build();

        workflowApiClientToCancelTask.sendMessage(sendMessageRequest);

    }

    private CancellationCorrelationKeys getCorrelationKeys(EventInformation eventInformation) {
        return CancellationCorrelationKeys.builder()
            .caseId(new DmnStringValue(eventInformation.getCaseReference()))
            .build();
    }

}
