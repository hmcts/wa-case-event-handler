package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.DmnTable.TASK_INITIATION;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private static final String MESSAGE_NAME = "createTaskMessage";

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
            .messageName(MESSAGE_NAME)
            .processVariables(processVariables)
            .correlationKeys(null)
            .build();
    }

    private InitiateProcessVariables buildProcessVariables(
        InitiateEvaluateResponse response,
        EventInformation eventInformation
    ) {

        return InitiateProcessVariables.builder()
            .caseType(new DmnStringValue(eventInformation.getCaseTypeId()))
            .dueDate(new DmnStringValue(isoDateFormatter.format(eventInformation.getDateTime())))
            .workingDaysAllowed(response.getWorkingDaysAllowed())
            .group(response.getGroup())
            .jurisdiction(new DmnStringValue(eventInformation.getJurisdictionId()))
            .name(response.getName())
            .taskId(response.getTaskId())
            .caseId(new DmnStringValue(eventInformation.getCaseReference()))
            .build();
    }

}
