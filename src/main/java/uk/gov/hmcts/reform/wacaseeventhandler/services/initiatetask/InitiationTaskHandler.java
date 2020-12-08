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

import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotEmpty;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private static final String DMN_NAME = "getTask";
    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
    }

    @Override
    public List<InitiateTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        return apiClientToInitiateTask.evaluateDmn(
            getTableKey(eventInformation.getJurisdictionId(), eventInformation.getCaseTypeId()),
            buildBodyWithInitiateTaskEvaluateDmnRequest(eventInformation.getEventId(), eventInformation.getNewStateId())
        ).getResults();
    }

    private String getTableKey(String jurisdictionId, String caseTypeId) {
        return DMN_NAME + "_" + jurisdictionId + "_" + caseTypeId;
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
    public void handle(List<? extends TaskEvaluateDmnResponse> results,
                       String caseTypeId,
                       String jurisdictionId) {

        SendMessageRequest<InitiateTaskSendMessageRequest> sendMessageRequest = new SendMessageRequest<>(
            "createTaskMessage",
            buildBodyWithInitiateSendMessageRequest(
                (InitiateTaskEvaluateDmnResponse) results.get(0),
                caseTypeId,
                jurisdictionId
            )
        );

        apiClientToInitiateTask.sendMessage(sendMessageRequest);
    }

    private InitiateTaskSendMessageRequest buildBodyWithInitiateSendMessageRequest(
        InitiateTaskEvaluateDmnResponse response,
        @NotEmpty String caseTypeId,
        @NotEmpty String jurisdictionId
    ) {
        return InitiateTaskSendMessageRequest.builder()
            .caseType(new DmnStringValue(caseTypeId))
            .dueDate(new DmnStringValue(LocalDateTime.now().toString()))
            .group(response.getGroup())
            .jurisdiction(new DmnStringValue(jurisdictionId))
            .name(response.getName())
            .taskId(response.getTaskId())
            .build();
    }
}
