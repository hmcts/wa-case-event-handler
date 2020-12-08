package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskSendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask.InitiationTaskHandler;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerTest {

    @Mock
    private WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    @Test
    void evaluateDmn() {

        Mockito.when(apiClientToInitiateTask.evaluateDmn(
            "getTask_IA_Asylum",
            InitiateTaskHelper.buildInitiateTaskDmnRequest()
        ))
            .thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        EventInformation eventInformation = EventInformation.builder()
            .eventId("submitAppeal")
            .newStateId("")
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .build();

        handlerService.evaluateDmn(eventInformation);

        Mockito.verify(apiClientToInitiateTask).evaluateDmn(
            eq("getTask_IA_Asylum"),
            eq(InitiateTaskHelper.buildInitiateTaskDmnRequest())
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void handle() {

        InitiateTaskEvaluateDmnResponse initiateTaskResponse = InitiateTaskEvaluateDmnResponse.builder()
            .group(new DmnStringValue("TCW"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .build();

        List<InitiateTaskEvaluateDmnResponse> results = List.of(initiateTaskResponse);

        handlerService.handle(results, "Asylum", "IA");

        SendMessageRequest<InitiateTaskSendMessageRequest> expectedSendMessageRequest = getExpectedSendMessageRequest();

        ArgumentCaptor<SendMessageRequest> argument = ArgumentCaptor.forClass(SendMessageRequest.class);
        Mockito.verify(apiClientToInitiateTask).sendMessage(argument.capture());

        SendMessageRequest actualSendMessageRequest = argument.getValue();
        assertThat(actualSendMessageRequest.getMessageName()).isEqualTo(expectedSendMessageRequest.getMessageName());

        InitiateTaskSendMessageRequest actualInitiateTaskSendMessageRequest =
            (InitiateTaskSendMessageRequest) actualSendMessageRequest.getProcessVariables();

        assertThat(actualInitiateTaskSendMessageRequest).isEqualToComparingOnlyGivenFields(
            expectedSendMessageRequest.getProcessVariables(),
            "caseType", "group", "jurisdiction", "name", "taskId"
        );
    }

    private SendMessageRequest<InitiateTaskSendMessageRequest> getExpectedSendMessageRequest() {
        InitiateTaskSendMessageRequest expectedInitiateTaskSendMessageRequest = InitiateTaskSendMessageRequest.builder()
            .caseType(new DmnStringValue("Asylum"))
            .group(new DmnStringValue("TCW"))
            .jurisdiction(new DmnStringValue("IA"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .build();

        return new SendMessageRequest<>(
            "createTaskMessage",
            expectedInitiateTaskSendMessageRequest
        );
    }
}
