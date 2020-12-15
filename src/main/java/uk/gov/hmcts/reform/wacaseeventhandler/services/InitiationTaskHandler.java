package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskSendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.util.List;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private static final String DMN_NAME = "wa-task-initiation";
    public static final String MESSAGE_NAME = "createTaskMessage";
    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;
    private final IsoDateFormatter isoDateFormatter;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask,
                                 IsoDateFormatter isoDateFormatter) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
        this.isoDateFormatter = isoDateFormatter;
    }

    @Override
    public List<InitiateTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        return apiClientToInitiateTask.evaluateDmn(
            getTableKey(
                eventInformation.getJurisdictionId(),
                eventInformation.getCaseTypeId()
            ),
            buildBodyWithInitiateTaskEvaluateDmnRequest(
                eventInformation.getEventId(),
                eventInformation.getNewStateId()
            )
        ).getResults();
    }

    private String getTableKey(String jurisdictionId, String caseTypeId) {
        return DMN_NAME + "-" + jurisdictionId + "-" + caseTypeId;
    }

    private EvaluateDmnRequest<InitiateTaskEvaluateDmnRequest> buildBodyWithInitiateTaskEvaluateDmnRequest(
        String eventId, String newStateId
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
