package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

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

        String tenantId = eventInformation.getJurisdictionId();

        EvaluateDmnRequest<CancellationEvaluateRequest> requestParameters = getParameterRequest(
            eventInformation.getPreviousStateId(),
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        return workflowApiClientToCancelTask.evaluateDmn(tableKey, requestParameters, tenantId).getResults();
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
            .filter(result -> result instanceof CancellationEvaluateResponse)
            .filter(result -> ((CancellationEvaluateResponse) result).getAction().getValue().equals("Cancel"))
            .map(result -> (CancellationEvaluateResponse) result)
            .forEach(cancellationEvaluateResponse -> {
                DmnStringValue taskCategories = cancellationEvaluateResponse.getTaskCategories();
                String value = taskCategories.getValue();
                sendMessageToCancelTasksForGivenCorrelations(
                    eventInformation.getCaseId(),
                    value
                );
            });
    }

    private void sendMessageToCancelTasksForGivenCorrelations(String caseReference, String category) {
        workflowApiClientToCancelTask.sendMessage(buildSendMessageRequest(category, caseReference));
    }

    private SendMessageRequest<ProcessVariables, CancellationCorrelationKeys> buildSendMessageRequest(
        String taskCategory,
        String caseReference
    ) {
        return SendMessageRequest.<ProcessVariables, CancellationCorrelationKeys>builder()
            .messageName(TASK_CANCELLATION.getMessageName())
            .correlationKeys(CancellationCorrelationKeys.builder()
                                 .caseId(new DmnStringValue(caseReference))
                                 .taskCategory(new DmnStringValue(taskCategory))
                                 .build())
            .build();
    }

}
