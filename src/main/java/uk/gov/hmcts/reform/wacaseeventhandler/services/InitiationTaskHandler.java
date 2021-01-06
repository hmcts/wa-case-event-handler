package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.ZonedDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.HandlerConstants.TASK_INITIATION;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;
    private final IsoDateFormatter isoDateFormatter;
    private final DueDateService dueDateService;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask,
                                 IsoDateFormatter isoDateFormatter, DueDateService dueDateService) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
        this.isoDateFormatter = isoDateFormatter;
        this.dueDateService = dueDateService;
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

        SendMessageRequest<InitiateProcessVariables, CorrelationKeys> sendMessageRequest =
            buildSendMessageRequest(results, eventInformation);

        apiClientToInitiateTask.sendMessage(sendMessageRequest);
    }

    private SendMessageRequest<InitiateProcessVariables, CorrelationKeys> buildSendMessageRequest(
        List<? extends EvaluateResponse> results,
        EventInformation eventInformation
    ) {

        InitiateProcessVariables processVariables = buildProcessVariables(
            (InitiateEvaluateResponse) results.get(0),
            eventInformation
        );

        return SendMessageRequest.<InitiateProcessVariables, CorrelationKeys>builder()
            .messageName(TASK_INITIATION.getMessageName())
            .processVariables(processVariables)
            .correlationKeys(null)
            .build();
    }

    private InitiateProcessVariables buildProcessVariables(
        InitiateEvaluateResponse response,
        EventInformation eventInformation
    ) {

        ZonedDateTime delayUntil = dueDateService.calculateDueDate(
            ZonedDateTime.parse(isoDateFormatter.format(eventInformation.getDateTime())),
                                    response.getWorkingDaysAllowed().getValue()
        );

        return InitiateProcessVariables.builder()
            .caseType(new DmnStringValue(eventInformation.getCaseTypeId()))
            .workingDaysAllowed(response.getWorkingDaysAllowed())
            .group(response.getGroup())
            .jurisdiction(new DmnStringValue(eventInformation.getJurisdictionId()))
            .name(response.getName())
            .taskId(response.getTaskId())
            .caseId(new DmnStringValue(eventInformation.getCaseReference()))
            .delayUntil(new DmnStringValue(delayUntil.toString()))
            .build();
    }

}
