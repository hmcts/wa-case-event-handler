package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToCancelTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask.CancellationTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask.CancellationTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnResponse;

import java.util.List;

@Service
@Order(1)
public class CancellationTaskHandler implements CaseEventHandler {

    private static final String DMN_NAME = "wa-task-cancellation";

    private final WorkflowApiClientToCancelTask workflowApiClientToCancelTask;

    public CancellationTaskHandler(WorkflowApiClientToCancelTask workflowApiClientToCancelTask) {
        this.workflowApiClientToCancelTask = workflowApiClientToCancelTask;
    }

    @Override
    public List<CancellationTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = getTableKey(eventInformation.getJurisdictionId(), eventInformation.getCaseTypeId());

        EvaluateDmnRequest<CancellationTaskEvaluateDmnRequest> requestParameters =
            buildBodyWithCancellationTaskEvaluateDmnRequest(
                eventInformation.getPreviousStateId(),
                eventInformation.getEventId(),
                eventInformation.getNewStateId()
            );

        return workflowApiClientToCancelTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private String getTableKey(String jurisdictionId, String caseTypeId) {
        return DMN_NAME + "-" + jurisdictionId + "-" + caseTypeId;
    }

    private EvaluateDmnRequest<CancellationTaskEvaluateDmnRequest> buildBodyWithCancellationTaskEvaluateDmnRequest(
        String previousStateId, String eventId, String newStateId
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
