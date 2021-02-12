package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

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
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.HandlerConstants.TASK_INITIATION;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;
    private final IsoDateFormatter isoDateFormatter;
    private final DueDateService dueDateService;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask,
                                 IdempotencyKeyGenerator idempotencyKeyGenerator,
                                 IsoDateFormatter isoDateFormatter,
                                 DueDateService dueDateService) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
        this.idempotencyKeyGenerator = idempotencyKeyGenerator;
        this.isoDateFormatter = isoDateFormatter;
        this.dueDateService = dueDateService;
    }

    @Override
    public List<InitiateEvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_INITIATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        String tenantId = TASK_INITIATION.getTenantId(eventInformation.getJurisdictionId());

        EvaluateDmnRequest<InitiateEvaluateRequest> requestParameters = getParameterRequest(
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        return apiClientToInitiateTask.evaluateDmn(tableKey, requestParameters, tenantId).getResults();
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
        String eventInfoDt = isoDateFormatter.format(eventInformation.getDateTime());

        ZonedDateTime delayUntil = dueDateService.calculateDueDate(
            ZonedDateTime.parse(eventInfoDt),
            cannotBeNull(initiateEvaluateResponse.getWorkingDaysAllowed()).getValue()
        );

        String idempotencyKey = idempotencyKeyGenerator.generateIdempotencyKey(
            eventInformation.getEventInstanceId(),
            initiateEvaluateResponse.getTaskId().getValue()
        );

        return InitiateProcessVariables.builder()
            .idempotencyKey(new DmnStringValue(idempotencyKey))
            .caseType(new DmnStringValue(eventInformation.getCaseTypeId()))
            .dueDate(new DmnStringValue(isoDateFormatter.format(eventInformation.getDateTime())))
            .workingDaysAllowed(cannotBeNull(initiateEvaluateResponse.getWorkingDaysAllowed()))
            .group(initiateEvaluateResponse.getGroup())
            .jurisdiction(new DmnStringValue(eventInformation.getJurisdictionId()))
            .name(initiateEvaluateResponse.getName())
            .taskId(initiateEvaluateResponse.getTaskId())
            .caseId(new DmnStringValue(eventInformation.getCaseId()))
            .taskCategory(initiateEvaluateResponse.getTaskCategory())
            .delayUntil(new DmnStringValue(delayUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .build();
    }

    private DmnIntegerValue cannotBeNull(DmnIntegerValue workingDaysAllowed) {
        return workingDaysAllowed == null ? new DmnIntegerValue(0) : workingDaysAllowed;
    }

}
