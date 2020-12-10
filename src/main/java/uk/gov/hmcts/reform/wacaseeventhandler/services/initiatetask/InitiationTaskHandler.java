package uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskSendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandler;

import java.util.List;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private static final String DMN_NAME = "wa-task-initiation";
    public static final String MESSAGE_NAME = "createTaskMessage";
    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
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
            .dueDate(new DmnStringValue(eventInformation.getDateTime().toString()))
            .workingDaysAllowed(response.getWorkingDaysAllowed())
            .group(response.getGroup())
            .jurisdiction(new DmnStringValue(eventInformation.getJurisdictionId()))
            .name(response.getName())
            .taskId(response.getTaskId())
            .caseId(new DmnStringValue(eventInformation.getCaseReference()))
            .build();
    }
}
