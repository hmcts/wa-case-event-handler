package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateTaskSendMessageRequest;
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
    public List<InitiateTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_INITIATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        EvaluateDmnRequest<InitiateTaskEvaluateDmnRequest> requestParameters =
            buildBodyWithInitiateTaskEvaluateDmnRequest(
                eventInformation.getEventId(),
                eventInformation.getNewStateId()
            );

        return apiClientToInitiateTask.evaluateDmn(tableKey, requestParameters).getResults();
    }

    private EvaluateDmnRequest<InitiateTaskEvaluateDmnRequest> buildBodyWithInitiateTaskEvaluateDmnRequest(
        String eventId,
        String newStateId
    ) {
        InitiateTaskEvaluateDmnRequest initiateTaskEvaluateDmnRequestVariables = new InitiateTaskEvaluateDmnRequest(
            new DmnStringValue(eventId),
            new DmnStringValue(newStateId)
        );

        return new EvaluateDmnRequest<>(initiateTaskEvaluateDmnRequestVariables);
    }

    @Override
    public void handle(List<? extends TaskEvaluateDmnResponse> results, EventInformation eventInformation) {

        SendMessageRequest<InitiateTaskSendMessageRequest> sendMessageRequest = new SendMessageRequest<>(
            MESSAGE_NAME,
            buildBodyWithInitiateSendMessageRequest((InitiateTaskEvaluateDmnResponse) results.get(0), eventInformation)
        );

        apiClientToInitiateTask.sendMessage(sendMessageRequest);
    }

    private InitiateTaskSendMessageRequest buildBodyWithInitiateSendMessageRequest(
        InitiateTaskEvaluateDmnResponse response,
        EventInformation eventInformation
    ) {

        return InitiateTaskSendMessageRequest.builder()
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
