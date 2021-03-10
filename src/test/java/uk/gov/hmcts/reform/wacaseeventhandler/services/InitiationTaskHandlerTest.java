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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerTest {

    public static final String INPUT_DATE = "2020-12-08T15:53:36.530377";
    public static final String EXPECTED_DATE = "2020-12-08T15:53:36.530377Z";
    private static final String DMN_NAME = "wa-task-initiation-ia-asylum";
    public static final String TENANT_ID = "ia";
    @Mock
    private WorkflowApiClientToInitiateTask apiClientToInitiateTask;
    @Mock
    private IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Captor
    private ArgumentCaptor<SendMessageRequest<InitiateProcessVariables, CorrelationKeys>> captor;

    @Mock
    private IsoDateFormatter isoDateFormatter;

    @Mock
    private DueDateService dueDateService;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    private final String eventInstanceId = UUID.randomUUID().toString();

    private final EventInformation eventInformation = EventInformation.builder()
        .eventInstanceId(eventInstanceId)
        .eventId("submitAppeal")
        .newStateId("")
        .jurisdictionId("ia")
        .caseTypeId("asylum")
        .caseId("some case reference")
        .eventTimeStamp(LocalDateTime.parse(INPUT_DATE))
        .build();

    @Test
    void evaluateDmn() {

        EvaluateDmnRequest<InitiateEvaluateRequest> requestParameters =
            InitiateTaskHelper.buildInitiateTaskDmnRequest();

        Mockito.when(apiClientToInitiateTask.evaluateDmn(
            DMN_NAME,
            requestParameters,
            TENANT_ID
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        handlerService.evaluateDmn(eventInformation);

        Mockito.verify(apiClientToInitiateTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters),
            eq(TENANT_ID)
        );
    }

    @Test
    void handle() {
        Mockito.when(isoDateFormatter.format(eq(LocalDateTime.parse(INPUT_DATE))))
            .thenReturn(EXPECTED_DATE);

        InitiateEvaluateResponse initiateTaskResponse1 = InitiateEvaluateResponse.builder()
            .group(new DmnStringValue("TCW"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .workingDaysAllowed(new DmnIntegerValue(0))
            .taskCategory(new DmnStringValue("Case progression"))
            .build();

        InitiateEvaluateResponse initiateTaskResponse2 = InitiateEvaluateResponse.builder()
            .group(new DmnStringValue("external"))
            .name(new DmnStringValue("Decide On Time Extension"))
            .taskId(new DmnStringValue("decideOnTimeExtension"))
            .workingDaysAllowed(new DmnIntegerValue(0))
            .taskCategory(new DmnStringValue("Time extension"))
            .build();

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "decideOnTimeExtension"))
            .thenReturn("idempotencyKey2");

        List<InitiateEvaluateResponse> results = List.of(initiateTaskResponse1, initiateTaskResponse2);

        when(dueDateService.calculateDueDate(ZonedDateTime.parse(EXPECTED_DATE), 0))
            .thenReturn(ZonedDateTime.parse(EXPECTED_DATE));

        handlerService.handle(results, eventInformation);

        Mockito.verify(apiClientToInitiateTask, Mockito.times(2)).sendMessage(captor.capture());
        SendMessageRequest<InitiateProcessVariables, CorrelationKeys> actualSendMessageRequest = captor.getValue();

        assertThat(captor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "Case progression",
            "TCW"
        ));

        assertThat(captor.getAllValues().get(1)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey2",
            "Decide On Time Extension",
            "decideOnTimeExtension",
            "Time extension",
            "external"
        ));
    }

    private SendMessageRequest<InitiateProcessVariables, CorrelationKeys> getExpectedSendMessageRequest(
        String idempotencyKey,
        String name,
        String taskId,
        String taskCategory,
        String group
    ) {
        InitiateProcessVariables expectedInitiateTaskSendMessageRequest = InitiateProcessVariables.builder()
            .idempotencyKey(new DmnStringValue(idempotencyKey))
            .caseType(new DmnStringValue("asylum"))
            .jurisdiction(new DmnStringValue("ia"))
            .group(new DmnStringValue(group))
            .name(new DmnStringValue(name))
            .taskId(new DmnStringValue(taskId))
            .taskCategory(new DmnStringValue(taskCategory))
            .caseId(new DmnStringValue("some case reference"))
            .dueDate(new DmnStringValue(EXPECTED_DATE))
            .workingDaysAllowed(new DmnIntegerValue(0))
            .delayUntil(new DmnStringValue(ZonedDateTime.parse(EXPECTED_DATE)
                                               .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .build();

        return new SendMessageRequest<>(
            "createTaskMessage",
            expectedInitiateTaskSendMessageRequest,
            null, false
        );
    }
}
