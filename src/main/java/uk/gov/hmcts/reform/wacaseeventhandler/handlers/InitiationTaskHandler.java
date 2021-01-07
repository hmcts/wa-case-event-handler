package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.HandlerConstants.TASK_INITIATION;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;
    private final IsoDateFormatter isoDateFormatter;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask,
                                 IsoDateFormatter isoDateFormatter) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
        this.isoDateFormatter = isoDateFormatter;
    }

    @Override
    public List<InitiateEvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_INITIATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        EvaluateDmnRequest<InitiateEvaluateRequest> requestParameters = getParameterRequest(
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        return apiClientToInitiateTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private EvaluateDmnRequest<InitiateEvaluateRequest> getParameterRequest(
        String eventId,
        String newStateId
    ) {
        InitiateEvaluateRequest variables = new InitiateEvaluateRequest(
            new DmnStringValue(eventId),
            new DmnStringValue(newStateId)
        );

        return new EvaluateDmnRequest<>(variables);
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        results.stream()
            .filter(result -> result instanceof InitiateEvaluateResponse)
            .map(result -> (InitiateEvaluateResponse) result)
            .forEach(initiateEvaluateResponse -> apiClientToInitiateTask.sendMessage(
                buildSendMessageRequest(initiateEvaluateResponse, eventInformation)
            ));
    }

    private SendMessageRequest<InitiateProcessVariables, CorrelationKeys> buildSendMessageRequest(
        InitiateEvaluateResponse initiateEvaluateResponse,
        EventInformation eventInformation
    ) {

        return SendMessageRequest.<InitiateProcessVariables, CorrelationKeys>builder()
            .messageName(TASK_INITIATION.getMessageName())
            .processVariables(buildProcessVariables(initiateEvaluateResponse, eventInformation))
            .build();
    }

    private InitiateProcessVariables buildProcessVariables(
        InitiateEvaluateResponse initiateEvaluateResponse,
        EventInformation eventInformation
    ) {

        return InitiateProcessVariables.builder()
            .caseType(new DmnStringValue(eventInformation.getCaseTypeId()))
            .dueDate(new DmnStringValue(isoDateFormatter.format(eventInformation.getDateTime())))
            .workingDaysAllowed(initiateEvaluateResponse.getWorkingDaysAllowed() == null ? new DmnIntegerValue(0) : initiateEvaluateResponse.getWorkingDaysAllowed())
            .group(initiateEvaluateResponse.getGroup())
            .jurisdiction(new DmnStringValue(eventInformation.getJurisdictionId()))
            .name(initiateEvaluateResponse.getName())
            .taskId(initiateEvaluateResponse.getTaskId())
            .caseId(new DmnStringValue(eventInformation.getCaseReference()))
            .taskCategory(initiateEvaluateResponse.getTaskCategory())
            .build();
    }

}
