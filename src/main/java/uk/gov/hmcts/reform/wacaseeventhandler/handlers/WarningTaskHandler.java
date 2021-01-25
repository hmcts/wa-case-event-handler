package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToWarnTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationCorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningEvaluateResponse;

import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.HandlerConstants.TASK_WARN;

@Service
@Order(2)
public class WarningTaskHandler implements CaseEventHandler {

    private final WorkflowApiClientToWarnTask workflowApiClientToWarnTask;

    public WarningTaskHandler(WorkflowApiClientToWarnTask workflowApiClientToWarnTask) {
        this.workflowApiClientToWarnTask = workflowApiClientToWarnTask;
    }

    @Override
    public List<WarningEvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_WARN.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        EvaluateDmnRequest<CancellationEvaluateRequest> requestParameters = getParameterRequest(
            eventInformation.getPreviousStateId(),
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        return workflowApiClientToWarnTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private EvaluateDmnRequest<CancellationEvaluateRequest> getParameterRequest(
        String previousStateId,
        String eventId,
        String newStateId
    ) {
        CancellationEvaluateRequest variables = new CancellationEvaluateRequest(
            new DmnStringValue(eventId),
            new DmnStringValue(newStateId),
            new DmnStringValue(previousStateId)
        );

        return new EvaluateDmnRequest<>(variables);
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        results.stream()
            .filter(result -> result instanceof WarningEvaluateResponse)
            .map(result -> (WarningEvaluateResponse) result)
            .forEach(cancellationEvaluateResponse -> sendMessageToWarnTasksForGivenCorrelations(
                eventInformation.getCaseReference(),
                cancellationEvaluateResponse.getAction().getValue()
            ));
    }

    private void sendMessageToWarnTasksForGivenCorrelations(String caseReference, String category) {
        workflowApiClientToWarnTask.sendMessage(buildSendMessageRequest(category, caseReference));
    }

    private SendMessageRequest<ProcessVariables, CancellationCorrelationKeys> buildSendMessageRequest(
        String taskCategory,
        String caseReference
    ) {
        return SendMessageRequest.<ProcessVariables, CancellationCorrelationKeys>builder()
            .messageName(TASK_WARN.getMessageName())
            .correlationKeys(CancellationCorrelationKeys.builder()
                                 .caseId(new DmnStringValue(caseReference))
                                 .taskCategory(new DmnStringValue(taskCategory))
                                 .build())
            .build();
    }
}
