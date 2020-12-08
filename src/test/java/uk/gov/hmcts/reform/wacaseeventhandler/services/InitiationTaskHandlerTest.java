package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskSendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask.InitiationTaskHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerTest {

    public static final String FIXED_DATE = "2020-12-08T15:53:36.530377";
    public static final String DMN_NAME = "wa-task-initiation-ia-asylum";
    @Mock
    private WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    @Captor
    private ArgumentCaptor<SendMessageRequest<InitiateTaskSendMessageRequest>> captor;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    private final EventInformation eventInformation = EventInformation.builder()
        .eventId("submitAppeal")
        .newStateId("")
        .jurisdictionId("ia")
        .caseTypeId("asylum")
        .caseReference("some case reference")
        .dateTime(LocalDateTime.parse(FIXED_DATE))
        .build();

    @Test
    void evaluateDmn() {

        EvaluateDmnRequest<InitiateTaskEvaluateDmnRequest> requestParameters =
            InitiateTaskHelper.buildInitiateTaskDmnRequest();

        Mockito.when(apiClientToInitiateTask.evaluateDmn(
            DMN_NAME,
            requestParameters
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        handlerService.evaluateDmn(eventInformation);

        Mockito.verify(apiClientToInitiateTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters)
        );
    }

    @Test
    void handle() {

        InitiateTaskEvaluateDmnResponse initiateTaskResponse = InitiateTaskEvaluateDmnResponse.builder()
            .group(new DmnStringValue("TCW"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .build();

        List<InitiateTaskEvaluateDmnResponse> results = List.of(initiateTaskResponse);

        handlerService.handle(results, eventInformation);

        Mockito.verify(apiClientToInitiateTask).sendMessage(captor.capture());
        SendMessageRequest<InitiateTaskSendMessageRequest> actualSendMessageRequest = captor.getValue();

        assertThat(actualSendMessageRequest).isEqualTo(getExpectedSendMessageRequest());
    }

    private SendMessageRequest<InitiateTaskSendMessageRequest> getExpectedSendMessageRequest() {
        InitiateTaskSendMessageRequest expectedInitiateTaskSendMessageRequest = InitiateTaskSendMessageRequest.builder()
            .caseType(new DmnStringValue("asylum"))
            .group(new DmnStringValue("TCW"))
            .jurisdiction(new DmnStringValue("ia"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .caseReference(new DmnStringValue("some case reference"))
            .dueDate(new DmnStringValue(FIXED_DATE))
            .build();

        return new SendMessageRequest<>(
            "createTaskMessage",
            expectedInitiateTaskSendMessageRequest
        );
    }
}
