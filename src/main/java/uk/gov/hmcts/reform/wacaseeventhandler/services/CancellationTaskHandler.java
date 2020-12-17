package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToCancelTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnResponse;

import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.DmnTable.TASK_CANCELLATION;

@Service
@Order(1)
public class CancellationTaskHandler implements CaseEventHandler {

    private final WorkflowApiClientToCancelTask workflowApiClientToCancelTask;

    public CancellationTaskHandler(WorkflowApiClientToCancelTask workflowApiClientToCancelTask) {
        this.workflowApiClientToCancelTask = workflowApiClientToCancelTask;
    }

    @Override
    public List<CancellationTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_CANCELLATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        EvaluateDmnRequest<CancellationTaskEvaluateDmnRequest> requestParameters =
            buildBodyWithCancellationTaskEvaluateDmnRequest(
                eventInformation.getPreviousStateId(),
                eventInformation.getEventId(),
                eventInformation.getNewStateId()
            );

        return workflowApiClientToCancelTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private EvaluateDmnRequest<CancellationTaskEvaluateDmnRequest> buildBodyWithCancellationTaskEvaluateDmnRequest(
        String previousStateId,
        String eventId,
        String newStateId
    ) {
        CancellationTaskEvaluateDmnRequest cancellationTaskEvaluateDmnRequestVariables =
            new CancellationTaskEvaluateDmnRequest(
                new DmnStringValue(eventId),
                new DmnStringValue(newStateId),
                new DmnStringValue(previousStateId)
            );

        return new EvaluateDmnRequest<>(cancellationTaskEvaluateDmnRequestVariables);
    }

    @Override
    public void handle(List<? extends TaskEvaluateDmnResponse> results, EventInformation eventInformation) {
        //todo
    }

}
