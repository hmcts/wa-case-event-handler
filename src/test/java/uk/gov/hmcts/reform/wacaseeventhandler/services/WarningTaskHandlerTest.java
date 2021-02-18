package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToWarnTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationCorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningTaskHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarningTaskHandlerTest {

    private static final String DMN_NAME = "wa-task-cancellation-ia-asylum";
    public static final String TENANT_ID = "ia";
    public static final String WARN_TASKS_MESSAGE_NAME = "warnProcess";


    @Mock
    private WorkflowApiClientToWarnTask workflowApiClientToWarnTask;

    @Captor
    private ArgumentCaptor<SendMessageRequest<ProcessVariables, CancellationCorrelationKeys>> sendMessageRequestCaptor;

    @InjectMocks
    private WarningTaskHandler handlerService;

    private final EventInformation eventInformation = EventInformation.builder()
        .eventId("some event id")
        .newStateId("some post state")
        .previousStateId("some previous state")
        .jurisdictionId("ia")
        .caseTypeId("asylum")
        .caseId("some case reference")
        .eventTimeStamp(LocalDateTime.now())
        .build();

    @Test
    void should_call_evaluateDmn_and_return_empty_list_response() {
        CancellationEvaluateRequest cancellationTaskEvaluateDmnRequestVariables =
            CancellationEvaluateRequest.builder()
                .state(new DmnStringValue("some post state"))
                .event(new DmnStringValue("some event id"))
                .fromState(new DmnStringValue("some previous state"))
                .build();

        EvaluateDmnRequest<CancellationEvaluateRequest> requestParameters =
            new EvaluateDmnRequest<>(cancellationTaskEvaluateDmnRequestVariables);

        when(workflowApiClientToWarnTask.evaluateDmn(
            DMN_NAME,
            requestParameters,
            TENANT_ID
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        List<? extends EvaluateResponse> response = handlerService.evaluateDmn(eventInformation);

        verify(workflowApiClientToWarnTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters),
            eq(TENANT_ID)
        );
        assertEquals(response.size(),0);


    }

    @Test
    void should_call_evaluateDmn_and_with_results() {
        CancellationEvaluateRequest cancellationTaskEvaluateDmnRequestVariables =
            CancellationEvaluateRequest.builder()
                .state(new DmnStringValue("some post state"))
                .event(new DmnStringValue("some event id"))
                .fromState(new DmnStringValue("some previous state"))
                .build();

        EvaluateDmnRequest<CancellationEvaluateRequest> requestParameters =
            new EvaluateDmnRequest<>(cancellationTaskEvaluateDmnRequestVariables);

        when(workflowApiClientToWarnTask.evaluateDmn(
            DMN_NAME,
            requestParameters,
            TENANT_ID
        )).thenReturn(new EvaluateDmnResponse<WarningResponse>(List.of(new WarningResponse(
            new DmnStringValue("testValue"),
            new DmnStringValue("testCategory"))
                              ))
        );

        List<? extends EvaluateResponse> response = handlerService.evaluateDmn(eventInformation);

        verify(workflowApiClientToWarnTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters),
            eq(TENANT_ID)
        );

        assertEquals(1, response.size());


    }


    @Test
    void handle_send_message_with_results() {
        WarningResponse result1 = WarningResponse.builder()
            .action(new DmnStringValue("Warn"))
            .build();

        WarningResponse result2 = WarningResponse.builder()
            .action(new DmnStringValue("Warn"))
            .build();

        CancellationEvaluateResponse result3 = CancellationEvaluateResponse.builder()
            .action(new DmnStringValue("Cancel"))
            .build();

        List<EvaluateResponse> results = List.of(result1, result2,result3);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClientToWarnTask, times(2))
            .sendMessage(sendMessageRequestCaptor.capture());

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(0)
        );

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(1)
        );
    }

    @Test
    void handle_send_message_with_empty_results() {
        List<EvaluateResponse> results = Collections.emptyList();

        handlerService.handle(results, eventInformation);

        verify(workflowApiClientToWarnTask, times(0))
            .sendMessage(sendMessageRequestCaptor.capture());

    }

    private void assertSendMessageRequest(
        SendMessageRequest<ProcessVariables, CancellationCorrelationKeys> sendMessageRequest) {

        assertThat(sendMessageRequest.getMessageName()).isEqualTo(WARN_TASKS_MESSAGE_NAME);

        assertThat(sendMessageRequest.getCorrelationKeys())
            .isEqualTo(CancellationCorrelationKeys.builder()
                           .caseId(new DmnStringValue("some case reference"))
                           .build());
    }
}

