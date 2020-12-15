package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToCancelTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask.CancellationCorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask.CancellationEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.SendMessageRequest;

import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.DmnTable.TASK_CANCELLATION;

@Service
@Order(1)
public class CancellationTaskHandler implements CaseEventHandler {

    private static final String MESSAGE_NAME = "cancelTasks";
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

        EvaluateDmnRequest<CancellationEvaluateRequest> requestParameters =
            buildBodyWithCancellationTaskEvaluateDmnRequest(
                eventInformation.getPreviousStateId(),
                eventInformation.getEventId(),
                eventInformation.getNewStateId()
            );

        return workflowApiClientToCancelTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private EvaluateDmnRequest<CancellationEvaluateRequest> buildBodyWithCancellationTaskEvaluateDmnRequest(
        String previousStateId,
        String eventId,
        String newStateId
    ) {
        CancellationEvaluateRequest cancellationTaskEvaluateDmnRequestVariables =
            new CancellationEvaluateRequest(
                new DmnStringValue(eventId),
                new DmnStringValue(newStateId),
                new DmnStringValue(previousStateId)
            );

        return new EvaluateDmnRequest<>(cancellationTaskEvaluateDmnRequestVariables);
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {

        SendMessageRequest<ProcessVariables, CancellationCorrelationKeys> sendMessageRequest =
            SendMessageRequest.<ProcessVariables, CancellationCorrelationKeys>builder()
                .messageName(MESSAGE_NAME)
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
