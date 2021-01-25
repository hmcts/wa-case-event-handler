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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningTaskHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarningTaskHandlerTest {

    private static final String DMN_NAME = "wa-task-cancellation-ia-asylum";
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
        .caseReference("some case reference")
        .dateTime(LocalDateTime.now())
        .build();

    @Test
    void evaluateDmn() {
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
            requestParameters
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        handlerService.evaluateDmn(eventInformation);

        verify(workflowApiClientToWarnTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters)
        );

    }


    @Test
    void handle() {
        WarningEvaluateResponse result1 = WarningEvaluateResponse.builder()
            .action(new DmnStringValue("Warn"))
            .build();

        WarningEvaluateResponse result2 = WarningEvaluateResponse.builder()
            .action(new DmnStringValue("Warn"))
            .build();

        List<WarningEvaluateResponse> results = List.of(result1, result2);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClientToWarnTask, times(2))
            .sendMessage(sendMessageRequestCaptor.capture());

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(0),
            "Warn"
        );

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(1),
            "Warn"
        );

    }

    private void assertSendMessageRequest(
        SendMessageRequest<ProcessVariables, CancellationCorrelationKeys> sendMessageRequest,
        String category
    ) {

        assertThat(sendMessageRequest.getMessageName()).isEqualTo(WARN_TASKS_MESSAGE_NAME);

        assertThat(sendMessageRequest.getCorrelationKeys())
            .isEqualTo(CancellationCorrelationKeys.builder()
                           .caseId(new DmnStringValue("some case reference"))
                           .taskCategory(new DmnStringValue(category))
                           .build());
    }
}
