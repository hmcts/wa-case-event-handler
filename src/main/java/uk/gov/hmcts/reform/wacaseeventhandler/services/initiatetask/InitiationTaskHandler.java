package uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskSendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandler;

import java.time.LocalDateTime;

@Service
@Order(3)
public class InitiationTaskHandler implements CaseEventHandler {

    private static final String DMN_NAME = "getTask";
    private final WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    public InitiationTaskHandler(WorkflowApiClientToInitiateTask apiClientToInitiateTask) {
        this.apiClientToInitiateTask = apiClientToInitiateTask;
    }

    @Override
    public boolean canHandle(EventInformation eventInformation) {
        EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse> response = apiClientToInitiateTask.evaluateDmn(
            getTableKey(eventInformation.getJurisdictionId(), eventInformation.getCaseTypeId()),
            buildBodyWithInitiateTaskEvaluateDmnRequest(eventInformation.getEventId(), eventInformation.getNewStateId())
        );

        return !response.getResults().isEmpty();
    }

    private String getTableKey(String jurisdictionId, String caseTypeId) {
        return DMN_NAME + "_" + jurisdictionId + "_" + caseTypeId;
    }

    private EvaluateDmnRequest<InitiateTaskEvaluateDmnRequest> buildBodyWithInitiateTaskEvaluateDmnRequest(
        String eventId, String newStateId
    ) {
        DmnStringValue eventIdDmnValue = new DmnStringValue(eventId);
        DmnStringValue postEventStateDmnValue = new DmnStringValue(newStateId);
        InitiateTaskEvaluateDmnRequest initiateTaskEvaluateDmnRequestVariables =
            new InitiateTaskEvaluateDmnRequest(eventIdDmnValue, postEventStateDmnValue);

        return new EvaluateDmnRequest<>(initiateTaskEvaluateDmnRequestVariables);
    }


    @Override
    public void handle() {
        SendMessageRequest<InitiateTaskSendMessageRequest> sendMessageRequest = new SendMessageRequest<>(
            "createTaskMessage",
            buildBodyWithInitiateSendMessageRequest()
        );

        apiClientToInitiateTask.sendMessage(sendMessageRequest);
    }

    private InitiateTaskSendMessageRequest buildBodyWithInitiateSendMessageRequest() {
        return InitiateTaskSendMessageRequest.builder()
            .caseType(new DmnStringValue("Asylum"))
            .dueDate(new DmnStringValue(LocalDateTime.now().toString()))
            .group(new DmnStringValue("TCW"))
            .jurisdiction(new DmnStringValue("IA"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .build();
    }
}
