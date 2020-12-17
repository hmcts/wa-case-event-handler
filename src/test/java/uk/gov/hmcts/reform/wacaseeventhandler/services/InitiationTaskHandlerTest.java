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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerTest {

    public static final String INPUT_DATE = "2020-12-08T15:53:36.530377";
    public static final String EXPECTED_DATE = "2020-12-08T15:53:36.530377Z";
    private static final String DMN_NAME = "wa-task-initiation-ia-asylum";

    @Mock
    private WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    @Captor
    private ArgumentCaptor<SendMessageRequest<InitiateProcessVariables, CorrelationKeys>> captor;

    @Mock
    private IsoDateFormatter isoDateFormatter;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    private final EventInformation eventInformation = EventInformation.builder()
        .eventId("submitAppeal")
        .newStateId("")
        .jurisdictionId("ia")
        .caseTypeId("asylum")
        .caseReference("some case reference")
        .dateTime(LocalDateTime.parse(INPUT_DATE))
        .build();

    @Test
    void evaluateDmn() {

        EvaluateDmnRequest<InitiateEvaluateRequest> requestParameters =
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

        Mockito.when(isoDateFormatter.format(eq(LocalDateTime.parse(INPUT_DATE))))
            .thenReturn(EXPECTED_DATE);

        InitiateEvaluateResponse initiateTaskResponse = InitiateEvaluateResponse.builder()
            .group(new DmnStringValue("TCW"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .build();

        List<InitiateEvaluateResponse> results = List.of(initiateTaskResponse);

        handlerService.handle(results, eventInformation);

        Mockito.verify(apiClientToInitiateTask).sendMessage(captor.capture());
        SendMessageRequest<InitiateProcessVariables, CorrelationKeys> actualSendMessageRequest = captor.getValue();

        assertThat(actualSendMessageRequest).isEqualTo(getExpectedSendMessageRequest());
    }

    private SendMessageRequest<InitiateProcessVariables, CorrelationKeys> getExpectedSendMessageRequest() {
        InitiateProcessVariables expectedInitiateTaskSendMessageRequest = InitiateProcessVariables.builder()
            .caseType(new DmnStringValue("asylum"))
            .group(new DmnStringValue("TCW"))
            .jurisdiction(new DmnStringValue("ia"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .caseId(new DmnStringValue("some case reference"))
            .dueDate(new DmnStringValue(EXPECTED_DATE))
            .build();

        return new SendMessageRequest<>(
            "createTaskMessage",
            expectedInitiateTaskSendMessageRequest,
                null);
    }
}
