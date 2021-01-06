package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToCancelTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class CancellationTaskHandlerTest {

    private static final String DMN_NAME = "wa-task-cancellation-ia-asylum";
    public static final String TENANT_ID = "ia";


    @Mock
    private WorkflowApiClientToCancelTask workflowApiClientToCancelTask;

    @InjectMocks
    private CancellationTaskHandler handlerService;

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

        Mockito.when(workflowApiClientToCancelTask.evaluateDmn(
            DMN_NAME,
            requestParameters,
            TENANT_ID
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        handlerService.evaluateDmn(eventInformation);

        Mockito.verify(workflowApiClientToCancelTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters),
            eq(TENANT_ID)
        );

    }

    @Test
    void handle() throws NoSuchMethodException {
        // change for something meaningful once Cancellation handler is implemented
        assertThat(handlerService.getClass().getMethod("evaluateDmn", EventInformation.class).getName())
            .isEqualTo("evaluateDmn");
    }
}
